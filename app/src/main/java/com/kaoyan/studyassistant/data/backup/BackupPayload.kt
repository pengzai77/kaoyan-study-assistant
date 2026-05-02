package com.kaoyan.studyassistant.data.backup

import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.local.entity.Subject

data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceInfo: String = "${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE}",
    val subjects: List<Subject> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val reviews: List<DailyReview> = emptyList(),
    val goalSettings: GoalSettings? = null,
    val userSettings: BackupUserSettings? = null
) {
    companion object {
        const val CURRENT_VERSION = 3
        const val MIN_COMPATIBLE_VERSION = 1
    }

    fun validate() {
        if (version < MIN_COMPATIBLE_VERSION) {
            throw IllegalArgumentException(
                "备份文件版本过旧(v$version)，当前最低支持 v$MIN_COMPATIBLE_VERSION"
            )
        }
    }

    fun summary(): String {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(createdAt))

        return buildString {
            append("科目：${subjects.size} 项\n")
            append("学习记录：${sessions.size} 条\n")
            append("复盘记录：${reviews.size} 条")
            if (goalSettings?.targetSchool?.isNotBlank() == true) {
                append("\n目标院校：${goalSettings.targetSchool}")
            }
            append("\n备份时间：$date")
        }
    }
}

// 仅保留非敏感设置快照，明确不导出 AI API Key / Base URL / Model / 模型缓存 / 对话上下文。
data class BackupUserSettings(
    val showSeconds: Boolean = true,
    val showLatestReview: Boolean = true,
    val historyDefaultRange: Int = 30,
    val enableVibration: Boolean = false,
    val darkMode: Int = 0
)
