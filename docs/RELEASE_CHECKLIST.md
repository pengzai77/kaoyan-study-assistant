# 发布检查清单

发布前建议逐项检查：

- [ ] 是否能本地构建 Debug 包。
- [ ] 是否已检查敏感文件，包括 API Key、`.env`、`local.properties`、签名证书和付款码。
- [ ] 是否已更新 README。
- [ ] 是否准备真实运行截图。
- [ ] 是否准备 release notes。
- [ ] 是否确认 APK / AAB 不直接提交到代码仓库。
- [ ] 是否通过 GitHub Releases 上传 APK。
- [ ] 是否确认 Release 附件不包含签名证书或调试用密钥。

## 推荐发布流程

1. 本地运行 `.\gradlew.bat assembleDebug` 或 `.\gradlew.bat assembleRelease`。
2. 检查 `git status --short --ignored`，确认构建产物和敏感文件处于 ignored 状态。
3. 更新 `CHANGELOG.md` 和 README 中需要同步的内容。
4. 提交代码和文档变更。
5. 在 GitHub 创建 Release，并将 APK 作为 Release 附件上传。
