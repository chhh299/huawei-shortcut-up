package com.example.dualshortcut.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dualshortcut.R
import com.example.dualshortcut.data.model.LaunchProfile
import com.example.dualshortcut.data.model.LaunchTarget
import com.example.dualshortcut.data.model.TargetType
import com.example.dualshortcut.data.repository.LauncherConfigRepository
import com.example.dualshortcut.databinding.ActivityMainBinding
import com.example.dualshortcut.databinding.DialogAppPickerBinding
import com.example.dualshortcut.util.AppShortcutParser
import com.example.dualshortcut.util.HuaweiShortcutUtils
import com.example.dualshortcut.util.ShortcutPickerHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: LauncherConfigRepository

    private var targetSelectingSlot: Int = 1

    // 接收第三方快捷方式选择回调
    private val shortcutResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val target = ShortcutPickerHelper.extractShortcutResult(result.data)
                if (target != null) {
                    if (targetSelectingSlot == 1) {
                        repository.saveSlot1(target)
                    } else {
                        repository.saveSlot2(target)
                    }
                    renderUI()
                    showSnackbar("已成功提取快捷方式：${target.label}")
                } else {
                    showSnackbar("未能识别有效的快捷方式数据")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LauncherConfigRepository(this)

        setupListeners()
        renderUI()
    }

    override fun onResume() {
        super.onResume()
        checkHuaweiPermissionStatus()
    }

    private fun setupListeners() {
        // 槽位 1 选择与清除
        binding.btnSlot1SelectApp.setOnClickListener {
            targetSelectingSlot = 1
            showAppPickerDialog()
        }
        binding.btnSlot1SelectShortcut.setOnClickListener {
            targetSelectingSlot = 1
            val pickerIntent = ShortcutPickerHelper.createShortcutPickerIntent(getString(R.string.dialog_select_type_title))
            shortcutResultLauncher.launch(pickerIntent)
        }
        binding.btnSlot1Clear.setOnClickListener {
            repository.clearSlot1()
            renderUI()
        }

        // 槽位 2 选择与清除
        binding.btnSlot2SelectApp.setOnClickListener {
            targetSelectingSlot = 2
            showAppPickerDialog()
        }
        binding.btnSlot2SelectShortcut.setOnClickListener {
            targetSelectingSlot = 2
            val pickerIntent = ShortcutPickerHelper.createShortcutPickerIntent(getString(R.string.dialog_select_type_title))
            shortcutResultLauncher.launch(pickerIntent)
        }
        binding.btnSlot2Clear.setOnClickListener {
            repository.clearSlot2()
            renderUI()
        }

        // 延迟配置与直接输入
        binding.btnEditDelay.setOnClickListener {
            showCustomDelayDialog()
        }

        binding.chipDelay0.setOnClickListener { updateDelayValue(0L) }
        binding.chipDelay10.setOnClickListener { updateDelayValue(10L) }
        binding.chipDelay20.setOnClickListener { updateDelayValue(20L) }
        binding.chipDelay30.setOnClickListener { updateDelayValue(30L) }
        binding.chipDelay50.setOnClickListener { updateDelayValue(50L) }
        binding.chipDelay100.setOnClickListener { updateDelayValue(100L) }

        // 精细延迟滑动条（1ms步进）
        binding.sliderDelay.addOnChangeListener { _, value, fromUser ->
            val delayMs = value.toLong()
            binding.btnEditDelay.text = "${delayMs} ms (点击修改)"
            if (fromUser) {
                repository.saveDelay(delayMs)
            }
        }

        // 即时测试双启
        binding.btnTestLaunch.setOnClickListener {
            val profile = repository.getProfile()
            if (!profile.isConfigured) {
                showSnackbar(getString(R.string.warn_incomplete_slots))
                return@setOnClickListener
            }

            showSnackbar("正在顺序测试调起应用…")
            val runner = DispatchRunner(
                context = this,
                onError = { errorMsg ->
                    runOnUiThread { showSnackbar(errorMsg) }
                }
            )
            runner.execute(profile)
        }

        // 添加到桌面快捷方式
        binding.btnCreateShortcut.setOnClickListener {
            val profile = repository.getProfile()
            if (!profile.isConfigured) {
                showSnackbar(getString(R.string.warn_incomplete_slots))
                return@setOnClickListener
            }

            // 华为生态权限拦截前置检查
            val permissionStatus = HuaweiShortcutUtils.checkShortcutPermission(this)
            if (HuaweiShortcutUtils.isHuaweiDevice() && permissionStatus == HuaweiShortcutUtils.PermissionStatus.DENIED) {
                showHuaweiPermissionDialog(profile)
            } else {
                performCreateShortcut(profile)
            }
        }

        // 华为权限修复横幅按钮
        binding.btnFixPermission.setOnClickListener {
            val intent = HuaweiShortcutUtils.createPermissionSettingIntent(this)
            startActivity(intent)
        }
    }

    private fun renderUI() {
        val profile = repository.getProfile()

        // 渲染槽位 1
        renderSlot(
            slotTarget = profile.slot1,
            tvName = binding.tvSlot1Name,
            tvDetail = binding.tvSlot1Detail,
            ivIcon = binding.ivSlot1Icon,
            chipType = binding.chipSlot1Type,
            btnClear = binding.btnSlot1Clear
        )

        // 渲染槽位 2
        renderSlot(
            slotTarget = profile.slot2,
            tvName = binding.tvSlot2Name,
            tvDetail = binding.tvSlot2Detail,
            ivIcon = binding.ivSlot2Icon,
            chipType = binding.chipSlot2Type,
            btnClear = binding.btnSlot2Clear
        )

        // 渲染延迟
        val safeDelay = profile.safeDelayMs.coerceIn(0L, 1000L).toFloat()
        if (binding.sliderDelay.value != safeDelay) {
            binding.sliderDelay.value = safeDelay
        }
        binding.btnEditDelay.text = "${safeDelay.toInt()} ms (点击修改)"
    }

    private fun renderSlot(
        slotTarget: LaunchTarget?,
        tvName: android.widget.TextView,
        tvDetail: android.widget.TextView,
        ivIcon: android.widget.ImageView,
        chipType: com.google.android.material.chip.Chip,
        btnClear: View
    ) {
        if (slotTarget != null) {
            tvName.text = slotTarget.label
            tvDetail.text = slotTarget.packageName
            tvDetail.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE
            chipType.visibility = View.VISIBLE

            when (slotTarget.type) {
                TargetType.APPLICATION -> {
                    chipType.text = getString(R.string.type_app)
                    loadAppIcon(slotTarget.packageName, ivIcon)
                }
                TargetType.SHORTCUT -> {
                    chipType.text = getString(R.string.type_shortcut)
                    loadShortcutIcon(slotTarget, ivIcon)
                }
            }
        } else {
            tvName.text = getString(R.string.slot_empty_hint)
            tvDetail.visibility = View.GONE
            btnClear.visibility = View.GONE
            chipType.visibility = View.GONE
            ivIcon.setImageResource(R.drawable.ic_apps)
        }
    }

    private fun loadAppIcon(packageName: String, imageView: android.widget.ImageView) {
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            imageView.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            imageView.setImageResource(R.drawable.ic_apps)
        }
    }

    private fun loadShortcutIcon(target: LaunchTarget, imageView: android.widget.ImageView) {
        if (!target.iconBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(target.iconBase64, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    imageView.setImageDrawable(BitmapDrawable(resources, bitmap))
                    return
                }
            } catch (e: Exception) {}
        }
        loadAppIcon(target.packageName, imageView)
    }

    private fun checkHuaweiPermissionStatus() {
        if (!HuaweiShortcutUtils.isHuaweiDevice()) {
            binding.cardPermissionBanner.visibility = View.GONE
            return
        }

        val status = HuaweiShortcutUtils.checkShortcutPermission(this)
        if (status == HuaweiShortcutUtils.PermissionStatus.DENIED) {
            binding.cardPermissionBanner.visibility = View.VISIBLE
            binding.tvPermissionBannerText.text = getString(R.string.permission_status_denied)
        } else {
            binding.cardPermissionBanner.visibility = View.GONE
        }
    }

    private fun showHuaweiPermissionDialog(profile: LaunchProfile) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.huawei_permission_title)
            .setMessage(R.string.huawei_permission_message)
            .setPositiveButton(R.string.btn_go_to_permission) { _, _ ->
                val intent = HuaweiShortcutUtils.createPermissionSettingIntent(this)
                startActivity(intent)
            }
            .setNegativeButton(R.string.btn_continue_anyway) { _, _ ->
                performCreateShortcut(profile)
            }
            .setNeutralButton(R.string.btn_cancel, null)
            .show()
    }

    private fun performCreateShortcut(profile: LaunchProfile) {
        val success = HuaweiShortcutUtils.requestPinShortcut(this, profile)
        if (success) {
            showSnackbar(getString(R.string.shortcut_created_success))
        } else {
            showSnackbar(getString(R.string.shortcut_created_failed))
        }
    }

    private fun showCustomDelayDialog() {
        val current = repository.getProfile().safeDelayMs
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            hint = "请输入 0~1000 之间的毫秒数值"
        }
        val container = android.widget.FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("自定义启动延迟 (毫秒)")
            .setMessage("提示：0 ms 为无感并发调起；推荐设置 10~30 ms 获得极速平滑体验。")
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val entered = input.text.toString().trim().toLongOrNull() ?: current
                val safe = entered.coerceIn(0L, 1000L)
                updateDelayValue(safe)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun updateDelayValue(delayMs: Long) {
        repository.saveDelay(delayMs)
        renderUI()
    }

    private fun showAppPickerDialog() {
        val dialogBinding = DialogAppPickerBinding.inflate(layoutInflater)
        val adapter = AppPickerAdapter { entry ->
            val defaultTarget = ShortcutPickerHelper.createFromApplication(
                label = entry.label,
                packageName = entry.packageName,
                className = entry.className
            )

            val shortcuts = AppShortcutParser.getShortcutsForPackage(this, entry.packageName, entry.label)
            val options = mutableListOf<String>()
            val targets = mutableListOf<LaunchTarget>()

            // 1. 默认主界面
            options.add("【默认】启动应用主界面")
            targets.add(defaultTarget)

            // 2. 长按快捷功能（如思源码、扫一扫等）
            for (sc in shortcuts) {
                val subLabel = sc.label.substringAfter(" - ")
                options.add("【快捷功能】$subLabel")
                targets.add(sc)
            }

            // 3. 高级页面选项
            options.add("【高级】浏览全部内部页面 (Activity)")

            MaterialAlertDialogBuilder(this)
                .setTitle("请选择【${entry.label}】启动目标")
                .setItems(options.toTypedArray()) { _, which ->
                    if (which < targets.size) {
                        applySelectedTarget(targets[which])
                        dialog?.dismiss()
                    } else {
                        showActivitiesDialog(entry)
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        dialogBinding.rvApps.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvApps.adapter = adapter
        dialogBinding.progressBar.visibility = View.VISIBLE

        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_app_list_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialogBinding.etSearch.doAfterTextChanged { text ->
            adapter.filter(text?.toString().orEmpty())
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val appList = queryInstalledApps()
            withContext(Dispatchers.Main) {
                dialogBinding.progressBar.visibility = View.GONE
                adapter.submitList(appList)
            }
        }

        dialog?.show()
    }

    private fun showActivitiesDialog(entry: AppEntry) {
        val activities = AppShortcutParser.getExportedActivitiesForPackage(this, entry.packageName, entry.label)
        if (activities.isEmpty()) {
            showSnackbar("该应用未导出可调起的功能页面")
            return
        }

        val names = activities.map { it.label.substringAfter(" - ") }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("【${entry.label}】内部功能页面")
            .setItems(names) { _, which ->
                applySelectedTarget(activities[which])
                dialog?.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun applySelectedTarget(target: LaunchTarget) {
        if (targetSelectingSlot == 1) {
            repository.saveSlot1(target)
        } else {
            repository.saveSlot2(target)
        }
        renderUI()
        showSnackbar("已成功选择：${target.label}")
    }

    private var dialog: androidx.appcompat.app.AlertDialog? = null

    private fun queryInstalledApps(): List<AppEntry> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            // 排除自身
            if (pkg == packageName) return@mapNotNull null
            val label = info.loadLabel(pm).toString()
            val className = info.activityInfo.name
            val icon = info.loadIcon(pm)
            AppEntry(label, pkg, className, icon)
        }.sortedBy { it.label.lowercase() }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
