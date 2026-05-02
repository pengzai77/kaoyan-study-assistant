package com.kaoyan.studyassistant.data.repository

import com.kaoyan.studyassistant.data.local.dao.DateDurationSummary
import com.kaoyan.studyassistant.data.local.dao.StudySessionDao
import com.kaoyan.studyassistant.data.local.dao.SubjectDurationSummary
import com.kaoyan.studyassistant.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习记录 Repository
 */
@Singleton
class StudySessionRepository @Inject constructor(
    private val sessionDao: StudySessionDao
) {
    /** 保存一条学习记录（别名 insertSession，供 TimerViewModel 调用） */
    suspend fun insertSession(session: StudySession): Long = sessionDao.insertSession(session)

    /** 保存一条学习记录（原有接口，保持兼容） */
    suspend fun saveSession(session: StudySession): Long = sessionDao.insertSession(session)

    /** 删除学习记录 */
    suspend fun deleteSession(session: StudySession) = sessionDao.deleteSession(session)

    /** 更新学习记录 */
    suspend fun updateSession(session: StudySession) = sessionDao.updateSession(session)

    /** 获取所有记录 */
    fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    /** 获取指定日期的记录 */
    fun getSessionsByDate(date: String): Flow<List<StudySession>> =
        sessionDao.getSessionsByDate(date)

    /** 获取日期范围内的记录 */
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<StudySession>> =
        sessionDao.getSessionsBetweenDates(startDate, endDate)

    /** 获取今日总时长 */
    fun getTodayTotalDuration(date: String): Flow<Long> = sessionDao.getTodayTotalDuration(date)

    /** 获取历史总时长 */
    fun getTotalDuration(): Flow<Long> = sessionDao.getTotalDuration()

    /** 按科目汇总时长 */
    fun getDurationBySubject(): Flow<List<SubjectDurationSummary>> =
        sessionDao.getDurationBySubject()

    /** 按日期范围汇总时长 */
    fun getDurationByDateRange(startDate: String, endDate: String): Flow<List<DateDurationSummary>> =
        sessionDao.getDurationByDateRange(startDate, endDate)

    /** 获取最近 N 天有学习记录的日期 */
    suspend fun getRecentStudyDates(limit: Int): List<String> =
        sessionDao.getRecentStudyDates(limit)

    /** 同步查询日期范围内的记录（用于智能总结） */
    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<StudySession> =
        sessionDao.getSessionsBetweenDatesSync(startDate, endDate)

    /** 同步查询所有记录（用于备份导出） */
    suspend fun getAllSessionsSync(): List<StudySession> =
        sessionDao.getAllSessionsSync()
}
