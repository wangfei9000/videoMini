package com.wangfei.videoshrink

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wangfei.videoshrink.media.CodecCapabilityChecker
import com.wangfei.videoshrink.media.MediaStoreSaver
import com.wangfei.videoshrink.media.VideoCompressionService
import com.wangfei.videoshrink.media.VideoMetadata
import com.wangfei.videoshrink.media.VideoMetadataReader
import com.wangfei.videoshrink.model.CompressionPreset
import com.wangfei.videoshrink.model.VideoCodecOption
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 页面展示和操作所需的全部状态。 */
data class VideoShrinkUiState(
    val selectedUri: Uri? = null,
    val metadata: VideoMetadata? = null,
    val preset: CompressionPreset = CompressionPreset.P480,
    val codec: VideoCodecOption = VideoCodecOption.H264,
    val hevcSupported: Boolean = false,
    val isReading: Boolean = false,
    val isCompressing: Boolean = false,
    val progress: Int = 0,
    val outputFile: File? = null,
    val statusText: String? = null,
)

/** 连接 Compose 界面、相册 URI 与压缩服务。 */
class VideoShrinkViewModel(application: Application) : AndroidViewModel(application) {
    private val compressionService = VideoCompressionService(application)
    private val mutableState = MutableStateFlow(VideoShrinkUiState())
    private var compressionJob: Job? = null

    /** 界面只读状态流。 */
    val state: StateFlow<VideoShrinkUiState> = mutableState.asStateFlow()

    /** 读取用户从系统照片选择器中选中的视频信息。 */
    fun selectVideo(uri: Uri) {
        cancelCompression()
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    selectedUri = uri,
                    metadata = null,
                    isReading = true,
                    outputFile = null,
                    progress = 0,
                    statusText = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    VideoMetadataReader.read(getApplication(), uri)
                }
            }.onSuccess { metadata ->
                mutableState.update { old ->
                    old.copy(
                        metadata = metadata,
                        isReading = false,
                        hevcSupported = supportsHevc(old.preset, metadata),
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(isReading = false, statusText = "读取视频失败：${error.userMessage()}")
                }
            }
        }
    }

    /** 修改目标分辨率，并重新检查 HEVC 编码能力。 */
    fun selectPreset(preset: CompressionPreset) {
        mutableState.update { old ->
            val supported = old.metadata?.let { supportsHevc(preset, it) } ?: false
            old.copy(
                preset = preset,
                hevcSupported = supported,
                codec = if (!supported && old.codec == VideoCodecOption.HEVC) {
                    VideoCodecOption.H264
                } else {
                    old.codec
                },
            )
        }
    }

    /** 修改视频编码格式；不支持 HEVC 的设备会保留 H.264。 */
    fun selectCodec(codec: VideoCodecOption) {
        mutableState.update { old ->
            if (codec == VideoCodecOption.HEVC && !old.hevcSupported) {
                old.copy(statusText = "这台设备不支持当前分辨率的 HEVC 编码")
            } else {
                old.copy(codec = codec, statusText = null)
            }
        }
    }

    /** 启动压缩，同一时间只允许一个导出任务。 */
    fun startCompression() {
        val snapshot = mutableState.value
        val uri = snapshot.selectedUri ?: return
        val metadata = snapshot.metadata ?: return
        if (snapshot.isCompressing) return

        compressionJob = viewModelScope.launch {
            mutableState.update {
                it.copy(isCompressing = true, progress = 0, outputFile = null, statusText = null)
            }
            try {
                val file = compressionService.compress(
                    inputUri = uri,
                    metadata = metadata,
                    preset = snapshot.preset,
                    codec = snapshot.codec,
                ) { progress -> mutableState.update { it.copy(progress = progress) } }
                mutableState.update {
                    it.copy(
                        isCompressing = false,
                        progress = 100,
                        outputFile = file,
                        statusText = "压缩完成，可保存到相册或分享",
                    )
                }
            } catch (_: CancellationException) {
                mutableState.update {
                    it.copy(isCompressing = false, progress = 0, statusText = "已取消压缩")
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        isCompressing = false,
                        progress = 0,
                        statusText = "压缩失败：${error.userMessage()}",
                    )
                }
            }
        }
    }

    /** 取消当前压缩任务。 */
    fun cancelCompression() {
        compressionService.cancel()
        compressionJob?.cancel()
        compressionJob = null
    }

    /** 把完成的缓存文件复制到 Movies/VideoShrink 相册目录。 */
    fun saveToGallery() {
        val file = mutableState.value.outputFile ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(statusText = "正在保存到相册…") }
            runCatching { MediaStoreSaver.save(getApplication(), file) }
                .onSuccess {
                    mutableState.update { old -> old.copy(statusText = "已保存到相册：DCIM/VideoShrink") }
                }
                .onFailure { error ->
                    mutableState.update { old ->
                        old.copy(statusText = "保存失败：${error.userMessage()}")
                    }
                }
        }
    }

    /** 判断设备编码器能否输出当前档位的 HEVC。 */
    private fun supportsHevc(preset: CompressionPreset, metadata: VideoMetadata): Boolean {
        val width = if (metadata.isPortrait) preset.landscapeHeight else preset.landscapeWidth
        val height = if (metadata.isPortrait) preset.landscapeWidth else preset.landscapeHeight
        return CodecCapabilityChecker.supports(
            VideoCodecOption.HEVC,
            width,
            height,
            preset.preferredBitrate(VideoCodecOption.HEVC),
        )
    }

    /** ViewModel 销毁时停止底层编码器。 */
    override fun onCleared() {
        compressionService.cancel()
        super.onCleared()
    }
}

/** 给底层异常生成适合直接显示的简短文本。 */
private fun Throwable.userMessage(): String = localizedMessage?.takeIf { it.isNotBlank() }
    ?: javaClass.simpleName
