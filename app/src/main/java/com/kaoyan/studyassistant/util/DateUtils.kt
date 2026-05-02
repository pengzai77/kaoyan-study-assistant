package com.kaoyan.studyassistant.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类
 */
object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** 获取今天的日期字符串，格式 "yyyy-MM-dd" */
    fun today(): String = dateFormat.format(Date())

    /** 将时间戳格式化为日期字符串 */
    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /** 将日期字符串格式化为显示格式 "MM月dd日" */
    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr) ?: return dateStr
            displayDateFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    /** 将日期字符串格式化为完整显示格式 "yyyy年MM月dd日" */
    fun formatFullDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr) ?: return dateStr
            fullDateFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    /** 将时间戳格式化为时间字符串 "HH:mm" */
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    /** 获取 N 天前的日期字符串 */
    fun daysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(cal.time)
    }

    /** 将毫秒时长格式化为 "X小时Y分钟" */
    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "不足1分钟"
        }
    }

    /** 将毫秒时长格式化为不含秒的计时器显示格式 "HH:MM" */
    fun formatTimerDisplayNoSeconds(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format("%02d:%02d", hours, minutes)
    }

    /** 将毫秒时长格式化为计时器显示格式 "HH:MM:SS" */
    fun formatTimerDisplay(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** 解析日期字符串为 Calendar */
    fun parseDate(dateStr: String): Calendar? {
        return try {
            val date = dateFormat.parse(dateStr) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }

    /** 判断日期字符串是否是今天 */
    fun isToday(dateStr: String): Boolean = dateStr == today()

    /** 获取一周前的日期 */
    fun weekAgo(): String = daysAgo(7)

    /** 获取一个月前的日期 */
    fun monthAgo(): String = daysAgo(30)

    /**
     * 将时间戳转换为日期字符串（yyyy-MM-dd）
     * 用于学习记录日期归属：以开始时间所在自然日为准，避免跨天问题
     */
    fun timestampToDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /**
     * 获取 N 天后的日期字符串（N 为负数时等同于 daysAgo）
     */
    fun daysLater(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return dateFormat.format(cal.time)
    }

    /**
     * 计算两个日期字符串之间相差的天数（endDate - startDate）
     * 返回值可为负数
     */
    fun daysBetween(startDate: String, endDate: String): Int {
        return try {
            val start = dateFormat.parse(startDate) ?: return 0
            val end = dateFormat.parse(endDate) ?: return 0
            ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) { 0 }
    }
}
