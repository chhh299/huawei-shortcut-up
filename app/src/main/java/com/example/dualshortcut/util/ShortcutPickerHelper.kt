package com.example.dualshortcut.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType
import java.io.ByteArrayOutputStream

/**
 * 针对 Android 标准 ACTION_CREATE_SHORTCUT 协议的快捷方式唤起与结果解析辅助类
 */
object ShortcutPickerHelper {

    private const val TAG = "ShortcutPickerHelper"

    /**
     * 创建用于调起支持快捷方式创建的第三方应用选择器的 Intent
     */
    fun createShortcutPickerIntent(title: String = "选择快捷方式"): Intent {
        val pickerIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        return Intent.createChooser(pickerIntent, title)
    }

    /**
     * 从第三方应用 onActivityResult 返回的数据中提取快捷方式配置
     */
    fun extractShortcutResult(resultData: Intent?): LaunchTarget? {
        if (resultData == null) return null

        // 提取目标真实 Intent
        val targetIntent = resultData.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
        if (targetIntent == null) {
            Log.w(TAG, "Result Intent does not contain EXTRA_SHORTCUT_INTENT")
            return null
        }

        // 提取显示名称
        val name = resultData.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
            ?: targetIntent.component?.shortClassName
            ?: "快捷方式"

        // 提取图标 (Bitmap 或 Resource)
        var iconBase64: String? = null
        val iconBitmap = resultData.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
        if (iconBitmap != null) {
            iconBase64 = bitmapToBase64(iconBitmap)
        }

        val pkg = targetIntent.`package`
            ?: targetIntent.component?.packageName
            ?: ""
        val cls = targetIntent.component?.className

        // 序列化 Intent
        val uri = IntentSerializer.toUri(targetIntent)

        return LaunchTarget(
            label = name,
            packageName = pkg,
            className = cls,
            intentUri = uri,
            type = TargetType.SHORTCUT,
            iconBase64 = iconBase64
        )
    }

    /**
     * 从普通应用包名和启动入口生成 LaunchTarget
     */
    fun createFromApplication(label: String, packageName: String, className: String?): LaunchTarget {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            if (!className.isNullOrBlank()) {
                setClassName(packageName, className)
            } else {
                `package` = packageName
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val uri = IntentSerializer.toUri(launchIntent)
        return LaunchTarget(
            label = label,
            packageName = packageName,
            className = className,
            intentUri = uri,
            type = TargetType.APPLICATION,
            iconBase64 = null
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to encode icon bitmap to base64", e)
            null
        }
    }
}
