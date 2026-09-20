package com.example.dualshortcut.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DispatchLogicTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `dispatch launches slot1 immediately and slot2 after delay`() = testScope.runTest {
        val target1 = LaunchTarget(
            label = "Target 1",
            packageName = "com.example.one",
            className = null,
            intentUri = "#Intent;action=android.intent.action.VIEW;package=com.example.one;end",
            type = TargetType.APPLICATION
        )
        val target2 = LaunchTarget(
            label = "Target 2",
            packageName = "com.example.two",
            className = null,
            intentUri = "#Intent;action=android.intent.action.VIEW;package=com.example.two;end",
            type = TargetType.SHORTCUT
        )
        val profile = LaunchProfile(target1, target2, delayMs = 300)

        val launchedEvents = mutableListOf<Pair<String, Long>>()
        var completed = false

        val runner = DispatchRunner(
            context = context,
            dispatcher = testDispatcher,
            launcher = { intent ->
                val pkg = intent.`package` ?: intent.component?.packageName ?: "unknown"
                launchedEvents.add(pkg to testScheduler.currentTime)
            }
        )

        runner.execute(profile, onComplete = { completed = true })

        // Immediately after start, slot 1 should be launched at t=0
        testScheduler.runCurrent()
        assertEquals(1, launchedEvents.size)
        assertEquals("com.example.one", launchedEvents[0].first)
        assertEquals(0L, launchedEvents[0].second)
        assertFalse(completed)

        // Advance past delay
        advanceTimeBy(300)
        testScheduler.runCurrent()

        // Slot 2 should now be launched
        assertEquals(2, launchedEvents.size)
        assertEquals("com.example.two", launchedEvents[1].first)
        assertEquals(300L, launchedEvents[1].second)
        assertTrue(completed)
    }

    @Test
    fun `dispatch handles ActivityNotFoundException gracefully without crash`() = testScope.runTest {
        val target1 = LaunchTarget(
            label = "Missing App",
            packageName = "com.example.missing",
            className = null,
            intentUri = "#Intent;action=android.intent.action.VIEW;package=com.example.missing;end",
            type = TargetType.APPLICATION
        )
        val target2 = LaunchTarget(
            label = "Target 2",
            packageName = "com.example.two",
            className = null,
            intentUri = "#Intent;action=android.intent.action.VIEW;package=com.example.two;end",
            type = TargetType.SHORTCUT
        )
        val profile = LaunchProfile(target1, target2, delayMs = 200)

        val errors = mutableListOf<String>()
        var completed = false

        val runner = DispatchRunner(
            context = context,
            dispatcher = testDispatcher,
            launcher = { intent ->
                if (intent.`package` == "com.example.missing") {
                    throw ActivityNotFoundException("No activity found for package")
                }
            },
            onError = { errorMsg ->
                errors.add(errorMsg)
            }
        )

        runner.execute(profile, onComplete = { completed = true })
        testScheduler.runCurrent()
        advanceTimeBy(200)
        testScheduler.runCurrent()

        // Missing app error caught, but does not crash, continues and completes
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Missing App"))
        assertTrue(completed)
    }
}
