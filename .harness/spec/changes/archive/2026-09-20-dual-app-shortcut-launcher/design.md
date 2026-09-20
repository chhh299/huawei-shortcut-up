## Context

在 Android 系统（特别是中国主流的华为 EMUI 及鸿蒙 HarmonyOS 生态）中，用户在特定自动化或多任务协同场景下存在强烈的“一键顺序调起两个应用”需求（例如：先启动取景/悬浮辅助工具，再启动目标应用）。传统手动操作需要在桌面或任务列表多次寻找与点击，操作链路繁长。

然而，实现平滑、稳定、无感的一键双启存在若干核心技术挑战：
1. **启动视觉体验与残影**：普通 Activity 中转容易引发界面白屏/黑屏闪烁，并在系统多任务管理器（Recents）中留下无用的空卡片；
2. **时序与后台启动限制 (BAL)**：Android 高版本对后台启动 Activity 施加了严格限制，两个 Activity 的快速连续唤起需要精确的生命周期管理与毫秒级时序协同；
3. **华为/鸿蒙生态的权限静默拦截**：华为桌面对于 `ShortcutManager` 固定快捷方式的创建往往存在独立的安全管家权限开关，若未授权系统会静默吞没请求而不给桌面添加图标，亦不给调用方报错；
4. **第三方快捷方式的深度提取**：除了普通 App 的启动入口，许多应用的功能依赖于自定义的 Shortcut（通过 `ACTION_CREATE_SHORTCUT` 协议提供），需要安全提取与跨进程持久化；
5. **合规性与自动化流水线**：由于不同 Android 版本及华为安装包管理器对签名机制的要求，必须支持完整的 V1+V2+V3 全版本签名，并集成 GitHub Actions 自动化 CI。

## Goals / Non-Goals

**Goals:**
- **透明极速调度**：设计完全透明、无过渡动画、不进最近任务列表的 `DispatchActivity`，实现默认 100~300ms（用户可自定义 0~2000ms）先后安全唤起两个目标应用。
- **第三方快捷方式与普通应用全覆盖**：支持通过 `ACTION_CREATE_SHORTCUT` 协议唤起第三方应用的快捷方式创建面板并提取 Intent，同时支持枚举系统已安装应用，序列化为 URI 字符串持久化存储。
- **华为/鸿蒙系统深度适配**：封装华为专有桌面快捷方式权限检测探针，解决无感静默失败问题，提供一键跳转华为系统管家/权限设置页的高可用引导。
- **优雅的 Material 3 配置主界面**：基于 Material 3 提供直观的双槽位卡片、实时延迟滑块调节、一键即时测试启动、快捷方式生成及权限看板。
- **工业级全签名与 CI/CD**：提供 V1+V2+V3 签名配置与基于 GitHub Actions 的自动构建发布流（`.github/workflows/build-apk.yml`），关联远程仓库。

**Non-Goals:**
- **不依赖 Root 或系统无障碍服务**：不通过 AccessibilityService 模拟点击，保持纯净、低功耗与高安全性。
- **不常驻后台 Service**：不引入后台守护进程或前台常驻通知栏服务，仅在点击瞬间执行瞬态中转，中转完毕即刻销毁。
- **不干预第三方 App 内部状态**：启动仅通过标准 Android Intent 通信，不进行 Hook、动态注入或侵入式修改。

## Decisions

### D1: 调度中转组件架构选型

**选择**：采用专用透明无界面的 `DispatchActivity`，配置 `excludeFromRecents="true"`、`noHistory="true"` 及透明无动画主题。

**替代方案**：
- A) 前台服务（ForegroundService）中转 — 在 Android 10+ 以后，后台或前台服务直接调用 `startActivity()` 受到严格的后台启动 Activity（BAL）限制，且前台服务必须弹出持续通知栏通知，用户感知明显、体验繁重。
- B) 无障碍服务（AccessibilityService）监听桌面点击模拟启动 — 需要用户授予高危无障碍辅助权限，不仅可能被华为安全管家报毒拦截，且服务容易在后台被系统杀死，维护成本极高。

