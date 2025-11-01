package com.ssbmax.core.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.ssbmax.core.data.local.DatabaseMigrations
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.dao.SRTSituationCacheDao
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.dao.TestUsageDao
import com.ssbmax.core.data.local.dao.WATWordCacheDao
import com.ssbmax.core.data.repository.NotificationRepositoryImpl
import com.ssbmax.core.data.repository.TestRepositoryImpl
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.repository.TestRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for data layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSSBDatabase(
        @ApplicationContext context: Context
    ): SSBDatabase {
        return Room.databaseBuilder(
            context,
            SSBDatabase::class.java,
            SSBDatabase.DATABASE_NAME
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6
            )
        .build()
    }
    
    @Provides
    fun provideTestResultDao(database: SSBDatabase): TestResultDao {
        return database.testResultDao()
    }
    
    @Provides
    fun provideNotificationDao(database: SSBDatabase): NotificationDao {
        return database.notificationDao()
    }
    
    @Provides
    fun provideOIRQuestionCacheDao(database: SSBDatabase): OIRQuestionCacheDao {
        return database.oirQuestionCacheDao()
    }
    
    @Provides
    fun provideTestUsageDao(database: SSBDatabase): TestUsageDao {
        return database.testUsageDao()
    }
    
    @Provides
    fun provideWATWordCacheDao(database: SSBDatabase): WATWordCacheDao {
        return database.watWordCacheDao()
    }
    
    @Provides
    fun provideSRTSituationCacheDao(database: SSBDatabase): SRTSituationCacheDao {
        return database.srtSituationCacheDao()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTestRepository(
        impl: TestRepositoryImpl
    ): TestRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.ssbmax.core.data.repository.AuthRepositoryImpl
    ): com.ssbmax.core.domain.repository.AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindSubmissionRepository(
        impl: com.ssbmax.core.data.remote.FirestoreSubmissionRepository
    ): com.ssbmax.core.domain.repository.SubmissionRepository
    
    @Binds
    @Singleton
    abstract fun bindAIScoringService(
        impl: com.ssbmax.core.data.service.MockAIScoringService
    ): com.ssbmax.core.domain.service.AIScoringService
    
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository
    
    @Binds
    @Singleton
    abstract fun bindTestSubmissionRepository(
        impl: com.ssbmax.core.data.repository.TestSubmissionRepositoryImpl
    ): com.ssbmax.core.domain.repository.TestSubmissionRepository
    
    @Binds
    @Singleton
    abstract fun bindTestContentRepository(
        impl: com.ssbmax.core.data.repository.TestContentRepositoryImpl
    ): com.ssbmax.core.domain.repository.TestContentRepository
    
    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: com.ssbmax.core.data.repository.UserProfileRepositoryImpl
    ): com.ssbmax.core.domain.repository.UserProfileRepository
    
    @Binds
    @Singleton
    abstract fun bindTestProgressRepository(
        impl: com.ssbmax.core.data.repository.TestProgressRepositoryImpl
    ): com.ssbmax.core.domain.repository.TestProgressRepository
    
    @Binds
    @Singleton
    abstract fun bindGradingQueueRepository(
        impl: com.ssbmax.core.data.repository.GradingQueueRepositoryImpl
    ): com.ssbmax.core.domain.repository.GradingQueueRepository
}

