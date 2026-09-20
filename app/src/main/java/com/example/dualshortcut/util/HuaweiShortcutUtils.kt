package com.example.dualshortcut.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.dualshortcut.R
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.ui.DispatchActivity

/**
 * 针对华为 EMUI / HarmonyOS 生态的桌面快捷方式适配工具
 */
object HuaweiShortcutUtils {

    private const val TAG = "HuaweiShortcutUtils"
    const val SHORTCUT_ID_PREFIX = "dual_app_shortcut_"

    enum class PermissionStatus {
        ALLOWED,
        DENIED,
        UNKNOWN
    }

    /**
     * 判断设备厂商是否为华为或荣耀
     */
    fun isHuaweiOrHonorManufacturer(manufacturer: String = Build.MANUFACTURER): Boolean {
        val lower = manufacturer.lowercase()
        return lower.contains("huawei") || lower.contains("honor")
    }

    /**
     * 判断当前系统是否运行在华为/荣耀环境（结合厂商与 EMUI 系统属性）
     */
    fun isHuaweiDevice(): Boolean {
        if (isHuaweiOrHonorManufacturer(Build.MANUFACTURER) || isHuaweiOrHonorManufacturer(Build.BRAND)) {
            return true
        }
        return try {
            val emuiVersion = getSystemProperty("ro.build.version.emui")
            !emuiVersion.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 探测当前应用的桌面快捷方式创建权限状态
     */
    fun checkShortcutPermission(context: Context): PermissionStatus {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return PermissionStatus.DENIED
        }

        if (!isHuaweiDevice()) {
            return PermissionStatus.ALLOWED
        }

        // 华为 EMUI / HarmonyOS AppOps 探针检测
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return PermissionStatus.UNKNOWN

            // 华为通过 AppOps 控制快捷方式权限（Op 编号通常为 81 / "android:add_shortcut"）
            val checkOpMethod = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )

            // 81 是常见的 OP_ADD_SHORTCUT
            val opAddShortcut = 81
            val uid = Binder.getCallingUid()
            val pkg = context.packageName

            val mode = checkOpMethod.invoke(appOps, opAddShortcut, uid, pkg) as? Int
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> PermissionStatus.ALLOWED
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> PermissionStatus.DENIED
                else -> PermissionStatus.UNKNOWN
            }
        } catch (e: Throwable) {
            Log.d(TAG, "AppOps checkOpNoThrow failed, defaulting to UNKNOWN: ${e.message}")
            PermissionStatus.UNKNOWN
        }
    }

    /**
     * 针对华为手机管家（com.huawei.systemmanager）及特殊应用权限设置构造一键跳转 Intent
     */
    fun createPermissionSettingIntent(context: Context): Intent {
        val packageManager = context.packageManager

        // 尝试 1: 华为手机管家权限管理主界面
        val intentEmuiPermission = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.permissionmanager.ui.MainActivity"
            )
        }
        if (isIntentResolvable(packageManager, intentEmuiPermission)) {
            return intentEmuiPermission
        }

        // 尝试 2: 华为系统管家应用控制活动
        val intentEmuiAppControl = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            )
        }
        if (isIntentResolvable(packageManager, intentEmuiAppControl)) {
            return intentEmuiAppControl
        }

        // 尝试 3: 特殊快捷方式设置广播或设置页面
        val intentEmuiShortcut = Intent("com.huawei.android.intent.action.SHORTCUT_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (isIntentResolvable(packageManager, intentEmuiShortcut)) {
            return intentEmuiShortcut
        }

        // 降级策略: 唤起当前 App 的系统标准设置详情页
        return createAppDetailsIntent(context)
    }

    /**
     * 降级应用详情设置页 Intent
     */
    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 构建并向系统发送固定快捷方式（Pinned Shortcut）请求
     */
    fun requestPinShortcut(
        context: Context,
        profile: LaunchProfile,
        customTitle: String? = null
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Log.w(TAG, "requestPinShortcut is not supported by launcher")
            return false
        }

        val title = if (!customTitle.isNullOrBlank()) {
            customTitle
        } else {
            val label1 = profile.slot1?.label ?: "应用1"
            val label2 = profile.slot2?.label ?: "应用2"
            "$label1 + $label2"
        }

        // 快捷方式启动 Intent 目标指向 DispatchActivity
        val launchIntent = Intent(context, DispatchActivity::class.java).apply {
            action = "com.example.dualshortcut.ACTION_DISPATCH"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val shortcutId = "${SHORTCUT_ID_PREFIX}${System.currentTimeMillis()}"
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)

        val pinShortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(title)
            .setLongLabel(title)
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
    }

    private fun isIntentResolvable(pm: PackageManager, intent: Intent): Boolean {
        return try {
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            activities.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String
        } catch (_: Exception) {
            null
        }
    }
}