**理由**：透明 Activity 作为前台可见组件被点击唤起瞬间，天然具备启动其它 Activity 的顶级合法权限，不受 BAL 限制；通过系统主题属性屏蔽窗口背景和动画，调度完毕即刻 `finish()`，对用户完全无感知且不占用任务列表。

---

### D2: 双应用唤起时序与生命周期协同

**选择**：基于协程或主线程 `Handler.postDelayed` 进行毫秒级微延时调度（默认 100~300ms 可调），在 `onCreate` 中直接执行启动链路并在触发槽位 2 之后立即调用 `finish()`。

**替代方案**：
- A) 两个应用同时调用 `startActivity` 无延迟并发唤起 — 两个应用如果完全在同一毫秒发起启动，会导致系统 WindowManagerService 动画混乱、GPU 负载突增，且往往后调起的应用与前一个应用发生窗口竞争抢占焦点，导致悬浮类辅助工具未能完成初始化即被主应用顶掉。
- B) `Thread.sleep()` 阻塞主线程等待 — 会直接阻塞 Android 主线程，引发严重的丢帧甚至系统 ANR 弹窗。

**理由**：主线程非阻塞延迟机制既保证了第一个应用有足够的毫秒级窗口启动窗口并完成窗口初始化（例如悬浮窗展示），又使调度器在第二个应用被拉起前保持在有效生命周期内，最后通过 `finish()` 优雅退出。

---

### D3: Intent 数据的序列化与持久化存储方案

**选择**：采用标准 `Intent.toUri(Intent.URI_INTENT_SCHEME)` 将 Intent 序列化为规范 URI 字符串，结合 SharedPreferences / Preferences DataStore 进行键值对持久化。

**替代方案**：
- A) 使用 Room / SQLite 数据库存储 — 存储需求仅仅是 2 个槽位的配置信息（包名、组件名、Action、Extra 及延迟配置），引入 ORM 数据库增加无谓的代码体积与冷启动开销。
- B) 自定义 JSON 解析存储 Intent 的各个字段 — Intent 内部可能包含复杂的 Bundle、Extras、Flags、Categories、Data URI 以及 ComponentName，自定义 JSON 解析极易遗漏特定系统 Flags 或造成嵌套 Extra 丢失。

**理由**：`Intent.toUri()` 与 `Intent.parseUri()` 是 Android 官方提供的工业级标准序列化协议，能够完整、保真地保留 Intent 的所有深层参数，且字符串形式非常轻量，易于持久化和安全校验。

---

### D4: 华为 EMUI / HarmonyOS 桌面快捷方式与防静默拦截方案

**选择**：统一采用 `ShortcutManagerCompat.requestPinShortcut` 发起固定快捷方式请求，结合华为专用权限探针（检测 AppOps 及华为手机管家权限标记）预检权限状态，并在缺失权限时提供精准跳转 Intent。

**替代方案**：
- A) 仅调用原生 `ShortcutManager.requestPinShortcut` 并不做任何华为定制适配 — 在华为/鸿蒙设备上，系统默认关闭了应用的“创建桌面快捷方式”权限，原生 API 调用后系统会返回成功但实际静默拦截丢弃，桌面不生成任何图标，导致用户认为 App 功能损坏。
- B) 继续使用已废弃的广播协议 `com.android.launcher.action.INSTALL_SHORTCUT` — 在 Android 8.0 (API 26) 及以上版本已被系统废弃，在现代鸿蒙系统和 Android 10+ 几乎 100% 被系统桌面忽略。

**理由**：`ShortcutManagerCompat` 向上兼容 Android 8.0~14+ 现代快捷方式标准；结合针对华为设备的权限预检，能将“静默失败”转变为“友好的前置授权引导”，极大提升在华为手机上的成功率与用户口碑。

---

### D5: V1+V2+V3 全版本签名与 GitHub Actions CI/CD 流水线

**选择**：在 Gradle 构建配置中显式开启 `v1SigningEnabled true`、`v2SigningEnabled true`、`v3SigningEnabled true`，并在 GitHub Actions 中通过 Base64 编码注入 Keystore 自动打包对齐签名。

