# 更新的逻辑
使用说明：
当您发布新版本时，请在 GitHub 仓库的 Releases 页面发布一个新的 Release：
1. Tag version：例如 v1.1 或 1.1（必须大于当前版本 1.0）。
2. Assets：务必上传编译好的 `.apk` 文件 到该 Release 中。
3. 应用会自动检测最新 Release 中的第一个 .apk 文件进行下载。