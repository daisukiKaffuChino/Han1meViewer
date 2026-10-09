package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HomePage
import io.github.daisukikaffuchino.han1meviewer.logic.state.HanimeErrorKind
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 首页解析护栏。
 *
 * fixture `home_page.html` 由线上真实页面（未登录）脱敏生成：保留全部模块及其顺序
 * （解析器按 `home-rows-wrapper` 的位置索引取模块），每个模块只留 2 张卡片，文本一律占位符。
 */
class ParserHomeTest {

    @BeforeTest
    fun disableAndroidLogging() {
        LogUtil.enabled = false
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "缺少测试 fixture: $name"
        }.use { it.readBytes().decodeToString() }

    private fun parse(isLoggedIn: Boolean = false): HomePage {
        val state = Parser.parseHomePageBody(
            fixture("home_page.html"),
            isAvSite = false,
            isLoggedIn = isLoggedIn,
        )
        val success = state as? UiState.Success
            ?: error("home_page.html 未解析成 Success: $state")
        return success.data
    }

    @Test
    fun `home sections are read by positional index`() {
        val home = parse()
        val sections = listOf(
            "latestRelease" to home.latestRelease,
            "latestHanime" to home.latestHanime,
            "ecchiAnime" to home.ecchiAnime,
            "shortEpisodeAnime" to home.shortEpisodeAnime,
            "motionAnime" to home.motionAnime,
            "threeDCG" to home.threeDCG,
            "twoPointFiveDAnime" to home.twoPointFiveDAnime,
            "twoDAnime" to home.twoDAnime,
            "aiGenerated" to home.aiGenerated,
            "mmd" to home.mmd,
            "cosplay" to home.cosplay,
            "watchingNow" to home.watchingNow,
        )
        sections.forEach { (name, list) ->
            assertEquals(2, list.size, "$name 应该有 2 张卡片（fixture 每模块截断为 2）")
        }
        // “新番预告”模块用的是 div.home-rows-videos-title，当前线上页面没有该结构
        assertEquals(0, home.newAnimeTrailer.size)
    }

    @Test
    fun `cards carry code, cover and item type`() {
        parse().latestRelease.forEach { card ->
            assertTrue(card.videoCode.isNotEmpty(), "videoCode 不应为空")
            assertTrue(
                card.coverUrl.startsWith("https://vdownload.hembed.com/"),
                "封面地址异常: ${card.coverUrl}",
            )
            assertEquals(HanimeInfo.NORMAL, card.itemType)
            // 文本已脱敏为占位符，只断言非空
            assertEquals("…", card.title)
        }
    }

    @Test
    fun `banner is parsed including the video code from the script`() {
        val banner = assertNotNull(parse().banner, "banner 应被解析出来")
        assertEquals("測試影片", banner.title)
        assertEquals("407358", banner.videoCode)
        assertTrue(
            banner.picUrl.contains("407358h.jpg"),
            "banner 图片地址异常: ${banner.picUrl}",
        )
    }

    @Test
    fun `logged out home page has no user and no csrf token`() {
        val home = parse()
        assertNull(home.username)
        assertNull(home.avatarUrl)
        assertNull(home.csrfToken)
        assertEquals("", home.userId)
    }

    @Test
    fun `logged in but expired session maps to SessionExpired`() {
        // 该页面 user-modal-trigger 指向 /login，登录态下应判定为登录态过期
        val state = Parser.parseHomePageBody(
            fixture("home_page.html"),
            isAvSite = false,
            isLoggedIn = true,
        )
        val error = state as? UiState.Error ?: error("应返回 Error，实际=$state")
        assertEquals(HanimeErrorKind.SessionExpired, error.error.kind)
    }
}
