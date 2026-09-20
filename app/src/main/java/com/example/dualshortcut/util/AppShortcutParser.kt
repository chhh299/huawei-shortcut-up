package com.example.dualshortcut.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream

/**
 * 现代 Android 应用长按快捷方式（App Shortcuts / shortcuts.xml）及内部 Activity 提取解析工具
 */
object AppShortcutParser {

    private const val TAG = "AppShortcutParser"

    /**
     * 深度解析指定应用中声明的所有长按快捷方式（如“思源码”、“扫一扫”等）
     */
    fun getShortcutsForPackage(context: Context, packageName: String, appLabel: String): List<LaunchTarget> {
        val pm = context.packageManager
        val list = mutableListOf<LaunchTarget>()

        try {
            val pkgInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            )
            val activities = pkgInfo.activities ?: return emptyList()
            val targetResources = pm.getResourcesForApplication(packageName)

            for (activity in activities) {
                val parser: XmlResourceParser = activity.loadXmlMetaData(pm, "android.app.shortcuts") ?: continue

                var eventType = parser.eventType
                var currentShortcutId: String? = null
                var currentLabel: String? = null
                var currentIcon: Drawable? = null
                var currentIntent: Intent? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "shortcut" -> {
                                currentShortcutId = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutId")

                                val labelRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "shortcutShortLabel", 0)
                                currentLabel = if (labelRes != 0) {
                                    try { targetResources.getString(labelRes) } catch (e: Exception) { null }
                                } else {
                                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutShortLabel")
                                }

                                if (currentLabel.isNullOrBlank()) {
                                    val longLabelRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "shortcutLongLabel", 0)
                                    if (longLabelRes != 0) {
                                        try { currentLabel = targetResources.getString(longLabelRes) } catch (e: Exception) { null }
                                    }
                                }

                                val iconRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "icon", 0)
                                currentIcon = if (iconRes != 0) {
                                    try { targetResources.getDrawable(iconRes, null) } catch (e: Exception) { null }
                                } else null
                            }
                            "intent" -> {
                                val action = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "action") ?: Intent.ACTION_VIEW
                                val targetPkg = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetPackage") ?: packageName
                                val targetClass = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetClass")
                                val dataUri = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "data")

                                val intent = Intent(action).apply {
                                    `package` = targetPkg
                                    if (!targetClass.isNullOrBlank()) {
                                        setClassName(targetPkg, targetClass)
                                    }
                                    if (!dataUri.isNullOrBlank()) {
                                        data = Uri.parse(dataUri)
                                    }
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                currentIntent = intent
                            }
                            "extra" -> {
                                val extraName = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                                val extraValue = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "value")
                                if (!extraName.isNullOrBlank() && extraValue != null) {
                                    currentIntent?.putExtra(extraName, extraValue)
                                }
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.name == "shortcut") {
                            if (!currentShortcutId.isNullOrBlank() && currentIntent != null) {
                                val itemLabel = currentLabel ?: currentShortcutId
                                val iconBase64 = drawableToBase64(currentIcon)
                                val uri = IntentSerializer.toUri(currentIntent)

                                list.add(
                                    LaunchTarget(
                                        label = "$appLabel - $itemLabel",
                                        packageName = packageName,
                                        className = currentIntent.component?.className,
                                        intentUri = uri,
                                        type = TargetType.SHORTCUT,
                                        iconBase64 = iconBase64
                                    )
                                )
                            }
                            currentShortcutId = null
                            currentLabel = null
                            currentIcon = null
                            currentIntent = null
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse shortcuts for $packageName", e)
        }

        return list
    }

    /**
     * 获取应用的所有内部页面 Activity 列表（供高级/自定义页面挑选）
     */
    fun getExportedActivitiesForPackage(context: Context, packageName: String, appLabel: String): List<LaunchTarget> {
        val pm = context.packageManager
        val list = mutableListOf<LaunchTarget>()

        try {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            val activities = pkgInfo.activities ?: return emptyList()

            for (activity in activities) {
                val className = activity.name
                val simpleName = className.substringAfterLast('.')
                val activityLabel = try {
                    val lbl = activity.loadLabel(pm).toString()
                    if (lbl.isNotBlank() && lbl != className) lbl else simpleName
                } catch (e: Exception) {
                    simpleName
                }

                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(packageName, className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val uri = IntentSerializer.toUri(intent)

                list.add(
                    LaunchTarget(
                        label = "$appLabel - $activityLabel",
                        packageName = packageName,
                        className = className,
                        intentUri = uri,
                        type = TargetType.SHORTCUT,
                        iconBase64 = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query activities for $packageName", e)
        }

        return list
    }

    private fun drawableToBase64(drawable: Drawable?): String? {
        if (drawable == null) return null
        return try {
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
