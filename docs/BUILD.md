# 构建说明

本文档说明如何在本地构建考研学习助手 Android 项目。

## 环境要求

- JDK 17
- Android Studio
- Android SDK，包含项目所需的 Android API 35
- Gradle Wrapper，仓库已包含 `gradlew` 和 `gradlew.bat`

## 本地构建

Windows：

```powershell
.\gradlew.bat assembleDebug
```

Linux / macOS：

```bash
./gradlew assembleDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 常见构建问题

### JDK 版本不匹配

项目使用 Java 17。如果本地 JDK 版本过低，Gradle 或 Android Gradle Plugin 可能无法正常运行。请在 Android Studio 或系统环境变量中配置 JDK 17。

### Android SDK 缺失

如果提示找不到 SDK 或 API 版本，请在 Android Studio SDK Manager 中安装项目所需的 Android SDK Platform。

### compileSdk 提示

当前项目使用 `compileSdk = 35`。如果 Android Gradle Plugin 提示兼容性警告，只要构建成功即可继续使用；如需消除警告，可在后续维护中升级 Android Gradle Plugin。

## 构建产物

APK / AAB 不应直接提交到代码仓库。需要分发测试包时，建议通过 GitHub Releases 上传构建产物。
