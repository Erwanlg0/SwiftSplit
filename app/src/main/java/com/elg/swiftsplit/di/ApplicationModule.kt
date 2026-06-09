package com.elg.swiftsplit.di

import com.elg.swiftsplit.application.port.input.*
import com.elg.swiftsplit.application.service.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    
    @Binds
    abstract fun bindImportRunUseCase(impl: ImportRunUseCaseImpl): ImportRunUseCase

    @Binds
    abstract fun bindExportRunUseCase(impl: ExportRunUseCaseImpl): ExportRunUseCase

    @Binds
    abstract fun bindGetRunsUseCase(impl: GetRunsUseCaseImpl): GetRunsUseCase

    @Binds
    abstract fun bindGetRunByIdUseCase(impl: GetRunByIdUseCaseImpl): GetRunByIdUseCase

    @Binds
    abstract fun bindSaveRunUseCase(impl: SaveRunUseCaseImpl): SaveRunUseCase

    @Binds
    abstract fun bindDeleteRunUseCase(impl: DeleteRunUseCaseImpl): DeleteRunUseCase

    @Binds
    abstract fun bindUpdateRunUseCase(impl: UpdateRunUseCaseImpl): UpdateRunUseCase

    
    @Binds
    abstract fun bindObserveTimerUseCase(impl: ObserveTimerUseCaseImpl): ObserveTimerUseCase

    @Binds
    abstract fun bindStartTimerUseCase(impl: StartTimerUseCaseImpl): StartTimerUseCase

    @Binds
    abstract fun bindSplitUseCase(impl: SplitUseCaseImpl): SplitUseCase

    @Binds
    abstract fun bindUndoSplitUseCase(impl: UndoSplitUseCaseImpl): UndoSplitUseCase

    @Binds
    abstract fun bindSkipSplitUseCase(impl: SkipSplitUseCaseImpl): SkipSplitUseCase

    @Binds
    abstract fun bindPauseResumeTimerUseCase(impl: PauseResumeTimerUseCaseImpl): PauseResumeTimerUseCase

    @Binds
    abstract fun bindResetTimerUseCase(impl: ResetTimerUseCaseImpl): ResetTimerUseCase

    
    @Binds
    abstract fun bindConnectToLiveSplitUseCase(impl: ConnectToLiveSplitUseCaseImpl): ConnectToLiveSplitUseCase

    @Binds
    abstract fun bindDisconnectLiveSplitUseCase(impl: DisconnectLiveSplitUseCaseImpl): DisconnectLiveSplitUseCase

    @Binds
    abstract fun bindSendLiveSplitCommandUseCase(impl: SendLiveSplitCommandUseCaseImpl): SendLiveSplitCommandUseCase

    @Binds
    abstract fun bindObserveLiveSplitConnectionUseCase(impl: ObserveLiveSplitConnectionUseCaseImpl): ObserveLiveSplitConnectionUseCase

    @Binds
    abstract fun bindObserveTimerLayoutPreferencesUseCase(impl: ObserveTimerLayoutPreferencesUseCaseImpl): ObserveTimerLayoutPreferencesUseCase

    @Binds
    abstract fun bindUpdateTimerLayoutPreferencesUseCase(impl: UpdateTimerLayoutPreferencesUseCaseImpl): UpdateTimerLayoutPreferencesUseCase
}
