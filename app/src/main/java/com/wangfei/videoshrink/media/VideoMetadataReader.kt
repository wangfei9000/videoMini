package com.wangfei.videoshrink.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

/** 压缩决策所需的输入视频元数据。 */
data class VideoMetadata(
    val displayName: String,
    val sizeBytes: Long?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
) {
    val isPortrait: Boolean
        get() = if (rotation == 90 || rotation == 270) width > height else height > width

    val displayHeight: Int
        get() = if (rotation == 90 || rotation == 270) width else height
}

/** 从 content URI 中读取名称、大小、时长、尺寸和方向。 */
object VideoMetadataReader {
    fun read(context: Context, uri: Uri): VideoMetadata {
        var name = "selected-video"
        var size: Long? = null
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            VideoMetadata(
                displayName = name,
                sizeBytes = size,
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                )?.toLongOrNull() ?: 0L,
                width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
                )?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
                )?.toIntOrNull() ?: 0,
                rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
                )?.toIntOrNull() ?: 0,
            )
        } finally {
            retriever.release()
        }
    }
}
