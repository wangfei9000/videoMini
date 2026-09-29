package com.wangfei.videoshrink.media

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.Effects
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.wangfei.videoshrink.model.CompressionPreset
import com.wangfei.videoshrink.model.VideoCodecOption
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/** 使用 Media3 Transformer 调用手机的视频编码器完成缩放和重新编码。 */
@UnstableApi
class VideoCompressionService(private val context: Context) {
    private var activeTransformer: Transformer? = null
    private var activeContinuation: CancellableContinuation<File>? = null
    private var activeToken: String? = null

    /**
     * 将相册 URI 压缩到应用缓存目录，并持续报告 0..100 的进度。
     * 必须从主线程调用，因为 Transformer 的回调绑定主线程 Looper。
     */
    @MainThread
    suspend fun compress(
        inputUri: Uri,
        metadata: VideoMetadata,
        preset: CompressionPreset,
        codec: VideoCodecOption,
        onProgress: (Int) -> Unit,
    ): File = suspendCancellableCoroutine { continuation ->
        cancel()

        val token = UUID.randomUUID().toString()
        val outputDirectory = File(context.cacheDir, "compressed").apply { mkdirs() }
        val outputFile = File(outputDirectory, "VideoShrink-$token.mp4")
        val targetHeight = min(metadata.displayHeight, preset.targetHeight(metadata.isPortrait))
            .coerceAtLeast(2)
            .let { if (it % 2 == 0) it else it - 1 }
        val videoEffects: List<Effect> = if (targetHeight < metadata.displayHeight) {
            listOf(Presentation.createForHeight(targetHeight))
        } else {
            emptyList()
        }
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(calculateVideoBitrate(metadata, preset, codec))
                    .build(),
            )
            .build()
        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setVideoMimeType(codec.mimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                /** 成功时返回缓存文件。 */
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    clearIfCurrent(token)
                    if (continuation.isActive) continuation.resume(outputFile)
                }

                /** 失败时删除不完整文件并把异常交给界面。 */
                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    outputFile.delete()
                    clearIfCurrent(token)
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            })
            .build()

        activeTransformer = transformer
        activeContinuation = continuation
        activeToken = token
        continuation.invokeOnCancellation {
            transformer.cancel()
            outputFile.delete()
            clearIfCurrent(token)
        }

        transformer.start(editedMediaItem, outputFile.absolutePath)
        CoroutineScope(continuation.context).launchProgress(transformer, continuation, onProgress)
    }

    /** 取消当前导出并唤醒等待中的协程。 */
    @MainThread
    fun cancel() {
        activeTransformer?.cancel()
        activeTransformer = null
        activeContinuation?.cancel()
        activeContinuation = null
        activeToken = null
    }

    /** 小视频使用“原文件大小”反推码率，避免降分辨率后体积反而增大。 */
    private fun calculateVideoBitrate(
        metadata: VideoMetadata,
        preset: CompressionPreset,
        codec: VideoCodecOption,
    ): Int {
        val qualityLimit = preset.preferredBitrate(codec)
        val durationSeconds = metadata.durationMs / 1_000.0
        val sourceSize = metadata.sizeBytes ?: return qualityLimit
        if (durationSeconds <= 0.0) return qualityLimit

        // 目标总大小约为原文件 85%，并给 AAC 音频预留 96 kbps。
        val sizeLimitedBitrate = ((sourceSize * 8 * 0.85 / durationSeconds) - 96_000)
            .toInt()
            .coerceAtLeast(180_000)
        return min(qualityLimit, sizeLimitedBitrate)
    }

    /** 只清理仍属于当前任务的引用，避免旧任务回调影响新任务。 */
    private fun clearIfCurrent(token: String) {
        if (activeToken == token) {
            activeTransformer = null
            activeContinuation = null
            activeToken = null
        }
    }
}

/** 在协程中读取 Transformer 进度，任务结束后自动退出。 */
private fun CoroutineScope.launchProgress(
    transformer: Transformer,
    continuation: CancellableContinuation<File>,
    onProgress: (Int) -> Unit,
) = launch {
    val holder = ProgressHolder()
    while (continuation.isActive) {
        if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
            onProgress(holder.progress.coerceIn(0, 100))
        }
        delay(250)
    }
}
