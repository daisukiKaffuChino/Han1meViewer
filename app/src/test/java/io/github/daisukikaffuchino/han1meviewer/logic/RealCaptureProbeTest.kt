package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Comment
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 临时探针：用线上抓到的真实页面验证 Parser，并生成脱敏后的 fixture。
 * 通过环境变量 HA1_CAPTURE_DIR / HA1_FIXTURE_DIR 指定输入输出目录；未设置时跳过。
 */
class RealCaptureProbeTest {

    private val captureDir = System.getenv("HA1_CAPTURE_DIR")
    private val fixtureDir = System.getenv("HA1_FIXTURE_DIR")

    @Test
    fun `build sanitized fixtures from real captures`() {
        if (captureDir == null || fixtureDir == null) return
        LogUtil.enabled = false

        val searchFile = File(captureDir).listFiles()?.firstOrNull { it.name.startsWith("real_search_") && it.length() > 1000 }
            ?: error("real search capture not found in $captureDir")
        val loginFile = File(captureDir).listFiles()?.firstOrNull { it.name.startsWith("real_login_") && it.length() > 1000 }
            ?: error("real login capture not found in $captureDir")

        val realSearchHtml = searchFile.readText()
        val originalTitles = Jsoup.parse(realSearchHtml)
            .select("div.title, h4.video-title")
            .map { it.text().trim() }
            .filter { it.length > 4 }

        // 真实页面必须能被解析
        val realState = Parser.hanimeSearch(realSearchHtml)
        val realSuccess = realState as? PagedUiState.Success
            ?: error("real search page did not parse into Success: $realState")
        assertTrue(realSuccess.data.totalPages >= 1)
        assertTrue(realSuccess.data.list.isNotEmpty(), "real search page should contain items")
        println("PROBE real-search items=${realSuccess.data.list.size} totalPages=${realSuccess.data.totalPages} firstCode=${realSuccess.data.list.first().videoCode}")

        val searchFixture = sanitizeSearch(searchFile.readText())
        val loginFixture = sanitizeLogin(loginFile.readText())

        val watchFile = File(captureDir).listFiles()
            ?.firstOrNull { it.name.startsWith("real_watch_") && it.length() > 1000 }
            ?: error("real watch capture not found in $captureDir")
        val realWatchHtml = watchFile.readText()
        val videoFixture = sanitizeVideo(realWatchHtml)

        val homeFile = File(captureDir).listFiles()
            ?.firstOrNull { it.name.startsWith("real_home_") && it.length() > 1000 }
            ?: error("real home capture not found in $captureDir")
        val realHomeHtml = homeFile.readText()
        val homeFixture = sanitizeHome(realHomeHtml)

        // 脱敏检查：生成物里不能残留任何原标题文本
        originalTitles.forEach { title ->
            assertTrue(!searchFixture.contains(title), "sanitized fixture leaked a title: ${title.take(12)}…")
        }
        // 详情页：只允许白名单元素（观看次数/日期行）里出现中文，其余必须是占位符
        val viewsLine = findViewsLines(Jsoup.parse(realWatchHtml).body()).joinToString(" ") { it.text() }
        val allowedRuns = Regex("[\\u3000-\\u9FFF\\uFF00-\\uFFEF]{2,}")
            .findAll(viewsLine).map { it.value }.toSet() + setOf("測試影片")
        val leakedRuns = Regex("[\\u3000-\\u9FFF\\uFF00-\\uFFEF]{2,}")
            .findAll(videoFixture).map { it.value }.toSet() - allowedRuns - setOf("…")
        assertTrue(leakedRuns.isEmpty(), "video fixture leaked unexpected text: $leakedRuns")
        val homeLeaks = Regex("[\\u3000-\\u9FFF\\uFF00-\\uFFEF]{2,}")
            .findAll(homeFixture).map { it.value }.toSet() - setOf("測試影片", "…")
        assertTrue(homeLeaks.isEmpty(), "home fixture leaked unexpected text: $homeLeaks")

        File(fixtureDir).mkdirs()
        File(fixtureDir, "search_normal.html").writeText(searchFixture)
        File(fixtureDir, "login_page.html").writeText(loginFixture)
        File(fixtureDir, "video_detail.html").writeText(videoFixture)
        File(fixtureDir, "home_page.html").writeText(homeFixture)

        // 生成后的 fixture 也必须能被解析
        val fixtureState = Parser.hanimeSearch(searchFixture)
        val fixtureSuccess = fixtureState as? PagedUiState.Success
            ?: error("generated fixture did not parse into Success: $fixtureState")
        assertTrue(fixtureSuccess.data.list.isNotEmpty(), "generated fixture should contain items")
        assertEquals(realSuccess.data.totalPages, fixtureSuccess.data.totalPages)
        println("PROBE fixture items=${fixtureSuccess.data.list.size} totalPages=${fixtureSuccess.data.totalPages} bytes=${searchFixture.length}")
        println("PROBE login token=${Parser.extractTokenFromLoginPage(loginFixture)}")

        // 真实详情页（未登录）必须能被解析；只打印结构信息，不打印标题
        val realVideo = (Parser.parseHanimeVideoBody(realWatchHtml, isAvSite = false) as? UiState.Success)?.data
            ?: error("real watch page did not parse into Success")
        println(
            "PROBE real-video resolutions=${realVideo.videoUrls.keys} tags=${realVideo.tags.size} " +
                "related=${realVideo.relatedHanimes.size} artist=${realVideo.artist != null} " +
                "myList=${realVideo.myList?.myListInfo?.size} playlist=${realVideo.playlist?.video?.size} " +
                "cover=${realVideo.coverUrl.startsWith("https://vdownload.hembed.com/")} " +
                "views=${realVideo.views} uploadTime=${realVideo.uploadTime}"
        )
        assertTrue(realVideo.videoUrls.isNotEmpty(), "real watch page should expose resolutions")

        val fixtureVideo = (Parser.parseHanimeVideoBody(videoFixture, isAvSite = false) as? UiState.Success)?.data
            ?: error("generated video fixture did not parse into Success")
        assertEquals(realVideo.videoUrls.keys.toList(), fixtureVideo.videoUrls.keys.toList())
        assertEquals(realVideo.tags.size, fixtureVideo.tags.size)
        assertTrue(
            fixtureVideo.relatedHanimes.isNotEmpty() && fixtureVideo.relatedHanimes.size <= 3,
            "fixture related list should be truncated to 1..3, was ${fixtureVideo.relatedHanimes.size}",
        )
        println(
            "PROBE video-fixture resolutions=${fixtureVideo.videoUrls.keys} tags=${fixtureVideo.tags.size} " +
                "related=${fixtureVideo.relatedHanimes.size} bytes=${videoFixture.length}"
        )

        // 首页（未登录）
        val realHome = (Parser.parseHomePageBody(realHomeHtml, isAvSite = false, isLoggedIn = false) as? UiState.Success)?.data
            ?: error("real home page did not parse into Success")
        println(
            "PROBE real-home sections=" +
                " latestRelease=${realHome.latestRelease.size} latestUpload=${realHome.latestHanime.size}" +
                " ecchi=${realHome.ecchiAnime.size} shortEpisode=${realHome.shortEpisodeAnime.size}" +
                " motion=${realHome.motionAnime.size} threeDCG=${realHome.threeDCG.size}" +
                " twoPointFiveD=${realHome.twoPointFiveDAnime.size} twoD=${realHome.twoDAnime.size}" +
                " ai=${realHome.aiGenerated.size} mmd=${realHome.mmd.size} cosplay=${realHome.cosplay.size}" +
                " watchingNow=${realHome.watchingNow.size} trailer=${realHome.newAnimeTrailer.size}" +
                " banner=${realHome.banner != null} bannerCode=${realHome.banner?.videoCode}" +
                " csrf=${realHome.csrfToken != null}"
        )

        val homeFixtureParsed = (Parser.parseHomePageBody(homeFixture, isAvSite = false, isLoggedIn = false) as? UiState.Success)?.data
            ?: error("generated home fixture did not parse into Success")
        assertEquals(realHome.banner?.videoCode, homeFixtureParsed.banner?.videoCode)
        // 生成时会把 URL 查询串裁到白名单（v / page），签名参数 secure= 会被去掉
        assertEquals(
            realHome.banner?.picUrl?.substringBefore('?'),
            homeFixtureParsed.banner?.picUrl,
        )
        assertEquals("測試影片", homeFixtureParsed.banner?.title)
        assertTrue(homeFixtureParsed.latestRelease.size in 1..2, "latestRelease=${homeFixtureParsed.latestRelease.size}")
        assertTrue(homeFixtureParsed.ecchiAnime.size in 1..2, "ecchi=${homeFixtureParsed.ecchiAnime.size}")
        assertTrue(homeFixtureParsed.watchingNow.size in 1..2, "watchingNow=${homeFixtureParsed.watchingNow.size}")
        println(
            "PROBE home-fixture latestRelease=${homeFixtureParsed.latestRelease.size}" +
                " ecchi=${homeFixtureParsed.ecchiAnime.size} banner=${homeFixtureParsed.banner != null}" +
                " bytes=${homeFixture.length}"
        )
    }

