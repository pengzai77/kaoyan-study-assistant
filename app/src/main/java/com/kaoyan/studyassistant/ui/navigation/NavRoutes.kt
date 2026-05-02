package com.kaoyan.studyassistant.ui.navigation

/**
 * 导航路由常量
 */
object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val TIMER = "timer"
    const val TODAY = "today"
    const val HISTORY = "history"
    const val STATISTICS = "statistics"
    const val REVIEW_LIST = "review_list"
    const val REVIEW_EDIT = "review_edit/{reviewId}"
    const val SUMMARY = "summary"
    const val SUBJECT_MANAGE = "subject_manage"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val GOAL = "goal"

    fun reviewEdit(reviewId: Long = 0L) = "review_edit/$reviewId"
}
