# Android 项目编译步骤

本文档记录本项目在 Windows 环境下的编译流程。

## 1. 打开项目

使用 Android Studio 打开项目目录：

```text
D:\android-wf\android
```

项目使用 Kotlin DSL 配置 Gradle，主要配置文件包括：

- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## 2. 检查 Android SDK

项目配置要求：

- compileSdk：36
- targetSdk：36
- minSdk：29
- Build Tools：35.0.0

Android SDK 路径由 `local.properties` 指定：

```text
C:\Users\wf\AppData\Local\Android\Sdk
```

如果 SDK 组件缺失，可以使用 Android Studio 的 SDK Manager 安装，或者使用命令行：

```powershell
$env:JAVA_HOME = 'C:\Users\wf\.jdks\corretto-1.8.0_362'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& 'C:\Users\wf\AppData\Local\Android\Sdk\tools\bin\sdkmanager.bat' `
  'platforms;android-36' `
  'build-tools;35.0.0'
```

旧版 `sdkmanager` 需要使用 Java 8，否则可能出现 `javax/xml/bind` 缺失错误。

## 3. 使用 Gradle 编译

本项目没有提交 `gradlew` 包装脚本，因此使用本机已安装的 Gradle 8.14.3：

```powershell
$env:GRADLE_USER_HOME = 'D:\android-wf\android\.gradle-build'
$env:ANDROID_USER_HOME = 'D:\android-wf\android\.android-build'

& 'C:\Users\wf\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat' assembleDebug
```

编译 Release 版本时可以执行：

```powershell
& 'C:\Users\wf\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat' assembleRelease
```

## 4. 本次修复的配置问题

### Kotlin JVM 编译目标

Kotlin 2.3 不再接受旧的 `kotlinOptions.jvmTarget = "17"` 写法，改为：

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

### Compose 版本兼容性

原来的 Compose BOM 版本要求 AGP 9.1 和 compileSdk 37，与当前项目的 AGP 8.13.2、compileSdk 36 不兼容。

因此在 `gradle/libs.versions.toml` 中使用兼容版本：

```toml
composeBom = "2025.10.01"
```

### Media3 Effects 导入

`Effects` 类属于 Transformer 包，正确导入方式为：

```kotlin
import androidx.media3.transformer.Effects
```

## 5. 编译结果

执行 `assembleDebug` 成功后，Debug APK 位于：

```text
D:\android-wf\android\app\build\outputs\apk\debug\app-debug.apk
```

也可以在 Android Studio 中通过：

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

进行编译。

## 6. 常见问题

### Gradle native library 加载失败

如果出现 `Failed to load native library 'native-platform.dll'`，可以将 Gradle 缓存切换到项目目录：

```powershell
$env:GRADLE_USER_HOME = 'D:\android-wf\android\.gradle-build'
```

### SDK 目录不可写

如果出现 `The SDK directory is not writable`，请使用 Android Studio 的 SDK Manager，或者以有权限的方式运行 `sdkmanager` 安装缺失组件。

### Kotlin daemon 权限错误

如果出现 Kotlin daemon 的权限错误，Gradle 通常会自动回退到无 daemon 编译。也可以先关闭 Android Studio 中正在运行的 Gradle/Kotlin 任务，再重新编译。

