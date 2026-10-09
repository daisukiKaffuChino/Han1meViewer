package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_URL
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.toVideoCode
import io.github.daisukikaffuchino.utils.LogUtil
import org.jsoup.nodes.Element

/**
 * Parser 各域共用的正则。
 */
internal object ParserRegex {
    val videoSource = Regex("""const source = '(.+)'""")
    val viewAndUploadTime = Regex("""(觀看次數|观看次数)：(.+次) *(\d{4}-\d{2}-\d{2})""")

    // AV 站的 CDN 節點（t26 / t27 / t30 等）可能已失效，需要統一切換成可用的 t33
    val avCdnHost = Regex("""^(\s*(?:https?:)?//)t\d+\.cdn2020\.com(?=[/:]|\z)""", RegexOption.IGNORE_CASE)
}

/** 目标站点是否为 AV 站（影响 CDN 域名修正）。 */
internal val isAvSiteFromSettings: Boolean
    get() = SettingsRepository.baseUrl == HANIME_URL[3]

/**
 * AV 站的 CDN 節點可能已失效，這裡統一修正成可用的節點；其餘站點原樣返回。
 */
internal fun fixAvCdnHost(url: String, isAvSite: Boolean): String {
    if (!isAvSite) return url
    return ParserRegex.avCdnHost.replace(url) { "${it.groupValues[1]}t33.cdn2020.com" }
}

/**
 * 基本都是必需的參數，所以如果是 null，就直接丟出 [ParseException]
 *
 * @param funcName 這個參數是在哪個函數中被使用的
 * @param varName 這個參數的名稱
 * @return 如果 [this] 不是 null，就回傳 [this]
 * @throws ParseException 如果 [this] 是 null，就丟出 [ParseException]
 */
internal fun <T> T?.throwIfParseNull(funcName: String, varName: String): T = this
    ?: throw ParseException(funcName, varName)

/**
 * 如果 [this] 是 null，就在 logcat 中顯示訊息
 *
 * @param funcName 這個參數是在哪個函數中被使用的
 * @param varName 這個參數的名稱
 * @return 回傳 [this]
 */
internal fun <T> T?.logIfParseNull(
    funcName: String, varName: String, loginNeeded: Boolean = false,
): T? = also {
    if (it == null) {
        val extra = if (loginNeeded) "（需要登入的欄位）" else ""
        LogUtil.d("Parse::$funcName", "[$varName] is null. 這有點不正常$extra")
    }
}

/**
 * 得到 Element 的 child，如果 index 超出範圍，就返回 null
 */
internal fun Element.childOrNull(index: Int): Element? {
    return try {
        child(index)
    } catch (_: IndexOutOfBoundsException) {
        null
    }
}

internal fun parseMaxPage(parseBody: Element): Int {
    return parseBody
        .select("ul.pagination")
        .lastOrNull()
        ?.select("a.page-link[href]")
        ?.mapNotNull {
            Regex("""[?&]page=(\d+)""").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
        }
        ?.maxOrNull() ?: 1
}

/**
 * 搜索页的总页数藏在「跳到指定页」输入框中。
 */
internal fun parseSearchMaxPage(parseBody: Element): Int {
    val input = parseBody.selectFirst("input#skip-page-input") ?: return 1
    return Regex("""validateNumberInput\(this,\s*\d+,\s*(\d+)\)""")
        .find(input.attr("oninput"))
        ?.groupValues?.get(1)
        ?.toIntOrNull()
        ?: 1
}

/**
 * 优先解析通用跳页输入框，失败后回退到订阅页的分页链接结构。
 */
internal fun parseListMaxPage(parseBody: Element): Int {
    val searchMax = parseSearchMaxPage(parseBody)
    return if (searchMax > 1) searchMax else parseMaxPage(parseBody)
}

internal data class VideoCardMeta(
    val artist: String = "",
    val uploadTime: String = "",
    val genre: String? = null,
)

internal fun Element.extractVideoCardMeta(): VideoCardMeta {
    val subtitleText = selectFirst("div.subtitle a, div.subtitle")
        ?.text()
        ?.trim()
        .orEmpty()
    if (subtitleText.isNotBlank()) {
        val parts = subtitleText.split("•").map { it.trim() }.filter { it.isNotEmpty() }
        return VideoCardMeta(
            artist = parts.getOrNull(0).orEmpty(),
            uploadTime = parts.getOrNull(1).orEmpty(),
        )
    }

    val metaDataText = selectFirst("div.video-meta-data a, div.video-meta-data")
        ?.text()
        ?.trim()
        .orEmpty()
    if (metaDataText.isNotBlank()) {
        val parts = metaDataText.split("•").map { it.trim() }.filter { it.isNotEmpty() }
        return VideoCardMeta(
            artist = parts.getOrNull(0).orEmpty(),
            uploadTime = parts.getOrNull(1).orEmpty(),
        )
    }

    return VideoCardMeta(
        artist = selectFirst(".meta-author a, a.card-mobile-user")?.text()?.trim().orEmpty(),
        uploadTime = selectFirst(".meta-stats span")?.text()?.trim().orEmpty(),
        genre = selectFirst(".meta-stats a")?.text()?.trim(),
    )
}

/** 一个标准（非简化）视频卡片单元。 */
internal fun hanimeNormalItemVer2(hanimeSearchItem: Element): HanimeInfo? {
    val title =
        hanimeSearchItem.selectFirst("div.title, h4.video-title")?.text()?.trim()
            .logIfParseNull("hanimeNormalItemVer2", "title")
    val coverUrl =
        hanimeSearchItem.select("img").getOrNull(0)?.absUrl("src")
            .logIfParseNull("hanimeNormalItemVer2", "coverUrl")
    val videoCode =
        hanimeSearchItem.select("a").getOrNull(0)?.absUrl("href")?.toVideoCode()
            .logIfParseNull("hanimeNormalItemVer2", "videoCode")
    if (title == null || coverUrl == null || videoCode == null) return null
    val durationAndViews = hanimeSearchItem.select("div[class^=thumb-container]")
    val duration = durationAndViews.select("div[class^=duration]").text()
    val views = durationAndViews.select("div[class^=stat-item]").getOrNull(1)?.text()
    val meta = hanimeSearchItem.extractVideoCardMeta()
    val infoBoxes = hanimeSearchItem.selectFirst(".stats-container .stat-item")
    val reviews = infoBoxes?.ownText()?.trim() ?: ""
    return HanimeInfo(
        title = title,
        coverUrl = coverUrl,
        videoCode = videoCode,
        duration = duration.logIfParseNull("hanimeNormalItemVer2", "duration"),
        currentArtist = meta.artist,
        views = views.logIfParseNull("hanimeNormalItemVer2", "views"),
        uploadTime = meta.uploadTime,
        genre = meta.genre,
        itemType = HanimeInfo.NORMAL,
        reviews = reviews
    )
}

/** 把一组卡片解析成影片列表（selector 默认匹配标准卡片）。 */
internal fun Element?.extractHanimeInfo(
    selector: String = "div[class^=horizontal-card]",
): MutableList<HanimeInfo> {
    val resultList = mutableListOf<HanimeInfo>()
    this?.select(selector)?.forEach { item ->
        hanimeNormalItemVer2(item)?.let { hanimeInfo ->
            resultList.add(hanimeInfo)
        }
    }
    return resultList
}
