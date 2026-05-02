package com.kaoyan.studyassistant.data.repository

import com.kaoyan.studyassistant.data.local.dao.DailyReviewDao
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日复盘 Repository
 */
@Singleton
class DailyReviewRepository @Inject constructor(
    private val reviewDao: DailyReviewDao
) {
    /** 获取所有复盘 */
    fun getAllReviews(): Flow<List<DailyReview>> = reviewDao.getAllReviews()

    /** 根据 ID 获取复盘 */
    suspend fun getReviewById(id: Long): DailyReview? = reviewDao.getReviewById(id)

    /** 根据日期获取复盘 */
    suspend fun getReviewByDate(date: String): DailyReview? = reviewDao.getReviewByDate(date)

    /** 保存复盘（新增或更新） */
    suspend fun saveReview(review: DailyReview): Long {
        return if (review.id == 0L) {
            reviewDao.insertReview(review)
        } else {
            reviewDao.updateReview(review.copy(updatedAt = System.currentTimeMillis()))
            review.id
        }
    }

    /** 删除复盘 */
    suspend fun deleteReview(review: DailyReview) = reviewDao.deleteReview(review)

    /** 获取日期范围内的复盘 */
    fun getReviewsBetweenDates(startDate: String, endDate: String): Flow<List<DailyReview>> =
        reviewDao.getReviewsBetweenDates(startDate, endDate)

    /** 同步获取所有复盘（用于智能总结） */
    suspend fun getAllReviewsSync(): List<DailyReview> = reviewDao.getAllReviewsSync()

    /** 同步获取日期范围内的复盘 */
    suspend fun getReviewsBetweenDatesSync(startDate: String, endDate: String): List<DailyReview> =
        reviewDao.getReviewsBetweenDatesSync(startDate, endDate)
}
