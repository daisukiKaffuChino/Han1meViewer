package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.model.MySubscriptions
import io.github.daisukikaffuchino.han1meviewer.logic.model.SubscriptionItem
import io.github.daisukikaffuchino.han1meviewer.logic.model.SubscriptionVideosItem
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import org.jsoup.Jsoup

/**
 * 我的订阅页解析。
 */
internal object HanimeSubscriptionParser {

    fun getMySubscriptions(body: String): UiState<MySubscriptions> {
        val parseBody = Jsoup.parse(body).body()
        val maxPage = parseMaxPage(parseBody)
        LogUtil.i("getMySubscriptions", "MaxPageList=$maxPage")
        val subscriptionsRoot = parseBody.selectFirst("div.subscriptions-nav")
            ?: return UiState.Error(IllegalStateException("找不到 subscriptions-nav"))
        val subscriptionsVideosRoot = parseBody.selectFirst("div.content-padding-new")
            ?: return UiState.Error(IllegalStateException("找不到 subscriptionsVideosRoot"))

        // 解析订阅作者
        val artists = subscriptionsRoot.select("div.subscriptions-artist-card").mapNotNull { card ->
            try {
                val imgs = card.select("img")
                val avatarSrc = imgs.getOrNull(1)?.absUrl("src") ?: return@mapNotNull null
                val artistName = card.selectFirst("div.card-mobile-title")?.text()?.trim()
                    ?: return@mapNotNull null

                SubscriptionItem(
                    artistName = artistName,
                    avatar = avatarSrc
                )
            } catch (_: Exception) {
                null
            }
        }

        // 解析订阅视频
        val videos = subscriptionsVideosRoot.select("div[class^=video-item-container]")
            .mapNotNull { videoCard ->
                try {
                    val link =
                        videoCard.selectFirst("a[class^=video-link]")?.absUrl("href") ?: return@mapNotNull null
                    val videoCode = Regex("""watch\?v=(\d+)""").find(link)?.groupValues?.get(1)
                        ?: return@mapNotNull null
                    val coverUrl = videoCard.select("img[class^=main-thumb]").getOrNull(0)?.absUrl("src") ?: return@mapNotNull null
                    val title = videoCard.attr("title").trim()
                    val durationAndViews = videoCard.select("div[class^=thumb-container]")
                    val duration = durationAndViews.select("div[class^=duration]").text()
                    val views = durationAndViews.select("div[class^=stat-item]").getOrNull(1)?.text()
                    val meta = videoCard.extractVideoCardMeta()
                    val infoBoxes = videoCard.selectFirst(".stats-container .stat-item")
                    val reviews = infoBoxes?.ownText()?.trim() ?: ""

                    SubscriptionVideosItem(
                        title = title,
                        coverUrl = coverUrl,
                        videoCode = videoCode,
                        duration = duration,
                        views = views,
                        reviews = reviews,
                        currentArtist = meta.artist,
                        uploadTime = meta.uploadTime
                    )
                } catch (_: Exception) {
                    null
                }
            }

        return UiState.Success(
            MySubscriptions(
                subscriptions = artists,
                subscriptionsVideos = videos,
                maxPage = maxPage
            )
        )
    }
}
