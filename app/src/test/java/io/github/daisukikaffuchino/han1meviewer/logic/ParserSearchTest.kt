package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
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

        val success = state as PageLoadingState.Success
        val result = success.info
        assertEquals(7, result.totalPages)
        assertEquals(2, result.list.size)

        val first = result.list[0]
        assertEquals("123456", first.videoCode)
        assertEquals("測試影片 A", first.title)
        assertEquals("https://hanime1.me/cover/123456.jpg", first.coverUrl)
        assertEquals("12:34", first.duration)
        assertEquals("1.2萬次觀看", first.views)
        assertEquals("作者A", first.currentArtist)
        assertEquals("2024-03-10", first.uploadTime)
        assertEquals("4.5", first.reviews)
        assertEquals(HanimeInfo.NORMAL, first.itemType)

        assertEquals("654321", result.list[1].videoCode)
    }

    @Test
    fun `simplified search page parses cards`() {
        val state = Parser.hanimeSearch(fixture("search_simplified.html"))

        val success = state as PageLoadingState.Success
        val result = success.info
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
        assertEquals(PageLoadingState.NoMoreData, state)
    }

    @Test
    fun `login page csrf token is extracted`() {
        assertEquals(
            "csrf-token-abc",
            Parser.extractTokenFromLoginPage(fixture("login_page.html")),
        )
    }
}
