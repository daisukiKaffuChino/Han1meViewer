package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.utils.LogUtil
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parser 解析护栏。
 *
 * `Parser` 内部通过 [LogUtil] 调用 `android.util.Log`，测试里把 [LogUtil.enabled]
 * 置为 false 即可短路日志调用。
 *
 * fixture 说明：`search_normal.html` / `login_page.html` 由线上真实页面生成（脱敏：
 * 标题、作者等文本替换为占位符，结构与 class/href 保持真实）；其余为按选择器构造的样例。
 */
class ParserSearchTest {

    @BeforeTest
    fun disableAndroidLogging() {
        LogUtil.enabled = false
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "缺少测试 fixture: $name"
        }.use { it.readBytes().decodeToString() }

    @Test
    fun `normal search page parses cards and total pages`() {
        val state = Parser.hanimeSearch(fixture("search_normal.html"))

        val success = state as PagedUiState.Success
        val result = success.data
        assertEquals(239, result.totalPages)
        assertEquals(3, result.list.size)

        val first = result.list[0]
        assertEquals("408583", first.videoCode)
        assertEquals("測試影片", first.title)
        assertTrue(first.coverUrl.startsWith("https://vdownload.hembed.com/image/thumbnail/408583"))
        assertEquals("06:00", first.duration)
        assertEquals("6.2萬次", first.views)
        assertEquals("作者X", first.currentArtist)
        assertEquals("11小時前", first.uploadTime)
        assertEquals("98%", first.reviews)
        assertEquals(HanimeInfo.NORMAL, first.itemType)

        assertEquals("408584", result.list[1].videoCode)
        assertEquals("408581", result.list[2].videoCode)
    }

    @Test
    fun `simplified search page parses cards`() {
        val state = Parser.hanimeSearch(fixture("search_simplified.html"))

        val success = state as PagedUiState.Success
        val result = success.data
        assertEquals(3, result.totalPages)
        assertEquals(2, result.list.size)
        assertEquals("777001", result.list[0].videoCode)
        assertEquals("簡化影片 A", result.list[0].title)
        assertEquals("/cover/777001.jpg", result.list[0].coverUrl)
        assertEquals(HanimeInfo.SIMPLIFIED, result.list[0].itemType)
        assertTrue(result.list.all { it.itemType == HanimeInfo.SIMPLIFIED })
    }

    @Test
    fun `search page without cards reports no more data`() {
        val state = Parser.hanimeSearch(fixture("search_empty.html"))
        assertEquals(PagedUiState.NoMoreData, state)
    }

    @Test
    fun `login page csrf token is extracted`() {
        assertEquals(
            "csrf-token-abc",
            Parser.extractTokenFromLoginPage(fixture("login_page.html")),
        )
    }
}
