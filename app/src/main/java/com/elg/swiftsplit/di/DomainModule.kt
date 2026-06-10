package com.elg.swiftsplit.di

import com.elg.swiftsplit.domain.service.ComparisonCalculator
import com.elg.swiftsplit.domain.service.SplitTimeCalculator
import com.elg.swiftsplit.domain.service.TimerService
import com.elg.swiftsplit.domain.service.Clock
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
    fun provideTimerService(clock: Clock): TimerService {
        return TimerService(clock)
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
