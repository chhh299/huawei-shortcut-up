package com.example.dualshortcut.data.model

/**
 * 双启动器完整配置画像
 *
 * @property slot1 槽位 1（先启动目标，例如悬浮辅助工具）
 * @property slot2 槽位 2（后启动目标，例如主业务应用）
 * @property delayMs 两次启动之间的毫秒间隔（默认 200ms）
 */
data class LaunchProfile(
    val slot1: LaunchTarget?,
    val slot2: LaunchTarget?,
    val delayMs: Long = DEFAULT_DELAY_MS
) {
    /**
     * 两个槽位均已配置时返回 true
     */
    val isConfigured: Boolean
        get() = slot1 != null && slot2 != null

    /**
     * 约束在安全有效范围 0~2000 毫秒内的延迟值
     */
    val safeDelayMs: Long
        get() = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

    companion object {
        const val DEFAULT_DELAY_MS = 200L
        const val MIN_DELAY_MS = 0L
        const val MAX_DELAY_MS = 2000L
    }
}
