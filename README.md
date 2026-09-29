# VideoShrink Android

这是 iOS `VideoShrink` 的 Android 实现。应用直接在 Android 手机上选择相册视频、缩放分辨率并重新编码，不上传服务器，也不依赖 FFmpeg。

默认输出 **MP4 + H.264 + AAC-LC**，兼容 macOS、iOS、Windows 和 Android。用户也可以主动选择 **MP4 + HEVC + AAC-LC** 省空间模式；应用会先检查手机是否存在对应的 HEVC 编码器。

## 已实现功能

- 使用 Android 系统照片选择器选择视频
- 选择 480p、540p、720p 或 1080p
- H.264 兼容模式（默认）
- HEVC 省空间模式（设备支持时可选）
- 显示实时压缩进度并支持取消
- 显示压缩前后文件大小与变化比例
- 保存到系统相册的 `Movies/VideoShrink` 目录
- 通过系统分享面板分享 MP4
- Android 10 及以上无需申请相册或存储权限

## 打包apk
```text
powershell -ExecutionPolicy Bypass -File .\build-apk-release.ps1
```

## 项目结构

```text
android/
├── app/
│   ├── build.gradle.kts                     # App 模块构建参数和依赖
│   └── src/main/
│       ├── AndroidManifest.xml               # Activity 与 FileProvider 声明
│       ├── java/com/wangfei/videoshrink/
│       │   ├── MainActivity.kt               # Android/Compose 入口
│       │   ├── VideoShrinkViewModel.kt       # 页面状态、业务流程和错误处理
│       │   ├── media/
│       │   │   ├── CodecCapabilityChecker.kt # 检查 H.264/HEVC 编码能力
│       │   │   ├── MediaStoreSaver.kt        # 保存 MP4 到系统相册
│       │   │   ├── VideoCompressionService.kt# Media3 压缩、码率和进度
│       │   │   └── VideoMetadataReader.kt    # 读取名称、大小、时长和分辨率
│       │   ├── model/
│       │   │   └── CompressionOptions.kt     # 编码与分辨率档位
│       │   └── ui/
│       │       ├── VideoShrinkScreen.kt      # 主界面、选择器、保存和分享
│       │       └── theme/Theme.kt             # Material 3 深浅色主题
│       └── res/                               # 文本、主题和共享路径配置
├── gradle/libs.versions.toml                 # 统一依赖版本
├── build.gradle.kts                          # 根项目插件配置
└── settings.gradle.kts                       # 模块与仓库配置
```

## 关键流程

1. `VideoShrinkScreen` 调用系统 `PickVisualMedia`，只允许选择视频。
2. `VideoShrinkViewModel.selectVideo()` 调用 `VideoMetadataReader` 读取视频信息。
3. `CodecCapabilityChecker` 检查当前设备能否按所选分辨率编码 HEVC。
4. `VideoCompressionService.compress()` 用 Media3 Transformer 和系统 `MediaCodec` 进行缩放、H.264/HEVC 编码与 AAC 音频编码。
5. 输出先写入应用缓存，用户确认后由 `MediaStoreSaver` 保存到相册，或通过 `FileProvider` 分享。

`VideoCompressionService.calculateVideoBitrate()` 同时考虑预设质量上限和原始文件大小。它把目标总大小控制在原文件约 85% 内，并预留 96 kbps 音频码率，以降低小视频重新编码后反而变大的概率。编码器和封装开销存在差异，因此不能保证每个文件都严格小于原文件。

## 构建环境

- Android Studio（建议使用当前稳定版）
- JDK 17（Android Studio 自带 JDK 即可）
- Android SDK 36
- 最低系统 Android 10 / API 29
- 真机建议 Android 10 或以上，并留出至少“源文件大小的 2 倍”可用空间

## 在 Android Studio 中运行

1. 打开 Android Studio，选择 **Open**。
2. 选择本目录：`ffmpeg-wf/android`，不要选择上一级 iOS 项目目录。
3. 等待 Gradle Sync 和 SDK 依赖下载完成。如果提示缺少 SDK 36，点击提示中的 **Install**。
4. 在手机打开“开发者选项”和“USB 调试”，通过 USB 连接 Mac，并在手机上允许调试。
5. Android Studio 顶部选择手机，选择 `app`，点击 **Run ▶**。

首次运行无需发布签名。Android Studio 会自动创建并使用 debug 签名，直接安装到真机。

## 命令行构建

在 `android` 目录执行：

```bash
./gradlew assembleDebug
```

生成的安装包位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

连接手机后可执行：

```bash
./gradlew installDebug
```

## 模拟器测试视频

模拟器通常没有视频。可以把 Mac 上的 MP4 直接拖进正在运行的 Android 模拟器窗口；系统导入完成后，从应用的“从相册选择视频”进入即可看到。压缩性能和 HEVC 支持应以真机结果为准，因为模拟器的编解码器能力与手机不同。

## 注意事项

- 压缩过程中不要强制结束应用；当前版本没有后台任务恢复功能。
- HEVC 的体积通常更小，但 Windows 老版本、旧 Android 设备或部分聊天软件可能不支持。跨平台发送优先选择默认 H.264。
- 目标档位是最大输出高度。源视频本身更小时不会放大，避免无意义增大文件。
- HDR、杜比视界、可变帧率、多音轨和特殊色彩空间可能被转换为设备编码器支持的普通 SDR 输出，且不同厂商结果可能不同。
- 压缩会产生临时缓存文件。Android 会在空间不足时清理缓存；保存到相册后才是长期文件。
- 发布到应用商店时，应创建 release keystore，并在本机安全配置签名；不要把密钥或密码提交到代码仓库。

## 修改参数

- 分辨率和基础码率：`CompressionOptions.kt` 中的 `CompressionPreset`
- HEVC 相对 H.264 的码率比例：`preferredBitrate()` 中的 `0.65`
- 目标大小比例与音频预留：`VideoCompressionService.calculateVideoBitrate()` 中的 `0.85` 和 `96_000`
- 相册保存目录：`MediaStoreSaver.kt` 中的 `Movies/VideoShrink`

如果以后需要剪切、滤镜、水印、字幕、多段合并或更精确的 FFmpeg 参数，再考虑引入裁剪后的 FFmpeg Android 原生库。当前单一“缩放压缩”需求使用系统硬件编码器更轻量，也更容易通过应用商店审核。
