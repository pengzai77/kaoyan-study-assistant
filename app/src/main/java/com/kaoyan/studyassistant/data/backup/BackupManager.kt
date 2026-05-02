package com.kaoyan.studyassistant.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份与恢复管理器（v5 性能优化版）
 *
 * 性能优化：
 * 1. restorePayload 中 5 个独立 DataStore 写入合并为 1 次批量写入
 * 2. 恢复 sessions 时预加载去重 key 集合，避免逐条查询
 * 3. 所有 IO 操作确保在 Dispatchers.IO 上执行
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: StudySessionRepository,
    private val reviewRepository: DailyReviewRepository,
    private val goalSettingsRepository: GoalSettingsRepository,
    private val appPreferences: AppPreferences
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val BACKUP_FOLDER = "考研学习助手"
    private val BACKUP_EXT = ".kaoyan_backup"
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // ===================== 公开接口 =====================

    suspend fun backup(): String = withContext(Dispatchers.IO) {
        val payload = collectPayload()
        val json = gson.toJson(payload)
        val fileName = "backup_${fileNameFormat.format(Date())}$BACKUP_EXT"
        writeToSharedDocuments(fileName, json)
        cleanOldBackups(maxKeep = 5)
        appPreferences.updateLastBackupTime(System.currentTimeMillis())
        fileName
    }

    suspend fun backupToUri(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val payload = collectPayload()
        val json = gson.toJson(payload)
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("无法打开目标文件")
        appPreferences.updateLastBackupTime(System.currentTimeMillis())
    }

    suspend fun readBackupFromUri(uri: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() }
            ?: throw IllegalStateException("无法读取备份文件")
        val payload = gson.fromJson(json, BackupPayload::class.java)
            ?: throw IllegalArgumentException("备份文件格式无效")
        payload.validate()
        payload
    }

    suspend fun restoreFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val payload = readBackupFromUri(uri)
        restorePayload(payload)
    }

    suspend fun restoreFromLatestAutoBackup(): String = withContext(Dispatchers.IO) {
        val files = getAutoBackupFileInfos()
        val latest = files.firstOrNull() ?: throw IllegalStateException("没有可用的自动备份")
        val json = readContentFromUri(latest.uri)
        val payload = gson.fromJson(json, BackupPayload::class.java)
            ?: throw IllegalArgumentException("备份文件格式无效")
        payload.validate()
        restorePayload(payload)
    }

    fun getAutoBackupFiles(): List<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getAutoBackupFileInfos().map { info ->
                object : File(info.name) {
                    override fun lastModified(): Long = info.lastModified
                    override fun length(): Long = info.size
                    override fun getName(): String = info.name
                }
            }
        } else {
            getLegacyBackupDir()?.listFiles()
                ?.filter { it.name.endsWith(BACKUP_EXT) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    fun hasAutoBackup(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getAutoBackupFileInfos().isNotEmpty()
        } else {
            getLegacyBackupDir()?.listFiles()
                ?.any { it.name.endsWith(BACKUP_EXT) } == true
        }
    }

    suspend fun peekLatestAutoBackup(): BackupPayload? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val infos = getAutoBackupFileInfos()
                val latest = infos.firstOrNull() ?: return@withContext null
                val json = readContentFromUri(latest.uri)
                gson.fromJson(json, BackupPayload::class.java)
            } else {
                val file = getLegacyBackupDir()?.listFiles()
                    ?.filter { it.name.endsWith(BACKUP_EXT) }
                    ?.maxByOrNull { it.lastModified() } ?: return@withContext null
                gson.fromJson(file.readText(Charsets.UTF_8), BackupPayload::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ===================== 私有：写入共享存储 =====================

    private fun writeToSharedDocuments(fileName: String, json: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(fileName, json)
        } else {
            writeViaFileApi(fileName, json)
        }
    }

    private fun writeViaMediaStore(fileName: String, json: String) {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER"

        val queryUri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        resolver.delete(queryUri, selection, arrayOf(fileName, "$relativePath/"))

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        val uri = resolver.insert(queryUri, contentValues)
            ?: throw IllegalStateException("无法在 Documents/$BACKUP_FOLDER 中创建备份文件")
        resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("无法写入备份文件")
    }

    private fun writeViaFileApi(fileName: String, json: String) {
        val dir = getLegacyBackupDir() ?: throw IllegalStateException("无法访问备份目录")
        File(dir, fileName).writeText(json, Charsets.UTF_8)
    }

    private fun getLegacyBackupDir(): File? {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(docsDir, BACKUP_FOLDER)
        return if (backupDir.exists() || backupDir.mkdirs()) backupDir else null
    }

    // ===================== 私有：列举共享存储备份 =====================

    private data class BackupFileInfo(val name: String, val uri: Uri, val lastModified: Long, val size: Long)

    private fun getAutoBackupFileInfos(): List<BackupFileInfo> {
        val result = mutableListOf<BackupFileInfo>()
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER/"
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(relativePath, "%$BACKUP_EXT")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        resolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val lastModified = cursor.getLong(dateCol) * 1000L
                val size = cursor.getLong(sizeCol)
                val uri = MediaStore.Files.getContentUri("external", id)
                result.add(BackupFileInfo(name, uri, lastModified, size))
            }
        }
        return result
    }

    private fun readContentFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() }
            ?: throw IllegalStateException("无法读取备份文件内容")
    }

    // ===================== 私有：清理旧备份 =====================

    private fun cleanOldBackups(maxKeep: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val infos = getAutoBackupFileInfos()
            if (infos.size > maxKeep) {
                val resolver = context.contentResolver
                infos.drop(maxKeep).forEach { info ->
                    try { resolver.delete(info.uri, null, null) } catch (_: Exception) {}
                }
            }
        } else {
            val dir = getLegacyBackupDir() ?: return
            val files = dir.listFiles()
                ?.filter { it.name.endsWith(BACKUP_EXT) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            if (files.size > maxKeep) {
                files.drop(maxKeep).forEach { try { it.delete() } catch (_: Exception) {} }
            }
        }
    }

    // ===================== 私有：数据收集与恢复 =====================

    private suspend fun collectPayload(): BackupPayload {
        val subjects = subjectRepository.getAllSubjectsSync()
        val sessions = sessionRepository.getAllSessionsSync()
        val reviews = reviewRepository.getAllReviewsSync()
        val goalSettings = goalSettingsRepository.getGoalSettingsSync()
        val userPrefs = appPreferences.userSettings.first()
        val userSettings = BackupUserSettings(
            showSeconds = userPrefs.showSeconds,
            showLatestReview = userPrefs.showLatestReview,
            historyDefaultRange = userPrefs.historyDefaultRange,
            enableVibration = userPrefs.enableVibration,
            darkMode = userPrefs.darkMode
        )
        return BackupPayload(
            subjects = subjects,
            sessions = sessions,
            reviews = reviews,
            goalSettings = goalSettings,
            userSettings = userSettings
        )
    }

    private suspend fun restorePayload(payload: BackupPayload): String {
        var subjectsAdded = 0
        var sessionsAdded = 0

        // 1. 恢复科目（按 name 去重）
        payload.subjects.forEach { subject ->
            if (subjectRepository.getSubjectByName(subject.name) == null) {
                subjectRepository.addSubject(subject.name, subject.color)
                subjectsAdded++
            }
        }

        // 2. 恢复学习记录（按 startTime + subjectName 去重）
        // 预加载全部现有 key 到内存，避免逐条查询
        val existingKeys = sessionRepository.getAllSessionsSync()
            .map { "${it.startTime}_${it.subjectName}" }.toHashSet()
        payload.sessions.forEach { session ->
            if ("${session.startTime}_${session.subjectName}" !in existingKeys) {
                sessionRepository.insertSession(session.copy(id = 0))
                sessionsAdded++
            }
        }

        // 3. 恢复复盘（按 date 去重，已存在则覆盖）
        payload.reviews.forEach { review ->
            val existing = reviewRepository.getReviewByDate(review.date)
            if (existing == null) {
                reviewRepository.saveReview(review.copy(id = 0))
            } else {
                reviewRepository.saveReview(review.copy(id = existing.id, updatedAt = System.currentTimeMillis()))
            }
        }

        // 4. 恢复目标设置（直接覆盖）
        payload.goalSettings?.let { goalSettingsRepository.saveGoalSettings(it) }

        // 5. 恢复用户设置 — 合并为一次批量写入，减少 5 次独立 DataStore 事务
        payload.userSettings?.let { us ->
            appPreferences.updateUserSettingsBatch(
                showSeconds = us.showSeconds,
                showLatestReview = us.showLatestReview,
                historyDefaultRange = us.historyDefaultRange,
                enableVibration = us.enableVibration,
                darkMode = us.darkMode
            )
        }

        return "恢复完成\n新增科目：$subjectsAdded 个\n新增记录：$sessionsAdded 条\n" +
                "新增/更新复盘：${payload.reviews.size} 条"
    }
}
