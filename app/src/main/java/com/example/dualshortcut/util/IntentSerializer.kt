package com.example.dualshortcut.util

import android.content.Intent
import android.util.Log

/**
 * Intent 与标准 URI 字符串之间的高保真序列化与反序列化工具
 */
object IntentSerializer {

    private const val TAG = "IntentSerializer"

    // 需要剥除的临时授权 Flags，防止跨进程重启时权限失效
    private val TRANSIENT_FLAGS_MASK = (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            ).inv()

    /**
     * 将 Intent 序列化为标准的 URI 字符串（Intent.URI_INTENT_SCHEME）
     * 自动剥离临时 URI 授权标志
     */
    fun toUri(intent: Intent): String {
        val sanitized = Intent(intent).apply {
            flags = flags and TRANSIENT_FLAGS_MASK
        }
        return sanitized.toUri(Intent.URI_INTENT_SCHEME)
    }

    /**
     * 从 URI 字符串解析并还原为 Intent 对象，并强制注入 FLAG_ACTIVITY_NEW_TASK
     */
    fun fromUri(uriString: String): Intent {
        val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    /**
     * 安全解析 URI 字符串，失败或为空时返回 null
     */
    fun fromUriOrNull(uriString: String?): Intent? {
        if (uriString.isNullOrBlank()) return null
        return try {
            fromUri(uriString)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to parse intent URI: $uriString", e)
            null
        }
    }
}
