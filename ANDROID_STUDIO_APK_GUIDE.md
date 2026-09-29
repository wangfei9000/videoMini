# 使用 Android Studio 生成 APK

本文档说明如何使用 Android Studio 编译 Debug APK、未签名 Release APK，以及正式签名 APK。

## 1. 打开项目并同步 Gradle

使用 Android Studio 打开：

```text
D:\android-wf\android
```

打开项目后，先执行：

```text
File > Sync Project with Gradle Files
```

等待右下角 Gradle 同步完成，并确认没有错误。

如果 Build 菜单中看不到签名 APK 选项，通常需要先完成这一步同步。

## 2. 选择构建类型

打开：

```text
Build > Select Build Variant...
```

将 `app` 模块的构建类型设置为：

```text
release
```

Debug 版本则选择：

```text
debug
```

## 3. 生成 Debug APK

菜单操作：

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

或者执行：

```text
Build > Make Project
```

Debug APK 输出位置：

```text
D:\android-wf\android\app\build\outputs\apk\debug\app-debug.apk
```

Debug APK 使用 Android Studio 自动生成的 Debug 签名，可以直接安装测试，但文件通常比较大。

## 4. 生成未签名 Release APK

可以在 Gradle 面板中执行：

```text
app > Tasks > build > assembleRelease
```

也可以运行项目根目录的脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk-release.ps1
```

未签名 Release APK 输出位置：

```text
D:\android-wf\android\app\build\outputs\apk\release\app-release-unsigned.apk
```

未签名 APK 一般不能作为正式应用发布，也可能无法直接安装到设备上。

## 5. 生成签名 Release APK

Gradle 同步成功后，打开：

```text
Build > Generate Signed Bundle / APK
```

然后按以下步骤操作：

1. 选择 `APK`。
2. 点击 `Next`。
3. 在 `Key store path` 处点击 `Create new...`。
4. 设置 `.jks` 文件路径和密码。
5. 填写 Key alias、Key password、姓名等信息。
6. 点击 `Next`。
7. 构建类型选择 `release`。
8. 点击 `Create` 或 `Finish`。

签名文件建议保存到安全位置，不要提交到 Git，也不要丢失密码。应用后续更新必须使用同一个签名密钥。

## 6. APK 输出目录

签名 Release APK 通常位于：

```text
D:\android-wf\android\app\build\outputs\apk\release\
```

文件名可能是：

```text
app-release.apk
```

## 7. 截图中菜单缺少签名选项时

截图中可以看到项目和 `app` 模块，但 Build 菜单没有 `Generate Signed Bundle / APK`。可以按以下顺序处理：

1. 执行 `File > Sync Project with Gradle Files`。
2. 等待 Gradle 同步结束。
3. 确认项目没有 Gradle 错误。
4. 确认左侧项目中存在 `app` 模块。
5. 再次打开 `Build` 菜单。
6. 如果仍然没有签名选项，使用 Gradle 面板执行 `assembleRelease`，然后在 Android Studio 中使用 APK 签名工具签名。

## 8. 本项目脚本

Debug 构建脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

Release 构建脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk-release.ps1
```

当前 Release 脚本生成未签名 APK。正式发布时，建议使用 Android Studio 的 `Generate Signed Bundle / APK` 完成签名。
