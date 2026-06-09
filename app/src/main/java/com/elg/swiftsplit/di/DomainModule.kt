package com.elg.swiftsplit.di

import com.elg.swiftsplit.domain.service.ComparisonCalculator
import com.elg.swiftsplit.domain.service.SplitTimeCalculator
import com.elg.swiftsplit.domain.service.TimerService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideTimerService(): TimerService {
        return TimerService()
    }

    @Provides
    @Singleton
    fun provideComparisonCalculator(): ComparisonCalculator {
        return ComparisonCalculator
    }

    @Provides
    @Singleton
    fun provideSplitTimeCalculator(): SplitTimeCalculator {
        return SplitTimeCalculator
    }
}
