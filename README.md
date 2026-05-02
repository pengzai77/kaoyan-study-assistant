# 考研学习助手

考研学习助手是一款面向考研学生的 Android 学习管理应用。它支持学习计时、科目管理、历史记录统计、每日复盘、目标设置，以及基于 OpenAI-compatible 接口的 AI 学习总结。

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- MVVM + Repository
- Room
- DataStore Preferences
- Hilt
- Retrofit + OkHttp
- Kotlin Coroutines + Flow
- Gradle 8.7

## 项目信息

- 包名：`com.kaoyan.studyassistant`
- 最低系统：Android 8.0，API 26
- `compileSdk`：35
- `targetSdk`：35
- Java / Kotlin JVM：17
- 版本：`1.0`

## 主要功能

- 学习计时：按科目开始、暂停、继续和结束学习计时。
- 科目管理：新增、编辑、删除科目，并支持科目颜色。
- 学习记录：查看历史学习记录、统计累计时长和趋势。
- 每日复盘：记录每天的问题、心得和总结。
- 考研目标：设置目标院校、专业、考试日期和学习目标。
- AI 总结：支持配置不同 AI 厂商的 Base URL、模型和 API Key，未配置时可回退到本地规则总结。

## 本地构建

请先安装：

- JDK 17
- Android SDK，包含 API 35

在项目根目录执行：

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出位置：

```text
app\build\outputs\apk\debug\app-debug.apk
```

Release 包可执行：

```powershell
.\gradlew.bat assembleRelease
```

## 敏感文件说明

以下文件不应提交到 GitHub：

- 签名证书：`*.jks`、`*.keystore`、`*.p12`
- API Key 或本地密钥：`.env`、`local.properties`
- 构建产物：`*.apk`、`*.aab`、`app/build/`
- 本地 SDK 和本地构建工具

如果需要发布安装包，建议使用 GitHub Releases 上传 APK，而不是直接提交到代码仓库。
