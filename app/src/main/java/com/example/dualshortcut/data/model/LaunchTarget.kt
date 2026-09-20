package com.example.dualshortcut.data.model

/**
 * 单个槽位的启动目标配置
 *
 * @property label 启动目标的用户友好显示名称
 * @property packageName 目标应用的包名
 * @property className 目标启动 Activity 完整类名（可选）
 * @property intentUri 通过 Intent.toUri 序列化后的标准规范 URI 字符串
 * @property type 目标类型：普通应用或快捷方式
 * @property iconBase64 可选的自定义图标数据（Base64 编码 PNG/JPEG），用于第三方快捷方式图标持久化
 */
data class LaunchTarget(
    val label: String,
    val packageName: String,
    val className: String? = null,
    val intentUri: String,
    val type: TargetType = TargetType.APPLICATION,
    val iconBase64: String? = null
)
