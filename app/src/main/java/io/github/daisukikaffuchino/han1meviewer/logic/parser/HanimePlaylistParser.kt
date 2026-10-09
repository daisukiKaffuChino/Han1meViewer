package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import org.jsoup.Jsoup

/**
 * 播放清单列表页解析。
 */
internal object HanimePlaylistParser {

    fun playlists(body: String): UiState<Playlists> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value")
        val lists = parseBody.getElementsByClass("user-tab-item-wrapper")
        val playlists = mutableListOf<Playlists.Playlist>()
        lists.forEach {
            val listCode = it.selectFirst("a[class=video-link]")?.absUrl("href")?.substringAfter('=')
                .throwIfParseNull("playlists", "listCode")
            val listTitle = it.selectFirst("div[class=title]")?.ownText()
                .throwIfParseNull("playlists", "listTitle")
            val listTotal = it.selectFirst("div[class=stat-item]")?.text()
            val formatedTotal = listTotal?.filter { char -> char.isDigit() }?.toIntOrNull() ?: -1
            val coverUrl = it.select("img[class=main-thumb]").first()?.attr("src")
            playlists += Playlists.Playlist(
                listCode = listCode, title = listTitle, total = formatedTotal, coverUrl = coverUrl
            )
        }
        return UiState.Success(
            Playlists(
                playlists = playlists,
                csrfToken = csrfToken,
                maxPage = parseListMaxPage(parseBody),
            )
        )
    }
}
