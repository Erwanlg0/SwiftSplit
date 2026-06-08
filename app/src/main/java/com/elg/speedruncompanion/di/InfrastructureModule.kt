package com.elg.speedruncompanion.di

import android.content.Context
import com.elg.speedruncompanion.application.port.output.*
import com.elg.speedruncompanion.infrastructure.parser.LssFileExporter
import com.elg.speedruncompanion.infrastructure.parser.LssFileParser
import com.elg.speedruncompanion.infrastructure.persistence.RoomRunRepository
import com.elg.speedruncompanion.infrastructure.remote.LiveSplitTcpClient
import com.elg.speedruncompanion.infrastructure.settings.DataStoreSettingsAdapter
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
