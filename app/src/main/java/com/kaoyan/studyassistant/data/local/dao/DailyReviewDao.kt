package com.kaoyan.studyassistant.data.local.dao

import androidx.room.*
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import kotlinx.coroutines.flow.Flow

/**
 * 每日复盘数据访问对象
 */
@Dao
interface DailyReviewDao {

    /** 插入复盘记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: DailyReview): Long

    /** 更新复盘记录 */
    @Update
    suspend fun updateReview(review: DailyReview)

    /** 删除复盘记录 */
    @Delete
    suspend fun deleteReview(review: DailyReview)

    /** 查询所有复盘，按日期倒序 */
    @Query("SELECT * FROM daily_reviews ORDER BY date DESC")
    fun getAllReviews(): Flow<List<DailyReview>>

    /** 根据 ID 查询复盘 */
    @Query("SELECT * FROM daily_reviews WHERE id = :id")
    suspend fun getReviewById(id: Long): DailyReview?

    /** 根据日期查询复盘（每天最多一条） */
    @Query("SELECT * FROM daily_reviews WHERE date = :date LIMIT 1")
    suspend fun getReviewByDate(date: String): DailyReview?

    /** 查询日期范围内的复盘 */
    @Query("SELECT * FROM daily_reviews WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReviewsBetweenDates(startDate: String, endDate: String): Flow<List<DailyReview>>

    /** 查询所有复盘（同步，用于智能总结） */
    @Query("SELECT * FROM daily_reviews ORDER BY date DESC")
    suspend fun getAllReviewsSync(): List<DailyReview>

    /** 查询日期范围内的复盘（同步） */
    @Query("SELECT * FROM daily_reviews WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getReviewsBetweenDatesSync(startDate: String, endDate: String): List<DailyReview>
}
