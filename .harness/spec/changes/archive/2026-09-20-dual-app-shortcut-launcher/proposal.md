## Why

在移动端日常及特定自动化场景中，用户频繁需要协同使用两个相互配合的应用程序（例如：先启动悬浮窗拍照/辅助录屏工具，紧接着启动主目标应用或特定深层业务页面）。当前系统缺乏原生高效的双App联动启动机制，手动依次查找并启动操作繁琐，且在华为 EMUI/HarmonyOS 生态中普遍存在桌面快捷方式权限静默拦截、启动过程界面闪屏突兀、多任务卡片留存多余残影等痛点。

本项目旨在构建一个体积轻巧、极简无感、高可靠的双App快捷启动器。通过透明调度中转技术与深度适配华为鸿蒙桌面生态，提供桌面一键顺序唤起双应用能力，彻底消除启动闪烁与多任务残留，保障流畅自然的协同体验。

## What Changes

- **透明调度中转 (DispatchActivity)**：新增完全透明、无过渡动画、`excludeFromRecents=true` 且 `noHistory=true` 的调度中转 Activity，通过 Handler/协程执行毫秒级（默认 100~300ms 可调）先后顺序唤起，执行完毕立即退出，无黑白屏闪烁与多任务列表残影。
- **外部快捷方式识别与持久化**：基于 Android 标准 `ACTION_CREATE_SHORTCUT` 协议，识别并提取第三方应用公开的快捷方式（包括深层页面 Intent、自定义图标与标签），同时支持普通应用启动 Intent 的提取；将 Intent 统一安全序列化为 URI 字符串持久化存储于 SharedPreferences/DataStore。
- **华为/鸿蒙桌面快捷方式与权限适配**：使用 `ShortcutManagerCompat` 规范创建桌面固定快捷方式（Pinned Shortcut）；封装华为 EMUI / HarmonyOS 桌面快捷方式权限探针，解决系统静默丢弃快捷方式创建请求的问题，支持精确检测权限状态并提供一键跳转至华为手机管家/权限设置页的引导。
- **Material 3 配置主界面 (MainActivity)**：提供现代化 Material 3 风格的管理主页面，包括 Slot 1（先启应用/快捷方式）与 Slot 2（后启应用/快捷方式）的选取配置、唤起间隔延时滑块（0~2000ms，步进 50ms）、即时测试双启按钮、生成桌面快捷方式动作栏，以及权限状态检测看板。
- **V1+V2+V3 全版本签名与 GitHub Actions CI 流水线**：配置 Gradle 构建脚本启用 `v1SigningEnabled true`、`v2SigningEnabled true`、`v3SigningEnabled true`；编写 `.github/workflows/build-apk.yml` 自动化工作流，支持在代码提交与 Release Tag 触发时通过 secrets 注入密钥并自动化对齐（zipalign）、签名和发布 APK 产物。

## Capabilities

### New Capabilities
- `transparent-dispatch`: 实现透明无感调度、无多任务残影、防界面闪烁与可配置延时的双应用先后启动中转能力。
- `shortcut-extraction`: 实现基于 ACTION_CREATE_SHORTCUT 协议与普通应用 Launcher Intent 的识别、提取、图标转换与 URI 字符串安全持久化机制。
- `huawei-adaptation`: 实现华为 EMUI / HarmonyOS 系统的桌面固定快捷方式创建、静默拦截检测及权限引导跳转适配。
- `config-ui`: 实现基于 Material 3 的交互配置界面，包含双槽位配置、延时调节、测试启动与快捷方式发布。
- `release-signing-ci`: 实现 Android V1+V2+V3 完整多版本签名方案与 GitHub Actions 持续集成自动构建发布流。

### Modified Capabilities
<!-- Existing capabilities whose REQUIREMENTS are changing (not just implementation).
     Only list here if spec-level behavior changes. Each needs a delta spec file.
     Use existing spec names from openspec/specs/. Leave empty if no requirement changes. -->

## Impact

- **系统架构**：新增标准的现代 Android 单工程结构（建议 Kotlin + Jetpack Material 3），包含 `DispatchActivity`、`MainActivity` 及相关工具/管理器。
- **权限与清单**：声明 `com.android.launcher.permission.INSTALL_SHORTCUT` 桌面快捷方式权限；针对 Android 11+ (API 30+) 添加 `<queries>` 标签或 `android.permission.QUERY_ALL_PACKAGES` 以支持枚举已安装应用及第三方快捷方式。
- **存储机制**：配置本地轻量持久化键值对存储，记录槽位配置、Intent URI、延迟时间与快捷方式元数据。
- **工程与 CI/CD**：引入 Keystore 签名配置与 `.github/workflows/build-apk.yml`，不影响现有代码库架构。
