## 1. 项目基础与工程搭建

- [x] 1.1 初始化 Android Gradle 工程骨架，配置 minSdkVersion (26) 与 targetSdkVersion (34)
- [x] 1.2 添加 AndroidX Core, AppCompat, Material 3, Preferences/DataStore 与 Kotlin 协程基础依赖
- [x] 1.3 在 AndroidManifest.xml 中配置快捷方式权限、包可见性声明 `<queries>` 及相关属性

## 2. 调度引擎与透明中转组件 (Transparent Dispatch Engine)

- [x] 2.1 定义专用的无边框无动画透明主题，配置 `android:windowAnimationStyle="@null"`
- [x] 2.2 实现 `DispatchActivity`，在清单中配置 `excludeFromRecents="true"`、`noHistory="true"` 与 `exported="true"`
- [x] 2.3 在 `DispatchActivity` 中实现读取槽位配置并顺序执行毫秒级（可调）延迟唤起链路
- [x] 2.4 实现目标应用未安装或组件失效时的 `ActivityNotFoundException` 异常捕获与用户友好 Toast 提示

## 3. 外部快捷方式识别与持久化 (Shortcut Extraction & Persistence)

- [x] 3.1 实现支持 `Intent.ACTION_CREATE_SHORTCUT` 协议的第三方应用快捷方式选取与结果提取器
- [x] 3.2 实现查询已安装常规应用列表的对话框，提取主入口 Intent、名称与图标
- [x] 3.3 实现 `IntentSerializer` 工具类，支持 Intent 与 URI 字符串（`Intent.toUri` / `Intent.parseUri`）的高保真转换
- [x] 3.4 实现 `LauncherConfigRepository` 配置持久化类，安全存储 Slot 1、Slot 2 的配置信息与延迟参数

## 4. 华为/鸿蒙桌面快捷方式与权限适配 (Huawei / HarmonyOS Adaptation)

- [x] 4.1 编写 `HuaweiPermissionHelper`，实现华为 EMUI / HarmonyOS 环境识别与桌面快捷方式权限状态检测探针
- [x] 4.2 实现针对华为手机管家（`com.huawei.systemmanager`）及系统特殊应用权限设置页的一键跳转 Intent 辅助方法
- [x] 4.3 封装 `ShortcutManagerCompat.requestPinShortcut` 桌面快捷方式生成逻辑与图标绘制合成逻辑

## 5. Material 3 配置主界面 (MainActivity & UI Interactions)

- [x] 5.1 搭建基于 Material 3 风格的 `MainActivity` 界面，包含 Slot 1 和 Slot 2 槽位信息卡片
- [x] 5.2 实现 0~2000ms 范围的延迟调节 Slider，提供实时毫秒文本展示与配置自动保存
- [x] 5.3 实现槽位配置选择弹窗与清除重置交互逻辑
- [x] 5.4 实现“即时测试双启”功能按钮，校验槽位完整性并在当前界面直接执行顺序调起
- [x] 5.5 实现“添加到桌面”按钮动作，集成华为权限预检与未授权拦截引导弹窗

## 6. V1+V2+V3 全版本签名与 GitHub Actions CI/CD (Signing & CI/CD Pipeline)

- [x] 6.1 在 Gradle 脚本中配置 release 签名块，显式开启 `v1SigningEnabled true`、`v2SigningEnabled true`、`v3SigningEnabled true`
- [x] 6.2 编写 `.github/workflows/build-apk.yml` 自动化构建工作流，配置 JDK 17 及 Android SDK 构建环境
- [x] 6.3 在 CI 流水线中实现基于 GitHub Secrets（`KEYSTORE_BASE64`、`KEY_ALIAS`、`KEY_PASSWORD`、`STORE_PASSWORD`）的安全注入与签名
- [x] 6.4 配置 GitHub Release 自动发布与构建产物 APK 的 Artifact 上传保存逻辑
