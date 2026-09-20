# transparent-dispatch Specification

## Purpose
TBD - created by archiving change dual-app-shortcut-launcher. Update Purpose after archive.
## Requirements
### Requirement: 透明无痕中转调度
系统 MUST 提供一个专用的透明调度组件 `DispatchActivity`，在被唤起时不得显示任何可见窗口背景或过渡动画，且不得在系统最近任务（Overview / Recents）列表中留下记录卡片。

#### Scenario: 桌面快捷方式唤起调度中转
- **WHEN** 用户点击桌面生成的启动快捷方式
- **THEN** 系统启动 DispatchActivity，无黑白屏闪烁或明显过渡动画，且系统多任务卡片列表中不增加该 App 的任务记录（`android:excludeFromRecents="true"` 且 `android:noHistory="true"`）

### Requirement: 双应用精确顺序延迟唤起
`DispatchActivity` SHALL 依次解析配置的槽位 1（Slot 1）和槽位 2（Slot 2）目标 Intent，先立刻唤起槽位 1 应用，并在经过指定的延时时间（默认 200ms，区间 0~2000ms）后唤起槽位 2 应用，调度完成后必须立即调用 `finish()` 销毁自身。

#### Scenario: 顺序唤起两个有效目标应用
- **WHEN** DispatchActivity 接收到包含有效配置的启动指令，延时设为 200ms
- **THEN** 系统首先执行 `startActivity` 启动槽位 1 应用，在 200ms 后执行 `startActivity` 启动槽位 2 应用，并立即销毁 DispatchActivity

#### Scenario: 目标应用异常或未安装容错
- **WHEN** 槽位 1 或槽位 2 所指向的目标应用已被卸载或其组件不存在触发 `ActivityNotFoundException`
- **THEN** DispatchActivity 捕获异常，记录错误日志并通过系统 Toast 提示用户应用未安装，安全退出且不发生崩溃崩溃（Crash）

