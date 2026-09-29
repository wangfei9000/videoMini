package com.wangfei.videoshrink.model

import androidx.media3.common.MimeTypes

/** 用户可以选择的视频编码格式。 */
enum class VideoCodecOption(
    val title: String,
    val description: String,
    val mimeType: String,
) {
    H264(
        title = "兼容模式",
        description = "MP4 + H.264 + AAC-LC，兼容 Mac、iPhone、Windows 和 Android",
        mimeType = MimeTypes.VIDEO_H264,
    ),
    HEVC(
        title = "省空间模式",
        description = "MP4 + HEVC + AAC-LC，文件通常更小，部分旧设备可能无法播放",
        mimeType = MimeTypes.VIDEO_H265,
    ),
}

/** 分辨率与基础码率档位。尺寸表示横屏情况下的最大边界。 */
enum class CompressionPreset(
    val title: String,
    val landscapeWidth: Int,
    val landscapeHeight: Int,
    private val h264Bitrate: Int,
) {
    P480("480p", 640, 480, 700_000),
    P540("540p", 960, 540, 1_000_000),
    P720("720p", 1280, 720, 1_800_000),
    P1080("1080p", 1920, 1080, 4_000_000);

    /** 返回当前编码器的质量码率上限。 */
    fun preferredBitrate(codec: VideoCodecOption): Int = when (codec) {
        VideoCodecOption.H264 -> h264Bitrate
        VideoCodecOption.HEVC -> (h264Bitrate * 0.65).toInt()
    }

    /** 按显示方向返回 Media3 Presentation 应使用的最大输出高度。 */
    fun targetHeight(isPortrait: Boolean): Int = if (isPortrait) landscapeWidth else landscapeHeight
}