    private val keepTextSelectors = listOf(
        "div.subtitle",
        "div[class^=duration]",
        "div[class^=stat-item]",
        ".meta-stats span",
    )

    /** 只保留分页输入框 + 结果容器里的前几张卡片，并白名单式脱敏文本。 */
    private fun sanitizeSearch(html: String): String {
        val source = Jsoup.parse(html)
        val container = source.selectFirst("div.content-padding-new")
            ?: error("content-padding-new not found in real capture")
        val pageInput = source.selectFirst("input#skip-page-input")

        // 只保留解析器真正认得的前 3 张卡片（有些卡片缺少必要字段会被跳过）
        val parseable = container.select("div[class^=horizontal-card]").toList()
            .filter { card ->
                Parser.hanimeSearch(wrap(card)).let { it is PagedUiState.Success && it.data.list.isNotEmpty() }
            }
            .take(3)
        check(parseable.isNotEmpty()) { "no parseable card found in real capture" }
        container.select("div[class^=horizontal-card]").forEach { card ->
            if (card !in parseable) card.remove()
        }
        // 清掉被删卡片留下的空容器（以及只包着空容器的广告 span）
        container.select("div.video-item-container").filter { it.childrenSize() == 0 }.forEach { it.remove() }
        container.select("span").filter { it.childrenSize() == 0 && it.text().isBlank() }.forEach { it.remove() }

        parseable.forEach { card ->
            val date = card.selectFirst("div.subtitle")?.text()?.substringAfter("•")?.trim().orEmpty()
            card.selectFirst("div.subtitle")?.text(if (date.isNotBlank()) "作者X • $date" else "作者X")
            redactText(card, card.select(keepTextSelectors.joinToString(",")).toHashSet())
            card.select("div.title, h4.video-title").forEach { it.text("測試影片") }
            card.select("[alt], [title]").forEach { el ->
                if (el.hasAttr("alt")) el.attr("alt", "cover")
                if (el.hasAttr("title")) el.attr("title", "")
            }
        }

        val out = Jsoup.parse("<html><head><title>fixture</title></head><body></body></html>")
        pageInput?.let { out.body().appendChild(it) }
        out.body().appendChild(container)
        redactAttributes(out)
        return out.outerHtml()
    }

