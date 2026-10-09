package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 影片详情页解析护栏。
 *
 * fixture `video_detail.html` 由线上真实页面（未登录）脱敏生成：
 * 结构与 class/href/src/size 保持真实，文本一律换成占位符。
 */
class ParserVideoTest {

    @BeforeTest
    fun disableAndroidLogging() {
        LogUtil.enabled = false
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "缺少测试 fixture: $name"
        }.use { it.readBytes().decodeToString() }

    private fun parseFixture(): HanimeVideo {
        val state = Parser.parseHanimeVideoBody(fixture("video_detail.html"), isAvSite = false)
        val success = state as? UiState.Success
            ?: error("video_detail.html 未解析成 Success: $state")
        return success.data
    }

    @Test
    fun `resolutions come from the source tags`() {
        val video = parseFixture()

        // 真实页面是 <source size="720|480|1080" type="video/mp4">，
        // HanimeResolution 按 1080P → 720P → 480P 的顺序输出
        assertEquals(listOf("1080P", "720P", "480P"), video.videoUrls.keys.toList())
        video.videoUrls.forEach { (label, link) ->
            assertEquals("mp4", link.subtype, "$label 的 subtype")
            assertTrue(
                link.link.contains("408583"),
                "$label 的地址应包含影片 code，实际=${link.link}",
            )
        }
    }

    @Test
    fun `metadata is parsed from the detail page`() {
        val video = parseFixture()

        assertEquals("測試影片", video.title)
        assertTrue(video.coverUrl.startsWith("https://vdownload.hembed.com/image/thumbnail/408583"))
        assertEquals("csrf-token-abc", video.csrfToken)
        assertEquals(20, video.tags.size)
        assertEquals("6.7萬次", video.views)
        assertEquals(LocalDate(2026, 10, 8), video.uploadTime)
        assertNotNull(video.artist, "艺术家区块应被解析出来")
        assertTrue(video.relatedHanimes.isNotEmpty(), "相关影片列表不应为空")
        assertNotNull(video.playlist, "播放清单区块应被解析出来")
    }

    @Test
    fun `av site flag rewrites the legacy cdn host`() {
        val body = """
            <html><body>
            <div id="shareBtn-title">測試影片</div>
            <video id="player" poster="https://example.com/p.jpg">
              <source src="https://t12.cdn2020.com/408583-1080p.mp4" type="video/mp4" size="1080">
            </video>
            </body></html>
        """.trimIndent()

        val normal = Parser.parseHanimeVideoBody(body, isAvSite = false) as UiState.Success
        val avSite = Parser.parseHanimeVideoBody(body, isAvSite = true) as UiState.Success

        assertTrue(normal.data.videoUrls.getValue("1080P").link.startsWith("https://t12.cdn2020.com"))
        assertTrue(avSite.data.videoUrls.getValue("1080P").link.startsWith("https://t33.cdn2020.com"))
    }
}
