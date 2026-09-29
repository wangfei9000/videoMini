package com.wangfei.videoshrink.ui

import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangfei.videoshrink.BuildConfig
import com.wangfei.videoshrink.VideoShrinkUiState
import com.wangfei.videoshrink.VideoShrinkViewModel
import com.wangfei.videoshrink.media.VideoMetadata
import com.wangfei.videoshrink.model.CompressionPreset
import com.wangfei.videoshrink.model.VideoCodecOption
import java.io.File
import java.util.Locale

/** 视频压缩主页面：选择、设置、压缩、保存与分享。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoShrinkScreen(viewModel: VideoShrinkViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let(viewModel::selectVideo)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("视频压缩") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "直接在手机上把相册视频输出为 MP4。默认 H.264 兼容模式，也可主动选择 HEVC 省空间模式。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    picker.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
                },
                enabled = !state.isCompressing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.selectedUri == null) "从相册选择视频" else "重新选择视频")
            }

            if (state.isReading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在读取视频信息…")
            }
            state.metadata?.let { VideoInformationCard(it) }

            SettingsSection(
                state = state,
                onPresetSelected = viewModel::selectPreset,
                onCodecSelected = viewModel::selectCodec,
            )

            if (state.isCompressing) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("正在压缩：${state.progress}%")
                OutlinedButton(
                    onClick = viewModel::cancelCompression,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("取消")
                }
            } else {
                Button(
                    onClick = viewModel::startCompression,
                    enabled = state.metadata != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("开始压缩")
                }
            }

            state.outputFile?.let { file ->
                OutputCard(
                    file = file,
                    inputSize = state.metadata?.sizeBytes,
                    onSave = viewModel::saveToGallery,
                    onShare = { shareVideo(context, file) },
                )
            }
            state.statusText?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 显示源视频文件名、大小、时长和显示分辨率。 */
@Composable
private fun VideoInformationCard(metadata: VideoMetadata) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(metadata.displayName, style = MaterialTheme.typography.titleMedium)
            Text("原始大小：${metadata.sizeBytes?.let { Formatter.formatFileSize(context, it) } ?: "未知"}")
            Text("时长：${formatDuration(metadata.durationMs)}")
            Text("分辨率：${metadata.width} × ${metadata.height}")
        }
    }
}

/** 显示分辨率与编码格式选择器。 */
@Composable
private fun SettingsSection(
    state: VideoShrinkUiState,
    onPresetSelected: (CompressionPreset) -> Unit,
    onCodecSelected: (VideoCodecOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("目标分辨率", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompressionPreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { onPresetSelected(preset) },
                    enabled = !state.isCompressing,
                    label = { Text(preset.title) },
                )
            }
        }

        Text("视频编码", style = MaterialTheme.typography.titleMedium)
        VideoCodecOption.entries.forEach { codec ->
            val supported = codec == VideoCodecOption.H264 || state.hevcSupported
            FilterChip(
                selected = state.codec == codec,
                onClick = { onCodecSelected(codec) },
                enabled = !state.isCompressing && supported,
                label = { Text(codec.title) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (!supported) "${codec.description}（当前设备不支持此档位）" else codec.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 显示输出大小、节省比例以及后续操作。 */
@Composable
private fun OutputCard(
    file: File,
    inputSize: Long?,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("压缩结果", style = MaterialTheme.typography.titleMedium)
            Text("输出大小：${Formatter.formatFileSize(context, file.length())}")
            inputSize?.takeIf { it > 0 }?.let { original ->
                val percentage = (1.0 - file.length().toDouble() / original) * 100
                Text(
                    if (percentage >= 0) {
                        "比原文件减少 ${String.format(Locale.getDefault(), "%.1f", percentage)}%"
                    } else {
                        "比原文件增大 ${String.format(Locale.getDefault(), "%.1f", -percentage)}%"
                    },
                )
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("保存到相册")
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("分享视频")
            }
        }
    }
}

/** 用系统分享面板分享缓存文件，FileProvider 避免暴露真实文件路径。 */
private fun shareVideo(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享压缩后的视频"))
}

/** 把毫秒时长格式化为 mm:ss 或 h:mm:ss。 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3_600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