    /** 只保留登录表单，并把 CSRF token 换成固定占位符。 */
    private fun sanitizeLogin(html: String): String {
        val source = Jsoup.parse(html)
        val form = source.selectFirst("form") ?: error("login form not found in real capture")
        form.selectFirst("input[name=_token]")?.attr("value", "csrf-token-abc")
        form.select("input[name=email]").forEach { it.attr("value", "") }
        form.select("input[name=password]").forEach { it.attr("value", "") }
        redactText(form, emptySet())
        val out = Jsoup.parse("<html><head><title>fixture</title></head><body></body></html>")
        out.body().appendChild(form)
        redactAttributes(out)
        return out.outerHtml()
    }

    /** 详情页：保留整页结构，去掉 script/style，只保留“观看次数 + 上映日期”那一行的文本。 */
    private fun sanitizeVideo(html: String): String {
        val source = Jsoup.parse(html)
        source.select("script, style, noscript").remove()
        val body = source.body()
        removeComments(body)
        // 去掉与解析无关的大块，并截断相关视频 / 播放清单列表
        body.select("nav, header, footer, aside").remove()

        val detailWrapper = body.selectFirst("div[class=video-details-wrapper]")
        val keepers = mutableSetOf<Element>()
        keepers.addAll(findViewsLines(body))
        body.getElementById("shareBtn-title")?.let {
            it.text("測試影片")
            keepers.add(it)
        }

        redactText(body, keepers)
        redactAttributes(body)
        sanitizeUrls(body)
        dropNoiseAttributes(body)
        body.getElementById("shareBtn-title")?.text("測試影片")

        // 只保留必要的隐藏字段，token / 用户 id 换成占位符
        body.selectFirst("input[name=_token]")?.attr("value", "csrf-token-abc")
        body.select("input[name=like-user-id], input[name=subscribe-user-id]")
            .forEach { it.attr("value", "12345") }

        // 相关视频：解析器只读 `horizontal-card`；桌面端 `desktop-grid-item` 解析器不读，整体删掉
        body.getElementById("related-tabcontent")?.let { related ->
            related.select("div.desktop-grid-item").drop(3).forEach { it.remove() }
        }
        // 详情页只通过 #related-tabcontent 读卡片，其它地方的卡片对解析无用，只留 3 张
        val keptCards = body.getElementById("related-tabcontent")
            ?.select("div[class^=horizontal-card]")
            ?.take(3)?.toSet().orEmpty()
        body.select("div[class^=horizontal-card]").forEach { card ->
            if (card !in keptCards) card.remove()
        }
        // 播放清单里还有一批 `video-thumb-container horizontal-card` 卡片，同样只留 2 张
        body.select("div.video-thumb-container.horizontal-card").drop(2).forEach { it.remove() }
        body.selectFirst("#playlist-scroll")
            ?.children()?.drop(2)?.forEach { it.remove() }

        val out = Jsoup.parse("<html><head><title>fixture</title></head><body></body></html>")
        body.childNodes().toList().forEach { out.body().appendChild(it) }
        return out.outerHtml()
    }

