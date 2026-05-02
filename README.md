# 考研学习助手

考研学习助手是一款面向考研学生的 Android 学习管理 App。项目用于解决备考过程中学习记录分散、复盘效率低、学习状态难以量化的问题，通过学习计时、科目管理、历史记录、每日复盘、目标设置和 AI 学习总结，帮助用户更清晰地跟踪自己的学习状态。

项目目前以个人学习场景为主，功能设计偏向本地可用、数据清晰和配置透明。

## 核心功能

- 学习计时：按科目开始、暂停、继续和结束学习计时。
- 科目管理：新增、编辑、删除学习科目，并支持科目颜色标识。
- 学习记录：保存每次学习时长、科目和备注，便于后续回顾。
- 学习统计：查看累计学习时长、近期学习趋势和目标完成情况。
- 每日复盘：记录当天的问题、心得、总结和改进方向。
- 考研目标设置：配置目标院校、专业、考试日期和阶段性学习目标。
- AI 学习总结：基于学习记录和复盘内容生成结构化总结。
- 本地规则总结兜底：未配置 API Key 或接口不可用时，仍可使用本地规则生成基础总结。
- OpenAI-compatible 接口配置：支持配置兼容 OpenAI Chat Completions 风格的模型服务地址、模型名和 API Key。
- 数据本地存储：学习记录、复盘内容和应用设置默认保存在本地设备。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Repository
- Room
- DataStore Preferences
- Hilt
- Retrofit + OkHttp
- Kotlin Coroutines + Flow
- Gradle

## 项目结构

```text
.
├── app/                              # Android 应用模块
│   ├── src/main/java/com/kaoyan/studyassistant/
│   │   ├── data/                     # 本地数据、远程接口、Repository
│   │   ├── di/                       # Hilt 依赖注入模块
│   │   ├── domain/                   # 学习总结等领域逻辑
│   │   ├── service/                  # 学习计时前台服务
│   │   ├── ui/                       # Compose 页面、导航、主题
│   │   └── util/                     # 通用工具
│   └── src/main/res/                 # 资源文件
├── docs/                             # 构建、发布、安全说明
├── screenshots/                      # 真实运行截图目录
├── gradle/                           # Gradle Wrapper 配置
├── build.gradle.kts                  # 根项目 Gradle 配置
├── settings.gradle.kts               # Gradle 模块配置
└── libs.versions.toml                # 依赖版本目录
```

## 本地构建

请先安装：

- JDK 17
- Android Studio
- Android SDK，包含项目所需的 Android API 35

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

不建议将 APK 直接提交到仓库。如需分发测试安装包，建议通过 GitHub Releases 发布。

更多构建说明见 [docs/BUILD.md](docs/BUILD.md)。

## 配置说明

- API Key 不应硬编码到项目代码中。
- `local.properties` 只用于本地环境配置，不应提交到仓库。
- 第三方模型服务地址、模型名和 API Key 需要由用户自行配置。
- 项目不提供默认第三方服务 Key。
- 使用第三方模型服务前，请自行确认对应服务的隐私政策和计费规则。

## 隐私与安全

- 用户学习数据默认存储在本地设备。
- API Key、签名文件、`local.properties`、`.env` 不应提交到仓库。
- 发布 APK 前应检查安装包和仓库中是否包含敏感信息。
- APK、AAB、签名证书和本地构建缓存不应直接提交到代码仓库。

更多说明见 [docs/PRIVACY_AND_SECURITY.md](docs/PRIVACY_AND_SECURITY.md)。

## 项目截图

仓库目前未提交真实运行截图。后续可在 `screenshots/` 目录补充以下页面截图：

- 首页
- 学习计时
- 科目管理
- 学习统计
- 每日复盘
- AI 总结
- 设置页面

请使用真实设备或模拟器截图，不要上传包含 API Key、手机号、邮箱等敏感信息的图片。

## Roadmap

- 完善 AI 对话功能。
- 支持多模型供应商配置。
- 优化 API Key 管理。
- 增强学习数据分析。
- 完善备份与恢复。
- 适配平板端布局。
- 优化番茄钟后台计时稳定性。

## 文档

- [构建说明](docs/BUILD.md)
- [发布检查清单](docs/RELEASE_CHECKLIST.md)
- [隐私与安全](docs/PRIVACY_AND_SECURITY.md)
- [更新日志](CHANGELOG.md)

## 许可证

当前仓库尚未添加开源许可证。未经明确授权，不建议将本项目用于商业分发。
