package com.kaoyan.studyassistant.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kaoyan.studyassistant.data.local.dao.DailyReviewDao
import com.kaoyan.studyassistant.data.local.dao.GoalSettingsDao
import com.kaoyan.studyassistant.data.local.dao.StudySessionDao
import com.kaoyan.studyassistant.data.local.dao.SubjectDao
import com.kaoyan.studyassistant.data.local.dao.SummaryChatMessageDao
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.local.entity.Subject
import com.kaoyan.studyassistant.data.local.entity.SummaryChatMessageEntity

@Database(
    entities = [
        Subject::class,
        StudySession::class,
        DailyReview::class,
        GoalSettings::class,
        SummaryChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun goalSettingsDao(): GoalSettingsDao
    abstract fun summaryChatMessageDao(): SummaryChatMessageDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM subjects
            WHERE id NOT IN (
                SELECT MIN(id) FROM subjects GROUP BY name
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subjects_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#4A90D9',
                createdAt INTEGER NOT NULL DEFAULT 0,
                isDefault INTEGER NOT NULL DEFAULT 0,
                UNIQUE(name)
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO subjects_new SELECT id, name, color, createdAt, isDefault FROM subjects")
        db.execSQL("DROP TABLE subjects")
        db.execSQL("ALTER TABLE subjects_new RENAME TO subjects")

        db.execSQL(
            """
            DELETE FROM daily_reviews
            WHERE id NOT IN (
                SELECT MIN(id) FROM daily_reviews GROUP BY date
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_reviews_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                completedContent TEXT NOT NULL DEFAULT '',
                problems TEXT NOT NULL DEFAULT '',
                tomorrowPlan TEXT NOT NULL DEFAULT '',
                extraNotes TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                UNIQUE(date)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO daily_reviews_new
            SELECT id, date, completedContent, problems, tomorrowPlan,
                   COALESCE(extraNotes, '') as extraNotes, createdAt, updatedAt
            FROM daily_reviews
            """.trimIndent()
        )
        db.execSQL("DROP TABLE daily_reviews")
        db.execSQL("ALTER TABLE daily_reviews_new RENAME TO daily_reviews")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                targetSchool TEXT NOT NULL DEFAULT '',
                targetMajor TEXT NOT NULL DEFAULT '',
                examDate TEXT NOT NULL DEFAULT '',
                weeklyGoalMinutes INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS summary_chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                conversationId TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                summaryRange TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_summary_chat_messages_conversationId ON summary_chat_messages(conversationId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_summary_chat_messages_createdAt ON summary_chat_messages(createdAt)"
        )
    }
}
