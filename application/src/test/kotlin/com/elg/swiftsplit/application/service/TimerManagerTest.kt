package com.elg.swiftsplit.application.service

import com.elg.swiftsplit.application.port.output.HapticFeedbackPort
import com.elg.swiftsplit.application.port.output.RunRepository
import com.elg.swiftsplit.application.port.output.SettingsPort
import com.elg.swiftsplit.domain.model.*
import com.elg.swiftsplit.domain.service.Clock
import com.elg.swiftsplit.domain.service.TimerService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    private val runRepository: RunRepository = mockk(relaxed = true)
    private val clock: Clock = mockk {
        every { currentTimeMillis() } returns 1000L
    }
    private val timerService = TimerService(clock)
    private val settingsPort: SettingsPort = mockk {
        every { observeSaveQuickRuns() } returns flowOf(false)
    }
    private val hapticFeedbackPort: HapticFeedbackPort = mockk(relaxed = true)
    private val timerManager = TimerManager(runRepository, timerService, settingsPort, clock, hapticFeedbackPort)

    @Test
    fun testResetIncrementsAttemptCountEvenIfNoSegmentsCompleted() = runTest {
        
        val runId = RunId("test-run-id")
        val initialRun = Run(
            id = runId,
            gameInfo = GameInfo("Super Mario 64", "120 Star"),
            segments = listOf(
                Segment("Bob-omb Battlefield"),
                Segment("Whomps Fortress")
            ),
            attemptCount = 5,
            attemptHistory = emptyList()
        )

        coEvery { runRepository.getById(runId) } returns initialRun

        
        timerManager.start(runId, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        timerManager.reset(saveAttempt = true)

        
        
        val runSlot = slot<Run>()
        coVerify(exactly = 1) { runRepository.update(capture(runSlot)) }

        val updatedRun = runSlot.captured
        assertEquals(6, updatedRun.attemptCount)
        assertEquals(1, updatedRun.attemptHistory.size)
        assertEquals(6, updatedRun.attemptHistory[0].id)
        
        
        val state = timerManager.timerState.first()
        assertTrue(state is TimerState.Idle)
    }

    @Test
    fun testSetComparisonUpdatesRunningState() = runTest {
        val runId = RunId("test-run-id")
        coEvery { runRepository.getById(runId) } returns Run(
            id = runId,
            gameInfo = GameInfo("Super Mario 64", "120 Star"),
            segments = listOf(Segment("Bob-omb Battlefield"))
        )

        timerManager.start(runId, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        timerManager.setComparison(ComparisonName.BEST_SEGMENTS)

        val state = timerManager.timerState.first()
        assertTrue(state is TimerState.Running)
        assertEquals(ComparisonName.BEST_SEGMENTS, (state as TimerState.Running).comparison)
    }

    @Test
    fun testSplitVibratesLongOnGoldSegment() = runTest {
        every { settingsPort.observeTimerLayoutPreferences() } returns flowOf(TimerLayoutPreferences.DEFAULT)
        val runId = RunId("test-run-id")
        coEvery { runRepository.getById(runId) } returns Run(
            id = runId,
            gameInfo = GameInfo("Super Mario 64", "120 Star"),
            segments = listOf(
                Segment("Bob-omb Battlefield", bestSegmentTime = SplitTime(realTime = TimeSpan(100L))),
                Segment("Whomps Fortress")
            )
        )

        timerManager.start(runId, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        timerManager.split()

        verify(exactly = 1) { hapticFeedbackPort.vibrate(150) }
    }

    @Test
    fun testSplitVibratesShortWithoutBestSegment() = runTest {
        every { settingsPort.observeTimerLayoutPreferences() } returns flowOf(TimerLayoutPreferences.DEFAULT)
        val runId = RunId("test-run-id")
        coEvery { runRepository.getById(runId) } returns Run(
            id = runId,
            gameInfo = GameInfo("Super Mario 64", "120 Star"),
            segments = listOf(
                Segment("Bob-omb Battlefield"),
                Segment("Whomps Fortress")
            )
        )

        timerManager.start(runId, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        timerManager.split()

        verify(exactly = 1) { hapticFeedbackPort.vibrate(50) }
    }
}
