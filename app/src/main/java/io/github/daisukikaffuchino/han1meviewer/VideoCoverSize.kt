package io.github.daisukikaffuchino.han1meviewer

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import io.github.daisukikaffuchino.utils.applicationContext
import io.github.daisukikaffuchino.utils.dp

/**
 * 用于计算视频封面的大小动态调整！
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/04/08 008 21:54
 */
@SuppressLint("StaticFieldLeak")
object VideoCoverSize {

    private const val TAG = "VideoCoverSize"

    private val context = applicationContext

    private val screenWidth get() = context.resources.displayMetrics.widthPixels

    private val videoCoverWidth =
        context.resources.getDimension(R.dimen.video_cover_width)
    private val simplifiedVideoCoverWidth =
        context.resources.getDimension(R.dimen.video_cover_simplified_width)

    // Ratio of the video cover's width to its height
    private const val RATIO = 15 / 22.0

    /**
     * 自带的一个Margin
     */
    private val margin = 4.dp

    /**
     * 通常父View也会有一个Margin，所以这里也加上
     */
    private val parentMargin = 8.dp

    object Normal {

        /**
         * 最少显示几个视�?
         */
        private const val AT_LEAST = 2

        val videoInOneLine
            get() = (screenWidth / videoCoverWidth).toInt().coerceAtLeast(AT_LEAST)

        fun ViewGroup.resizeForVideoCover(atLeast: Int = AT_LEAST) {
            require(atLeast > 0)
            val screenWidth = screenWidth
            val videoInOneLine = (screenWidth / videoCoverWidth).toInt()
            if (videoInOneLine < atLeast) {
                val width = (screenWidth - parentMargin * 2) / atLeast - margin * 2
                val height = (width * RATIO).toInt()
                updateLayoutParams {
                    this.width = width
                    this.height = height
                }
            }
        }
    }

    object Simplified {

        /**
         * 最少显示几个视�?
         */
        private const val AT_LEAST = 3

        val videoInOneLine
            get() = (screenWidth / simplifiedVideoCoverWidth).toInt().coerceAtLeast(AT_LEAST)

        fun ViewGroup.resizeForVideoCover(atLeast: Int = AT_LEAST) {
            require(atLeast > 0)
            val screenWidth = screenWidth
            val videoInOneLine = (screenWidth / simplifiedVideoCoverWidth).toInt()
            if (videoInOneLine < atLeast) {
                val width = (screenWidth - parentMargin * 2) / atLeast - margin * 2
                val height = (width / RATIO).toInt()
                updateLayoutParams {
                    this.width = width
                    this.height = height
                }
            }
        }
    }
}
