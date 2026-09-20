package com.example.dualshortcut.util

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntentSerializerTest {

    @Test
    fun `serialize and deserialize round trip preserves intent properties`() {
        val original = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName("com.example.app", "com.example.app.TargetActivity")
            data = Uri.parse("https://example.com/item/123")
            putExtra("key_string", "test_value")
            putExtra("key_int", 42)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        val uriString = IntentSerializer.toUri(original)
        assertNotNull(uriString)
        assertTrue(uriString.startsWith("intent:"))

        val restored = IntentSerializer.fromUri(uriString)
        assertEquals(Intent.ACTION_VIEW, restored.action)
        assertEquals("com.example.app", restored.component?.packageName)
        assertEquals("com.example.app.TargetActivity", restored.component?.className)
        assertEquals("https://example.com/item/123", restored.dataString)
        assertEquals("test_value", restored.getStringExtra("key_string"))
        assertEquals(42, restored.getIntExtra("key_int", 0))
        assertTrue(restored.hasCategory(Intent.CATEGORY_DEFAULT))

        // Must ensure FLAG_ACTIVITY_NEW_TASK is set for cross-activity launch
        assertTrue((restored.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `serialize strips transient grant uri permission flags`() {
        val original = Intent(Intent.ACTION_SEND).apply {
            component = ComponentName("com.example.share", "com.example.share.ShareActivity")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val uriString = IntentSerializer.toUri(original)
        val restored = IntentSerializer.fromUri(uriString)

        // Transient flags should have been stripped before serialization
        val hasReadGrant = (restored.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        val hasWriteGrant = (restored.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
        val hasPersistableGrant = (restored.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0

        assertEquals(false, hasReadGrant)
        assertEquals(false, hasWriteGrant)
        assertEquals(false, hasPersistableGrant)
    }

    @Test
    fun `deserializing empty or invalid uri returns fallback intent`() {
        val emptyResult = IntentSerializer.fromUriOrNull("")
        assertEquals(null, emptyResult)

        val invalidResult = IntentSerializer.fromUriOrNull("not_a_valid_intent_uri")
        assertEquals(null, invalidResult)
    }
}
