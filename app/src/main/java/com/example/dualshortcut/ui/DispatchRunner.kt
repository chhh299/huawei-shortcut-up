package com.example.dualshortcut.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.util.IntentSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 负责顺序双启调度的执行器
 */
class DispatchRunner(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val launcher: (Intent) -> Unit = { intent -> context.startActivity(intent) },
    private val onError: (String) -> Unit = {}
) {

    private val scope = CoroutineScope(dispatcher)

    /**
     * 顺序执行双应用启动
     */
    fun execute(profile: LaunchProfile, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                // 1. 唤起槽位 1
                profile.slot1?.let { target ->
                    launchSingleTarget(target)
                }

                // 2. 毫秒级非阻塞延迟等待
                val delayTime = profile.safeDelayMs
                if (delayTime > 0) {
                    delay(delayTime)
                }

                // 3. 唤起槽位 2
                profile.slot2?.let { target ->
                    launchSingleTarget(target)
                }
            } finally {
                onComplete()
            }
        }
    }

    private fun launchSingleTarget(target: LaunchTarget) {
        val intent = IntentSerializer.fromUriOrNull(target.intentUri)
        if (intent == null) {
            val msg = "目标应用 Intent 解析失败：${target.label}"
            Log.e(TAG, msg)
            onError(msg)
            return
        }

        try {
            launcher(intent)
        } catch (e: ActivityNotFoundException) {
            val msg = "无法找到目标应用组件：${target.label} (${target.packageName})"
            Log.e(TAG, msg, e)
            onError(msg)
        } catch (e: Throwable) {
            val msg = "启动目标失败：${target.label} (${e.message})"
            Log.e(TAG, msg, e)
            onError(msg)
        }
    }

    companion object {
        private const val TAG = "DispatchRunner"
    }
}
