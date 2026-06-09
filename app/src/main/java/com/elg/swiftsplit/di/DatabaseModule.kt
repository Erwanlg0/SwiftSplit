package com.elg.swiftsplit.di

import android.content.Context
import com.elg.swiftsplit.infrastructure.persistence.SpeedrunDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): SpeedrunDatabase {
        return SpeedrunDatabase.create(context)
    }

    @Provides
    fun provideRunDao(database: SpeedrunDatabase): RunDao {
        return database.runDao()
    }
}
