# 考研学习助手

考研学习助手是一款面向考研学生的个人学习管理 Android App。项目尝试解决备考过程中学习记录分散、复盘效率低、学习状态难以量化的问题，通过学习计时、科目管理、历史记录、每日复盘、目标设置和 AI 学习总结，帮助用户形成更稳定的学习反馈闭环。

本项目目前以个人学习场景为主，功能设计偏向本地可用、数据清晰和配置透明。

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
- 当前版本：`1.0`

## AI / Agent 辅助开发流程

本项目在开发过程中使用 AI / Agent 辅助完成了需求拆解、代码结构分析、Jetpack Compose 页面优化、Bug 定位、构建错误排查、智能总结模块设计和 README 文档整理。

典型使用场景包括：

- 根据考研学习管理需求拆解 App 功能模块。
- 辅助设计学习计时、学习记录、每日复盘、学习统计等核心页面。
- 分析计时、状态保存、AI 总结触发逻辑等问题。
- 设计“本地规则总结 + 大模型增强”的智能总结流程。
- 辅助整理构建说明、项目文档和后续路线图。

AI / Agent 主要承担需求分析、代码修改建议、调试思路生成和文档整理工作；人工负责最终代码审核、运行测试、功能取舍和发布前检查。

更多说明见 [docs/AI_AGENT_WORKFLOW.md](docs/AI_AGENT_WORKFLOW.md)。

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

## 本地构建

请先安装：

- JDK 17
- Android SDK，包含项目所需的 Android API 35

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

Release APK / AAB 不建议直接提交到代码仓库。如需发布安装包，建议通过 GitHub Releases 上传构建产物。

## Roadmap

- 完善 AI 对话功能。
- 支持多模型供应商配置。
- 优化 API Key 管理。
- 增强学习数据分析。
- 完善备份与恢复。
- 适配平板端布局。
- 优化番茄钟后台计时稳定性。

## 隐私与安全

- API Key 不应硬编码进项目代码。
- `local.properties`、`.env`、签名文件不应提交到 GitHub。
- 用户学习数据默认存储在本地设备。
- 若使用第三方模型接口，应由用户自行配置服务地址和 Key，并自行确认对应服务的隐私政策和计费规则。
- APK、AAB、签名证书和本地构建缓存不应直接提交到代码仓库。

## 文档

- [AI / Agent 辅助开发流程](docs/AI_AGENT_WORKFLOW.md)
- [提交材料建议](docs/SUBMISSION_MATERIALS.md)
- [发布检查清单](docs/RELEASE_CHECKLIST.md)
- [更新日志](CHANGELOG.md)

## 许可证

当前仓库尚未添加开源许可证。未经明确授权，不建议将本项目用于商业分发。