**替代方案**：
- A) 仅启用 V2/V3 签名，不启用 V1 (Jar) 签名 — 部分较早版本的系统或第三方 ROM/分发渠道在安装纯 V2/V3 签名的 APK 时会报 `INSTALL_PARSE_FAILED_NO_CERTIFICATES` 证书解析错误。
- B) 本地开发者手动使用 `apksigner` 工具命令行签名后上传 — 人工操作繁琐、容易引入人为疏漏，无法实现每次 Git Tag 或 Pull Request 的自动化质量追踪与稳定产物交付。

**理由**：V1+V2+V3 全版本签名确保了在从低版本 Android 到最新 HarmonyOS 全生态下的最大兼容性与防篡改安全性；通过 GitHub Actions 实现提交即打包、打标签即 Release，完全自动化免去维护烦恼。

## Risks / Trade-offs

- **[Risk 1: 华为 HarmonyOS/EMUI 后台权限与快捷方式静默拦截]**
  - *Mitigation*: 建立 `HuaweiPermissionHelper` 工具类，通过系统属性 `ro.build.version.emui` 判定华为环境，并通过反射检测 AppOpsManager 中的 `OP_ADD_VOICEMAIL` / `OP_POST_NOTIFICATION` / 快捷方式对应 flag。若未授权，弹出清晰的对话框并提供直跳华为手机管家（`com.huawei.systemmanager`）权限配置页的 Intent。
- **[Risk 2: 目标快捷方式 Intent 包含单次临时授权标志导致下次唤起失效]**
  - *Mitigation*: 在从 `ACTION_CREATE_SHORTCUT` 提取 Intent 并序列化之前，显式移除 `FLAG_GRANT_READ_URI_PERMISSION` 等临时授权 Flag，并确保存储的是持久化的 Component 或具有明确 Action/Data 的 Intent。
- **[Risk 3: Android 11+ 包可见性限制查询不到第三方 App]**
  - *Mitigation*: 在 `AndroidManifest.xml` 中配置 `<queries>` 标签声明 `android.intent.action.CREATE_SHORTCUT` 及常规 Launcher 意图，或按工具类需求申请 `QUERY_ALL_PACKAGES` 权限，确保能枚举系统应用列表。
- **[Risk 4: 快速顺序启动引起的窗口视觉冲突]**
  - *Mitigation*: `DispatchActivity` 采用 `@android:style/Theme.Translucent.NoTitleBar` 全透明样式并禁用窗口动画；`startActivity` 调用均带上 `FLAG_ACTIVITY_NEW_TASK`，槽位 2 唤起前设置可调的缓冲延迟（默认 200ms）。

## Migration Plan

1. **第一阶段：核心引擎与中转验证**
   - 建立 Android 项目工程骨架（Gradle / Kotlin），配置透明 `DispatchActivity` 及 Intent 序列化引擎；
   - 验证单 App 及双 App 的先后唤起延迟逻辑与异常容错。
2. **第二阶段：快捷方式提取与华为适配**
   - 实现 `ACTION_CREATE_SHORTCUT` 交互结果接收器与应用列表选择器；
   - 实现 `ShortcutManagerCompat` 桌面快捷方式生成逻辑与华为权限探针/跳转辅助类。
3. **第三阶段：UI 界面整合**
   - 搭建 Material 3 `MainActivity`，接入数据持久化，完成槽位配置、延时调节与即时测试按钮交互。
4. **第四阶段：CI/CD 与全版本签名**
   - 配置 Gradle `signingConfigs` 支持 V1/V2/V3；
   - 编写 `.github/workflows/build-apk.yml`，关联远程仓库并测试自动化编译与打包。

## Open Questions

- **Q1: 华为折叠屏及平板设备的悬浮窗应用联动体验**
  - *说明*：华为 Mate X 等折叠屏设备上，如果槽位 1 应用是支持平行视界或悬浮窗的应用，延迟时间设置在 100~300ms 是否足够其完成悬浮窗初始化？
  - *建议*：提供 0~2000ms 的灵活滑动调节条，并提供默认 200ms 推荐值，允许用户针对特定机型微调。
