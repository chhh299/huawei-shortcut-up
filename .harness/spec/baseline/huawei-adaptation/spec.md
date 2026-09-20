# huawei-adaptation Specification

## Purpose
TBD - created by archiving change dual-app-shortcut-launcher. Update Purpose after archive.
## Requirements
### Requirement: 桌面固定快捷方式创建
系统 MUST 支持通过 `ShortcutManagerCompat.requestPinShortcut` 为配置好的双启动任务向桌面发送创建固定快捷方式（Pinned Shortcut）请求，传入唯一的 Shortcut ID、自定义标题、合成图标以及指向 `DispatchActivity` 的启动 Intent。

#### Scenario: 发起桌面快捷方式固定请求
- **WHEN** 用户在主界面点击“创建桌面快捷方式”
- **THEN** 系统构建 `ShortcutInfoCompat` 对象，调用 `requestPinShortcut` 发送请求，系统弹出桌面添加确认弹窗或由桌面直接添加

### Requirement: 华为 EMUI/HarmonyOS 桌面快捷方式权限检测
系统 MUST 针对华为及荣耀设备（检测系统制造商为 HUAWEI 或 HONOR，及搭载 EMUI / HarmonyOS 的环境）提供桌面快捷方式权限状态探针，检测是否已被系统安全管家静默禁止创建快捷方式。

#### Scenario: 华为设备权限已被允许
- **WHEN** 运行在华为/鸿蒙设备上且系统已授予桌面快捷方式创建权限
- **THEN** 权限探针返回 ALLOWED 状态，界面允许直接执行快捷方式创建且不弹权限警示

#### Scenario: 华为设备检测到权限被禁止或可能静默拦截
- **WHEN** 运行在华为/鸿蒙设备上但快捷方式权限处于 DENIED 或受限状态
- **THEN** 系统拦截无感静默失败行为，在界面弹出警示对话框提示用户华为系统需手动开启“桌面快捷方式”权限

### Requirement: 一键跳转华为系统快捷方式权限设置页
当华为系统权限缺失时，系统 SHALL 提供一键跳转能力，根据 EMUI / HarmonyOS 版本特性，优先构造跳转至华为手机管家（`com.huawei.systemmanager`）应用权限管理页面或系统“特殊应用权限 - 创建桌面快捷方式”页面的显式 Intent；若未匹配到特定组件则降级跳转至系统标准应用详情页。

#### Scenario: 用户点击去授权
- **WHEN** 用户在权限提示对话框中点击“前往开启权限”
- **THEN** 系统调起华为手机管家应用权限设置页或当前应用详情页，引导用户开启桌面快捷方式开关

