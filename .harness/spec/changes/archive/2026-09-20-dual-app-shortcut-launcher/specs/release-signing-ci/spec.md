## ADDED Requirements

### Requirement: Android V1+V2+V3 全版本签名配置
系统构建脚本（Gradle build.gradle.kts / build.gradle）MUST 配置 Release 签名配置块，显式启用 `v1SigningEnabled true`、`v2SigningEnabled true` 和 `v3SigningEnabled true`，以确保打包出的 APK 文件能够在各代 Android 系统（特别是华为 EMUI / HarmonyOS 设备）上正常通过包验证并完成安全安装。

#### Scenario: 执行 release 构建打出三版本全签名 APK
- **WHEN** 执行 `./gradlew assembleRelease` 构建任务
- **THEN** Gradle 产物通过 `apksigner verify --verbose` 校验时，V1、V2、V3 签名验证结果均显示为 true

### Requirement: GitHub Actions 自动化构建与发布工作流
项目代码库根目录 MUST 包含 `.github/workflows/build-apk.yml` 自动化工作流文件。该工作流需支持在向 `main` 分支推送代码、提交 PR 以及推送版本标签（如 `v*`）时自动触发，完成 JDK 环境搭建、Android SDK 依赖同步、Keystore 还原、APK 编译、对齐签名与发布 Release 产物。

#### Scenario: 推送代码触发自动编译打包
- **WHEN** 开发者向关联的远程仓库分支推送提交或创建 Release Tag
- **THEN** GitHub Actions runner 启动并执行 build-apk.yml，自动安装 JDK 17，运行 Gradle 编译产出 Release APK，并将打包好的 APK 上传为 Workflow Artifact 或发布到 GitHub Releases

### Requirement: 安全密钥注入与本地调试降级
CI 流水线 MUST 通过 GitHub Secrets（`KEYSTORE_BASE64`、`KEY_ALIAS`、`KEY_PASSWORD`、`STORE_PASSWORD`）安全注入签名密钥并在构建结束时清理临时文件；当未配置 Secret 或在本地开发者机器构建时，系统 SHALL 自动降级使用默认 debug.keystore 进行签名，确保本地无密钥时依然能够顺利通过单元测试与 Debug 编译。

#### Scenario: 本地未配置签名秘钥时执行编译
- **WHEN** 本地开发者在没有生产 Keystore 的环境下运行 `./gradlew assembleDebug`
- **THEN** 构建系统自动采用内置 debug 签名正常编译成功，不发生构建中断
