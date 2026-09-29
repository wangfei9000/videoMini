package com.wangfei.videoshrink.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import com.wangfei.videoshrink.model.VideoCodecOption

/** 检查当前设备是否存在满足目标参数的硬件或软件视频编码器。 */
object CodecCapabilityChecker {
    fun supports(
        codec: VideoCodecOption,
        width: Int,
        height: Int,
        bitrate: Int,
        frameRate: Int = 30,
    ): Boolean {
        val format = MediaFormat.createVideoFormat(codec.mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
        }
        return MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format) != null
    }
}
