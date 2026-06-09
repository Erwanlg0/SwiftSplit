package com.elg.swiftsplit.di

import com.elg.swiftsplit.application.port.output.*
import com.elg.swiftsplit.infrastructure.parser.LssFileExporter
import com.elg.swiftsplit.infrastructure.parser.LssFileParser
import com.elg.swiftsplit.infrastructure.persistence.RoomRunRepository
import com.elg.swiftsplit.infrastructure.remote.LiveSplitTcpClient
import com.elg.swiftsplit.infrastructure.settings.DataStoreSettingsAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InfrastructureModule {

    @Binds
    @Singleton
    abstract fun bindRunRepository(impl: RoomRunRepository): RunRepository

    @Binds
    abstract fun bindRunFileParser(impl: LssFileParser): RunFileParser

    @Binds
    abstract fun bindRunFileExporter(impl: LssFileExporter): RunFileExporter

    @Binds
    @Singleton
    abstract fun bindLiveSplitRemotePort(impl: LiveSplitTcpClient): LiveSplitRemotePort

    @Binds
    @Singleton
    abstract fun bindSettingsPort(impl: DataStoreSettingsAdapter): SettingsPort
}
