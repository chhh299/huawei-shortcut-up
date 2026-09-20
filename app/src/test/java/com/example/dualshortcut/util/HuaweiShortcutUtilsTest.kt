package com.example.dualshortcut.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HuaweiShortcutUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `isHuaweiOrHonor matches huawei and honor casing variants`() {
        assertTrue(HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("HUAWEI"))
        assertTrue(HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("Huawei"))
        assertTrue(HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("HONOR"))
        assertTrue(HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("Honor"))
        assertEquals(false, HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("Google"))
        assertEquals(false, HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("Xiaomi"))
        assertEquals(false, HuaweiShortcutUtils.isHuaweiOrHonorManufacturer("Samsung"))
    }

    @Test
    fun `createHuaweiPermissionSettingIntent returns valid intent with new task flag`() {
        val intent = HuaweiShortcutUtils.createPermissionSettingIntent(context)
        assertNotNull(intent)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `fallback permission setting intent targets package settings`() {
        val fallbackIntent = HuaweiShortcutUtils.createAppDetailsIntent(context)
        assertNotNull(fallbackIntent)
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, fallbackIntent.action)
        assertEquals("package:" + context.packageName, fallbackIntent.dataString)
    }
}
