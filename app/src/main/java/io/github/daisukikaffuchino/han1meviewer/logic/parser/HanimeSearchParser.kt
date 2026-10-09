package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeSearchResult
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.toVideoCode
import io.github.daisukikaffuchino.utils.LogUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 搜索页解析。
 *
 * 页面有两种卡片形态：标准卡片（`div[class^=horizontal-card]`）和简化卡片
 * （`div[class=home-rows-videos-title]`），按容器 class 自动选择。
 */
internal object HanimeSearchParser {

    fun parse(body: String): PagedUiState<HanimeSearchResult> {
        val parseBody = Jsoup.parse(body).body()
        val maxPage = parseSearchMaxPage(parseBody)
        val allContentsClass =
            parseBody.getElementsByClass("content-padding-new").firstOrNull()
        val allSimplifiedContentsClass =
            parseBody.getElementsByClass("home-rows-videos-wrapper").firstOrNull()

        if (allContentsClass != null) {
            return parseNormal(allContentsClass, maxPage)
        } else if (allSimplifiedContentsClass != null) {
            return parseSimplified(allSimplifiedContentsClass, maxPage)
        }
        return PagedUiState.Success(HanimeSearchResult(emptyList(), maxPage))
    }

    // 每一个简化版视频单元
    private fun simplifiedItem(hanimeSearchItem: Element): HanimeInfo? {
        val videoCode = hanimeSearchItem.attr("href").toVideoCode()
            .logIfParseNull("hanimeSimplifiedItem", "videoCode")
        val coverUrl = hanimeSearchItem.selectFirst("img")?.attr("src")
            .logIfParseNull("hanimeSimplifiedItem", "coverUrl")
        val title = hanimeSearchItem.selectFirst("div[class=home-rows-videos-title]")?.text()
            .logIfParseNull("hanimeSimplifiedItem", "title")
        if (videoCode == null || coverUrl == null || title == null) return null
        return HanimeInfo(
            title = title,
            coverUrl = coverUrl,
            videoCode = videoCode,
            itemType = HanimeInfo.SIMPLIFIED
        )
    }

    // 出来后是正常视频单元的页面用这个
    private fun parseNormal(
        allContentsClass: Element,
        maxPage: Int,
    ): PagedUiState<HanimeSearchResult> {
        val hanimeSearchList = mutableListOf<HanimeInfo>()
        val hanimeSearchItems =
            allContentsClass.select("div[class^=horizontal-card]")
        if (hanimeSearchItems.isEmpty()) {
            return PagedUiState.NoMoreData
        } else {
            hanimeSearchItems.forEach { hanimeSearchItem ->
                hanimeNormalItemVer2(hanimeSearchItem)?.let(hanimeSearchList::add)
            }
        }
        LogUtil.d("search_result", "$hanimeSearchList")
        return PagedUiState.Success(HanimeSearchResult(hanimeSearchList, maxPage))
    }

    // 出来后是简化版视频单元的页面用这个
    private fun parseSimplified(
        allSimplifiedContentsClass: Element,
        maxPage: Int,
    ): PagedUiState<HanimeSearchResult> {
        val hanimeSearchList = mutableListOf<HanimeInfo>()
        val hanimeSearchItems = allSimplifiedContentsClass.children()
        if (hanimeSearchItems.isEmpty()) {
            return PagedUiState.NoMoreData
        } else hanimeSearchItems.forEach { hanimeSearchItem ->
            simplifiedItem(hanimeSearchItem)?.let(hanimeSearchList::add)
        }
        return PagedUiState.Success(HanimeSearchResult(hanimeSearchList, maxPage))
    }
}