    /** 首页：保留全部模块（解析器按位置索引取），每个模块只留 2 张卡片，文本全部占位符。 */
    private fun sanitizeHome(html: String): String {
        val source = Jsoup.parse(html)
        val body = source.body()

        // banner 的视频 code 只存在于脚本里，先取出（脚本随后会被清掉）
        val bannerCode = body.select("script")
            .firstOrNull { it.data().contains("watch?v=") }
            ?.data()
            ?.let { Regex("watch\\?v=(\\d+)").find(it)?.groupValues?.get(1) }

        body.select("script, style, noscript").remove()
        removeComments(body)
        dropNoiseAttributes(body)
        sanitizeUrls(body)

        val sections = body.select("div[id=home-rows-wrapper] > div")
        sections.forEach { section ->
            section.select("div[class^=horizontal-card]").drop(2).forEach { it.remove() }
            section.select("div.video-thumb-container.horizontal-card").drop(2).forEach { it.remove() }
        }

        redactText(body, emptySet())
        redactAttributes(body)

        body.selectFirst("div[id=home-banner-wrapper]")
            ?.previousElementSibling()?.selectFirst("img")?.attr("alt", "測試影片")
        if (bannerCode != null) {
            body.selectFirst("div[id=home-banner-wrapper]")
                ?.appendElement("script")
                ?.text("var sourceUrl = 'https://hanime1.me/watch?v=$bannerCode';")
        }

        val out = Jsoup.parse("<html><head><title>fixture</title></head><body></body></html>")
        body.childNodes().toList().forEach { out.body().appendChild(it) }
        return out.outerHtml()
    }

