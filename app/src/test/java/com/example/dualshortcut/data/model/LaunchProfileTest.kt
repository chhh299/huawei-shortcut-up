package com.example.dualshortcut.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchProfileTest {

    @Test
    fun `isConfigured returns false when slot1 is missing`() {
        val target2 = LaunchTarget(
            label = "App Two",
            packageName = "com.example.two",
            className = "com.example.two.MainActivity",
            intentUri = "#Intent;action=android.intent.action.MAIN;package=com.example.two;end",
            type = TargetType.APPLICATION
        )
        val profile = LaunchProfile(
            slot1 = null,
            slot2 = target2,
            delayMs = 200
        )

        assertFalse(profile.isConfigured)
    }

    @Test
    fun `isConfigured returns false when slot2 is missing`() {
        val target1 = LaunchTarget(
            label = "App One",
            packageName = "com.example.one",
            className = "com.example.one.MainActivity",
            intentUri = "#Intent;action=android.intent.action.MAIN;package=com.example.one;end",
            type = TargetType.APPLICATION
        )
        val profile = LaunchProfile(
            slot1 = target1,
            slot2 = null,
            delayMs = 200
        )

        assertFalse(profile.isConfigured)
    }

    @Test
    fun `isConfigured returns true when both slots are present`() {
        val target1 = LaunchTarget(
            label = "App One",
            packageName = "com.example.one",
            className = "com.example.one.MainActivity",
            intentUri = "#Intent;action=android.intent.action.MAIN;package=com.example.one;end",
            type = TargetType.APPLICATION
        )
        val target2 = LaunchTarget(
            label = "App Two Shortcut",
            packageName = "com.example.two",
            className = null,
            intentUri = "#Intent;action=android.intent.action.VIEW;package=com.example.two;end",
            type = TargetType.SHORTCUT
        )
        val profile = LaunchProfile(
            slot1 = target1,
            slot2 = target2,
            delayMs = 350
        )

        assertTrue(profile.isConfigured)
        assertEquals(350L, profile.delayMs)
        assertEquals(TargetType.APPLICATION, profile.slot1?.type)
        assertEquals(TargetType.SHORTCUT, profile.slot2?.type)
    }

    @Test
    fun `delay is constrained to valid bounds 0 to 2000 ms`() {
        val profileLower = LaunchProfile(slot1 = null, slot2 = null, delayMs = -100)
        assertEquals(0L, profileLower.safeDelayMs)

        val profileUpper = LaunchProfile(slot1 = null, slot2 = null, delayMs = 5000)
        assertEquals(2000L, profileUpper.safeDelayMs)

        val profileNormal = LaunchProfile(slot1 = null, slot2 = null, delayMs = 250)
        assertEquals(250L, profileNormal.safeDelayMs)
    }
}
