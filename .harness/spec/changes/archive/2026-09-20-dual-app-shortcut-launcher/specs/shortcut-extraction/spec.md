## ADDED Requirements

### Requirement: 第三方应用快捷方式协议识别与提取
系统 MUST 支持标准 Android `Intent.ACTION_CREATE_SHORTCUT` 协议，能够调起支持该协议的第三方应用（例如特定相机工具、扫一扫等）提供的快捷方式配置界面，并从返回结果中提取快捷方式的目标 Intent、显示名称以及图标资源。

#### Scenario: 成功提取第三方快捷方式
- **WHEN** 用户在应用槽位中选择“添加第三方快捷方式”并完成目标应用内特定动作的选取
- **THEN** 系统在 `onActivityResult` 中接收返回的 `Intent.EXTRA_SHORTCUT_INTENT`，提取名称和图标，并暂存为槽位配置

### Requirement: 已安装普通应用的启动 Intent 提取
系统 MUST 能够通过 `PackageManager` 查询系统内所有具有 `android.intent.category.LAUNCHER` 属性的已安装应用程序列表，并生成启动该应用的直接 Intent。

#### Scenario: 从应用列表选择常规应用
- **WHEN** 用户在槽位配置中打开已安装应用选择对话框并点击某一应用程序
- **THEN** 系统提取该应用主包名与启动 Activity，构造对应的启动 Intent，并提取应用图标和显示名称填入槽位

### Requirement: Intent 对象的 URI 序列化与持久化存储
系统 SHALL 将选定的目标 Intent 使用 `intent.toUri(Intent.URI_INTENT_SCHEME)` 序列化为规范的 URI 字符串，并持久化保存至 SharedPreferences 或 Preferences DataStore 中；在调度执行时能够通过 `Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)` 安全完整地还原原始 Intent 及 Extra 参数。

#### Scenario: Intent 序列化持久化与还原
- **WHEN** 用户保存包含复杂 Extra 数据或 ComponentName 的快捷方式配置
- **THEN** 系统将 Intent 转换为 URI 字符串写入本地存储；在 DispatchActivity 读取该字符串时完整还原为等效的 Intent 对象
