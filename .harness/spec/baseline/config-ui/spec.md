# config-ui Specification

## Purpose
TBD - created by archiving change dual-app-shortcut-launcher. Update Purpose after archive.
## Requirements
### Requirement: Material 3 槽位配置主界面
系统 MUST 提供基于 Material 3 设计规范的 `MainActivity` 配置界面，包含清晰展示的 Slot 1（先启）与 Slot 2（后启）卡片。每个卡片显示当前已选择应用的图标、名称、启动类型，并提供更改与清除按钮。

#### Scenario: 浏览与查看当前配置槽位
- **WHEN** 用户打开应用进入 MainActivity
- **THEN** 界面展示 Material 3 风格顶栏、Slot 1 与 Slot 2 卡片，若已配置则展示对应应用的图标和名称，未配置则展示“点击选择应用”占位提示

### Requirement: 启动延迟滑块调节
系统 SHALL 在主界面提供一个可视化延时调节组件（Slider），支持在 0ms 至 2000ms 范围内滑动设置唤起两个应用之间的间隔毫秒数（默认 200ms），并在滑块旁实时显示具体毫秒数值，数据变动实时自动持久化。

#### Scenario: 调整双启延迟时间
- **WHEN** 用户拖动延迟滑块至 350ms
- **THEN** 界面立即更新文本为“350 ms”，并将新的延迟配置保存至本地存储

### Requirement: 即时测试双启执行
系统 MUST 在主界面提供“测试双启”悬浮按钮或动作按钮，点击后立即使用当前配置直接在后台先后唤起 Slot 1 与 Slot 2 应用，以便用户无须退回桌面即可直接验证启动效果。

#### Scenario: 点击测试启动按钮
- **WHEN** 用户已完整配置两个槽位并点击“测试启动”
- **THEN** 系统立即触发双启调度逻辑，先后唤起两个应用，验证延时和启动链路正常

#### Scenario: 槽位配置未完成时点击测试
- **WHEN** 用户未配置 Slot 1 或 Slot 2 时点击“测试启动”
- **THEN** 系统拦截启动行为，弹出 Snackbar 提示用户“请先完成两个启动槽位的配置”

### Requirement: 桌面快捷方式创建触发与状态展示
系统 SHALL 在主界面提供明显的“添加到桌面”按钮，并展示当前设备快捷方式权限状态标签；当用户点击按钮且槽位有效时触发快捷方式创建流程。

#### Scenario: 触发生成桌面快捷方式
- **WHEN** 两个槽位均已配置完成，用户点击“添加到桌面”按钮
- **THEN** 系统读取当前槽位名称与图标合成快捷方式信息，触发桌面固定快捷方式请求流程，并展示成功或授权引导反馈

