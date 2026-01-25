# 更新的逻辑
使用说明：
当您发布新版本时，请在 GitHub 仓库的 Releases 页面发布一个新的 Release：
1. Tag version：例如 v1.1 或 1.1（必须大于当前版本 1.0）。
2. Assets：务必上传编译好的 `.apk` 文件 到该 Release 中。
3. 应用会自动检测最新 Release 中的第一个 .apk 文件进行下载。
# 字段意义
1. `versionName`: 这是一个字符串（String），你可以随意设置。
    * 它可以是 "1.0"，也可以是 "1.0.1"，甚至是 "2.0.0-alpha"。
    * 在你的 SettingsScreen.kt 中，显示的代码是 Text("当前版本: ${BuildConfig.VERSION_NAME}")。
    * 所以，你完全可以将 `versionName` 设置为 `"1.0.1"`，它会在设置页面正确显示出来。

2. `versionCode`: 这是一个整数（Integer）。
    * 它不能包含小数点。
    * 它的作用是给系统（和应用商店）看的，用来判断哪个版本更高。
    * 每次发布新版，你只需要把这个数字加 1 即可（比如从 1 变成 2）。


# 开发记录
- 更新文件报错
- App not installed as package conflictswith an existing package.