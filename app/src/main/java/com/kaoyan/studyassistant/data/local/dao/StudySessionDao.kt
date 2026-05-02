package com.kaoyan.studyassistant.data.local.dao

import androidx.room.*
import com.kaoyan.studyassistant.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

/**
 * 学习记录数据访问对象
 */
@Dao
interface StudySessionDao {

    /** 插入一条学习记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    /** 更新学习记录 */
    @Update
    suspend fun updateSession(session: StudySession)

    /** 删除学习记录 */
    @Delete
    suspend fun deleteSession(session: StudySession)

    /** 查询所有记录，按开始时间倒序 */
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    /** 查询指定日期的所有记录 */
    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY startTime ASC")
    fun getSessionsByDate(date: String): Flow<List<StudySession>>

    /** 查询指定日期范围内的所有记录 */
    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<StudySession>>

    /** 查询指定科目的所有记录 */
    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY startTime DESC")
    fun getSessionsBySubject(subjectId: Long): Flow<List<StudySession>>

    /** 查询今日总学习时长（毫秒） */
    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM study_sessions WHERE date = :date")
    fun getTodayTotalDuration(date: String): Flow<Long>

    /** 查询历史总学习时长（毫秒） */
    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM study_sessions")
    fun getTotalDuration(): Flow<Long>

    /** 按科目汇总时长，返回 (subjectName, totalMillis) */
    @Query("""
        SELECT subjectName, SUM(durationMillis) as totalMillis 
        FROM study_sessions 
        GROUP BY subjectName 
        ORDER BY totalMillis DESC
    """)
    fun getDurationBySubject(): Flow<List<SubjectDurationSummary>>

    /** 按日期汇总时长 */
    @Query("""
        SELECT date, SUM(durationMillis) as totalMillis 
        FROM study_sessions 
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDurationByDateRange(startDate: String, endDate: String): Flow<List<DateDurationSummary>>

    /** 查询最近 N 天的学习日期列表（有记录的天） */
    @Query("SELECT DISTINCT date FROM study_sessions ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentStudyDates(limit: Int): List<String>

    /** 查询某日期范围内的所有记录（用于智能总结，非 Flow） */
    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<StudySession>

    /** 查询所有记录（同步，用于备份导出） */
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<StudySession>
}

/** 科目时长汇总 DTO */
data class SubjectDurationSummary(
    val subjectName: String,
    val totalMillis: Long
)

/** 日期时长汇总 DTO */
data class DateDurationSummary(
    val date: String,
    val totalMillis: Long
)