    private fun wrap(element: Element): String =
        "<html><body><div class=\"content-padding-new\">${element.outerHtml()}</div></body></html>"

    /** “观看次数 + 上映日期”那一行（解析器读的 views/uploadTime 来源；桌面/移动各一份）。 */
    private fun findViewsLines(root: Element): List<Element> =
        root.select("div").filter { element ->
            element.ownText().let { it.contains("觀看次數") || it.contains("观看次数") }
        }

    /** 文本可能藏在 title / alt / aria-label 等属性里（如父容器的 title），统一清空。 */
    private fun redactAttributes(root: Element) {
        root.select("[title]").forEach { it.attr("title", "") }
        root.select("[aria-label]").forEach { it.attr("aria-label", "") }
        root.select("[placeholder]").forEach { it.attr("placeholder", "") }
        root.select("[alt]").forEach { it.attr("alt", "cover") }
    }

    /** URL 查询参数里也会带正文（如 ?genre=xxx、?query=xxx），只保留解析需要的参数。 */
    private fun sanitizeUrls(root: Element) {
        val urlAttrs = setOf("href", "src", "data-src", "data-href", "poster", "action")
        val keepParams = setOf("v", "page")
        root.getAllElements().forEach { element ->
            element.attributes().toList().forEach { attribute ->
                val name = attribute.key
                val value = attribute.value
                if (value.isBlank()) return@forEach
                if (name in urlAttrs) {
                    element.attr(name, sanitizeUrl(value, keepParams))
                } else if (value.any { it.code > 127 }) {
                    element.attr(name, "")
                }
            }
        }
    }

    private fun sanitizeUrl(url: String, keepParams: Set<String>): String {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val base = url.substring(0, queryStart)
        val kept = url.substring(queryStart + 1)
            .substringBefore('#')
            .split('&')
            .filter { it.substringBefore('=') in keepParams }
        return if (kept.isEmpty()) base else "$base?${kept.joinToString("&")}"
    }

    private fun removeComments(root: Element) {
        root.childNodes().toList().forEach { node ->
            if (node is Comment) node.remove() else if (node is Element) removeComments(node)
        }
    }

    /** 页面里大量内联样式 / data-* / on* 属性与解析无关，删掉可显著缩小 fixture。 */
    private fun dropNoiseAttributes(root: Element) {
        root.getAllElements().forEach { element ->
            element.attributes().toList().forEach { attribute ->
                val name = attribute.key
                val noise = name.startsWith("aria-") ||
                    name.startsWith("on") ||
                    name.startsWith("data-") && name != "data-href" ||
                    name == "style" && element.tagName() != "img"
                if (noise) element.removeAttr(name)
            }
        }
    }

    /** 白名单之外的所有文本节点替换成占位符。 */
    private fun redactText(root: Element, keepers: Set<Element>) {
        fun isKept(node: Node): Boolean {
            var parent = node.parent()
            while (parent != null) {
                if (parent in keepers) return true
                parent = parent.parent()
            }
            return false
        }
        fun walk(node: Node) {
            if (node is TextNode) {
                if (!isKept(node) && node.text().isNotBlank()) node.text("…")
            } else {
                node.childNodes().toList().forEach(::walk)
            }
        }
        walk(root)
    }
}
