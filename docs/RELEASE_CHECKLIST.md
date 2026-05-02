# 发布检查清单

发布前建议逐项检查：

- [ ] 是否能本地构建。
- [ ] 是否已检查敏感文件。
- [ ] 是否已更新 README。
- [ ] 是否准备 Release Notes。
- [ ] 是否确认 APK 不直接提交到仓库。
- [ ] 是否通过 GitHub Releases 上传 APK。
- [ ] 是否确认安装包中不包含 API Key。
- [ ] 是否确认版本号和 tag。
- [ ] 是否确认签名文件没有提交到仓库。

## 推荐发布流程

1. 更新版本号、README 和 `CHANGELOG.md`。
2. 运行 Debug 或 Release 构建。
3. 检查 `git status --short --ignored`，确认构建产物和敏感文件处于 ignored 状态。
4. 提交代码和文档变更。
5. 创建 Git tag，例如 `v0.1.0-alpha`。
6. 在 GitHub 创建 Release，并将 APK 作为 Release 附件上传。
