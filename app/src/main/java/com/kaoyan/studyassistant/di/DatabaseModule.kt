package com.kaoyan.studyassistant.di

import android.content.Context
import androidx.room.Room
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.backup.BackupManager
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.dao.DailyReviewDao
import com.kaoyan.studyassistant.data.local.dao.GoalSettingsDao
import com.kaoyan.studyassistant.data.local.dao.StudySessionDao
import com.kaoyan.studyassistant.data.local.dao.SubjectDao
import com.kaoyan.studyassistant.data.local.dao.SummaryChatMessageDao
import com.kaoyan.studyassistant.data.local.database.AppDatabase
import com.kaoyan.studyassistant.data.local.database.MIGRATION_1_2
import com.kaoyan.studyassistant.data.local.database.MIGRATION_2_3
import com.kaoyan.studyassistant.data.local.database.MIGRATION_3_4
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import com.kaoyan.studyassistant.data.repository.SummaryChatRepository
import com.kaoyan.studyassistant.domain.summarizer.RuleBasedSummarizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kaoyan_study_assistant.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()

    @Provides
    fun provideDailyReviewDao(db: AppDatabase): DailyReviewDao = db.dailyReviewDao()

    @Provides
    fun provideGoalSettingsDao(db: AppDatabase): GoalSettingsDao = db.goalSettingsDao()

    @Provides
    fun provideSummaryChatMessageDao(db: AppDatabase): SummaryChatMessageDao = db.summaryChatMessageDao()

    @Provides
    @Singleton
    fun provideSubjectRepository(dao: SubjectDao): SubjectRepository = SubjectRepository(dao)

    @Provides
    @Singleton
    fun provideStudySessionRepository(dao: StudySessionDao): StudySessionRepository =
        StudySessionRepository(dao)

    @Provides
    @Singleton
    fun provideDailyReviewRepository(dao: DailyReviewDao): DailyReviewRepository =
        DailyReviewRepository(dao)

    @Provides
    @Singleton
    fun provideGoalSettingsRepository(dao: GoalSettingsDao): GoalSettingsRepository =
        GoalSettingsRepository(dao)

    @Provides
    @Singleton
    fun provideSummaryChatRepository(dao: SummaryChatMessageDao): SummaryChatRepository =
        SummaryChatRepository(dao)

    @Provides
    @Singleton
    fun provideAutoBackupCoordinator(
        backupManager: BackupManager,
        appPreferences: AppPreferences
    ): AutoBackupCoordinator = AutoBackupCoordinator(backupManager, appPreferences)

    @Provides
    @Singleton
    fun provideRuleBasedSummarizer(): RuleBasedSummarizer = RuleBasedSummarizer()

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        subjectRepository: SubjectRepository,
        sessionRepository: StudySessionRepository,
        reviewRepository: DailyReviewRepository,
        goalSettingsRepository: GoalSettingsRepository,
        appPreferences: AppPreferences
    ): BackupManager = BackupManager(
        context = context,
        subjectRepository = subjectRepository,
        sessionRepository = sessionRepository,
        reviewRepository = reviewRepository,
        goalSettingsRepository = goalSettingsRepository,
        appPreferences = appPreferences
    )
}
