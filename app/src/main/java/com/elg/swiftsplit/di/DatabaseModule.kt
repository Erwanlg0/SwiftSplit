package com.elg.swiftsplit.di

import android.content.Context
import com.elg.swiftsplit.infrastructure.persistence.SwiftSplitDatabase
import com.elg.swiftsplit.infrastructure.persistence.dao.RunDao
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
    fun provideDatabase(@ApplicationContext context: Context): SwiftSplitDatabase {
        return SwiftSplitDatabase.create(context)
    }

    @Provides
    fun provideRunDao(database: SwiftSplitDatabase): RunDao {
        return database.runDao()
    }
}
