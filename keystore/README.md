# Keystore 签名配置说明

本项目在 release 构建中支持 Android V1 + V2 + V3 全版本签名方案。

## 1. 本地生成发布 Keystore

可以使用 JDK 附带的 `keytool` 命令生成：

```bash
keytool -genkey -v \
  -keystore keystore/release.keystore \
  -alias key0 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass your_store_password \
  -keypass your_key_password \
  -dname "CN=DualAppLauncher, OU=App, O=DualApp, L=SZ, ST=GD, C=CN"
```

## 2. GitHub Actions CI 密钥配置

在 GitHub 仓库的 **Settings -> Secrets and variables -> Actions** 中配置以下 Secrets：

- `KEYSTORE_BASE64`：执行 `base64 -w 0 keystore/release.keystore`（或 Windows PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore/release.keystore"))`）得到的 Base64 字符串。
- `KEY_ALIAS`：密钥别名（例如 `key0`）。
- `STORE_PASSWORD`：Keystore 密码。
- `KEY_PASSWORD`：Key 别名密码。

若未配置上述 Secrets，CI 及本地开发构建将自动安全降级为内置 debug 密钥，确保构建不会意外中断。
