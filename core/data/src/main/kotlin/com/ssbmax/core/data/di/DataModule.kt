package com.ssbmax.core.data.di

import android.content.Context
import androidx.room.Room
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.repository.TestRepositoryImpl
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
        ).build()
    }
    
    @Provides
    fun provideTestResultDao(database: SSBDatabase): TestResultDao {
        return database.testResultDao()
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
}

