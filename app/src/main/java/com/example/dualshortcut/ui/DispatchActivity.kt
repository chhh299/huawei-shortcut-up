package com.example.dualshortcut.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.example.dualshortcut.R
import com.example.dualshortcut.data.repository.LauncherConfigRepository

/**
 * 透明调度中转 Activity
 * 无背景、无动画、不进多任务列表，调度完成后立即 finish()
 */
class DispatchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableActivityTransitions()

        val repository = LauncherConfigRepository(this)
        val profile = repository.getProfile()

        if (!profile.isConfigured) {
            Toast.makeText(
                this,
                getString(R.string.warn_incomplete_slots),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val runner = DispatchRunner(
            context = this,
            onError = { errorMsg ->
                runOnUiThread {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        runner.execute(profile) {
            finish()
            disableActivityTransitions()
        }
    }

    private fun disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
