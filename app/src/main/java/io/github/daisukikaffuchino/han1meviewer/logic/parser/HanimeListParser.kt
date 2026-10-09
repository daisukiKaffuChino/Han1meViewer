package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import org.jsoup.Jsoup

/**
 * 个人收藏 / 播放清单 / 观看历史列表解析。
 */
internal object HanimeListParser {

    fun myListItems(body: String): PagedUiState<MyListItems<HanimeInfo>> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value")
        val desc = parseBody.getElementById("playlist-show-description")?.ownText()
        val allHanimeClass = parseBody.getElementsByClass("horizontal-row").firstOrNull()
        val myListHanimeList = allHanimeClass.extractHanimeInfo("div[class^=user-tab-item-wrapper]")
        val maxPage = parseListMaxPage(parseBody)

        return PagedUiState.Success(
            MyListItems(
                myListHanimeList,
                desc = desc,
                csrfToken = csrfToken,
                maxPage = maxPage,
            )
        )
    }

    fun myPlayListItems(body: String): PagedUiState<MyListItems<HanimeInfo>> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value")
        val desc = parseBody.select("p.playlist-description").first()?.text()
        val allHanimeClass = parseBody.getElementsByClass("playlist-video-list").firstOrNull()
        val myListHanimeList = allHanimeClass.extractHanimeInfo("div[class^=user-tab-item-wrapper]")
        val maxPage = parseListMaxPage(parseBody)

        return PagedUiState.Success(
            MyListItems(
                myListHanimeList,
                desc = desc,
                csrfToken = csrfToken,
                maxPage = maxPage,
            )
        )
    }

    fun onlineWatchHistoryItems(body: String): PagedUiState<MyListItems<HanimeInfo>> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value")
        val allHanimeClass = parseBody.getElementsByClass("horizontal-row").firstOrNull()
        val items = allHanimeClass.extractHanimeInfo("div[class^=user-tab-item-wrapper]")
        return if (items.isEmpty()) {
            PagedUiState.NoMoreData
        } else {
            PagedUiState.Success(
                MyListItems(
                    items,
                    csrfToken = csrfToken,
                    maxPage = parseListMaxPage(parseBody),
                )
            )
        }
    }
}
