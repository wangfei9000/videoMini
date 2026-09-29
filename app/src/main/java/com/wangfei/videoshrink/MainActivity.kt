package com.wangfei.videoshrink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wangfei.videoshrink.ui.VideoShrinkScreen
import com.wangfei.videoshrink.ui.theme.VideoShrinkTheme

/** 应用唯一入口，加载 Compose 视频压缩页面。 */
class MainActivity : ComponentActivity() {
    /** 创建 Activity 并挂载界面。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoShrinkTheme {
                VideoShrinkScreen()
            }
        }
    }
}
