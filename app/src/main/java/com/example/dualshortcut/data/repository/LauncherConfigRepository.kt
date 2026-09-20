package com.example.dualshortcut.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType

/**
 * 启动器配置本地持久化仓库，基于 SharedPreferences 存储槽位及延迟配置
 */
class LauncherConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 读取当前保存的双启动配置画像
     */
    fun getProfile(): LaunchProfile {
        val slot1 = readSlot(SLOT_1_PREFIX)
        val slot2 = readSlot(SLOT_2_PREFIX)
        val delayMs = prefs.getLong(KEY_DELAY_MS, LaunchProfile.DEFAULT_DELAY_MS)
        return LaunchProfile(slot1, slot2, delayMs)
    }

    /**
     * 保存完整画像
     */
    fun saveProfile(profile: LaunchProfile) {
        prefs.edit().apply {
            writeSlot(this, SLOT_1_PREFIX, profile.slot1)
            writeSlot(this, SLOT_2_PREFIX, profile.slot2)
            putLong(KEY_DELAY_MS, profile.safeDelayMs)
            apply()
        }
    }

    /**
     * 单独保存槽位 1
     */
    fun saveSlot1(target: LaunchTarget?) {
        prefs.edit().apply {
            writeSlot(this, SLOT_1_PREFIX, target)
            apply()
        }
    }

    /**
     * 单独保存槽位 2
     */
    fun saveSlot2(target: LaunchTarget?) {
        prefs.edit().apply {
            writeSlot(this, SLOT_2_PREFIX, target)
            apply()
        }
    }

    /**
     * 保存延迟毫秒数
     */
    fun saveDelay(delayMs: Long) {
        val safeDelay = delayMs.coerceIn(LaunchProfile.MIN_DELAY_MS, LaunchProfile.MAX_DELAY_MS)
        prefs.edit().putLong(KEY_DELAY_MS, safeDelay).apply()
    }

    /**
     * 清空槽位 1
     */
    fun clearSlot1() {
        saveSlot1(null)
    }

    /**
     * 清空槽位 2
     */
    fun clearSlot2() {
        saveSlot2(null)
    }

    /**
     * 清空所有配置重置为初始状态
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun readSlot(prefix: String): LaunchTarget? {
        val uri = prefs.getString("${prefix}_uri", null) ?: return null
        val label = prefs.getString("${prefix}_label", "") ?: ""
        val packageName = prefs.getString("${prefix}_pkg", "") ?: ""
        val className = prefs.getString("${prefix}_class", null)
        val typeStr = prefs.getString("${prefix}_type", TargetType.APPLICATION.name)
        val iconBase64 = prefs.getString("${prefix}_icon", null)
        val type = try {
            TargetType.valueOf(typeStr ?: TargetType.APPLICATION.name)
        } catch (_: Exception) {
            TargetType.APPLICATION
        }
        return LaunchTarget(
            label = label,
            packageName = packageName,
            className = className,
            intentUri = uri,
            type = type,
            iconBase64 = iconBase64
        )
    }

    private fun writeSlot(editor: SharedPreferences.Editor, prefix: String, target: LaunchTarget?) {
        if (target == null) {
            editor.remove("${prefix}_uri")
            editor.remove("${prefix}_label")
            editor.remove("${prefix}_pkg")
            editor.remove("${prefix}_class")
            editor.remove("${prefix}_type")
            editor.remove("${prefix}_icon")
        } else {
            editor.putString("${prefix}_uri", target.intentUri)
            editor.putString("${prefix}_label", target.label)
            editor.putString("${prefix}_pkg", target.packageName)
            editor.putString("${prefix}_class", target.className)
            editor.putString("${prefix}_type", target.type.name)
            editor.putString("${prefix}_icon", target.iconBase64)
        }
    }

    companion object {
        private const val PREFS_NAME = "dual_launcher_prefs"
        private const val SLOT_1_PREFIX = "slot_1"
        private const val SLOT_2_PREFIX = "slot_2"
        private const val KEY_DELAY_MS = "launch_delay_ms"
    }
}
