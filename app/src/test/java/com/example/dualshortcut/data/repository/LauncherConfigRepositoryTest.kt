package com.example.dualshortcut.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherConfigRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: LauncherConfigRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        context.getSharedPreferences("dual_launcher_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = LauncherConfigRepository(context)
    }

    @Test
    fun `initial profile has default delay and empty slots`() {
        val profile = repository.getProfile()
        assertNull(profile.slot1)
        assertNull(profile.slot2)
        assertEquals(200L, profile.delayMs)
        assertFalse(profile.isConfigured)
    }

    @Test
    fun `save slot1 and retrieve`() {
        val target1 = LaunchTarget(
            label = "Camera Tool",
            packageName = "com.example.camera",
            className = "com.example.camera.CaptureActivity",
            intentUri = "intent:#Intent;action=android.intent.action.MAIN;package=com.example.camera;end",
            type = TargetType.APPLICATION
        )

        repository.saveSlot1(target1)

        val profile = repository.getProfile()
        assertNotNull(profile.slot1)
        assertEquals("Camera Tool", profile.slot1?.label)
        assertEquals("com.example.camera", profile.slot1?.packageName)
        assertEquals(TargetType.APPLICATION, profile.slot1?.type)
        assertNull(profile.slot2)
    }

    @Test
    fun `save slot2 and delay and clearSlot1`() {
        val target1 = LaunchTarget(
            label = "App 1",
            packageName = "com.example.app1",
            className = null,
            intentUri = "intent:#Intent;action=android.intent.action.MAIN;package=com.example.app1;end",
            type = TargetType.APPLICATION
        )
        val target2 = LaunchTarget(
            label = "Shortcut 2",
            packageName = "com.example.app2",
            className = null,
            intentUri = "intent:#Intent;action=android.intent.action.VIEW;package=com.example.app2;end",
            type = TargetType.SHORTCUT
        )

        repository.saveSlot1(target1)
        repository.saveSlot2(target2)
        repository.saveDelay(450L)

        var profile = repository.getProfile()
        assertTrue(profile.isConfigured)
        assertEquals(450L, profile.delayMs)

        repository.clearSlot1()

        profile = repository.getProfile()
        assertNull(profile.slot1)
        assertNotNull(profile.slot2)
        assertFalse(profile.isConfigured)
        assertEquals(450L, profile.delayMs)
    }

    @Test
    fun `clearAll resets slots and delay to defaults`() {
        val target1 = LaunchTarget(
            label = "App 1",
            packageName = "com.example.app1",
            className = null,
            intentUri = "intent:#Intent;action=android.intent.action.MAIN;package=com.example.app1;end",
            type = TargetType.APPLICATION
        )
        repository.saveSlot1(target1)
        repository.saveDelay(600L)

        repository.clearAll()

        val profile = repository.getProfile()
        assertNull(profile.slot1)
        assertNull(profile.slot2)
        assertEquals(200L, profile.delayMs)
    }
}
