package com.elg.speedruncompanion.application.service

import com.elg.speedruncompanion.application.port.output.RunRepository
import com.elg.speedruncompanion.domain.model.*
import com.elg.speedruncompanion.domain.service.TimerService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    private val runRepository: RunRepository = mockk(relaxed = true)
    private val timerService = TimerService()
    private val timerManager = TimerManager(runRepository, timerService)

    @Test
    fun testResetIncrementsAttemptCountEvenIfNoSegmentsCompleted() = runTest {
        // Given
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

        // When: Start and then Reset the timer immediately (currentSegmentIndex = 0)
        timerManager.start(runId, ComparisonName.PERSONAL_BEST, TimingMethod.REAL_TIME)
        timerManager.reset(saveAttempt = true)

        // Then
        // Verify that runRepository.update was called with run having attemptCount = 6
        val runSlot = slot<Run>()
        coVerify(exactly = 1) { runRepository.update(capture(runSlot)) }

        val updatedRun = runSlot.captured
        assertEquals(6, updatedRun.attemptCount)
        assertEquals(1, updatedRun.attemptHistory.size)
        assertEquals(6, updatedRun.attemptHistory[0].id)
        
        // Also verify that the state went back to Idle
        val state = timerManager.timerState.first()
        assertTrue(state is TimerState.Idle)
    }
}
