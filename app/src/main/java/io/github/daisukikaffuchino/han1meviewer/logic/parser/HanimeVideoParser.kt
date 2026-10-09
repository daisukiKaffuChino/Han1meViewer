package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.HanimeResolution
import io.github.daisukikaffuchino.han1meviewer.LOCAL_DATE_FORMAT
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.toVideoCode
import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.datetime.LocalDate
import org.jsoup.Jsoup

/**
 * 影片详情页解析。
 */
internal object HanimeVideoParser {

    fun parse(body: String): UiState<HanimeVideo> =
        parseHanimeVideoBody(body, isAvSite = isAvSiteFromSettings)

    fun parseHanimeVideoBody(body: String, isAvSite: Boolean): UiState<HanimeVideo> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value") // csrf token

        val currentUserId =
            parseBody.selectFirst("input[name=like-user-id]")?.attr("value") // current user id

        val title = parseBody.getElementById("shareBtn-title")?.text()
            .throwIfParseNull("hanimeVideo", "title")

        var likeStatus = parseBody.selectFirst("[name=like-status]")
            ?.attr("value")
        LogUtil.i("likeStatus", likeStatus.toString())
        if (!likeStatus.isNullOrEmpty()) {
            likeStatus = "1"
        }
        var unlikeStatus = parseBody.selectFirst("[name=unlike-status]")
            ?.attr("value")
        if (!unlikeStatus.isNullOrEmpty()) {
            unlikeStatus = "1"
        }
        val likesCount = parseBody.selectFirst("input[name=likes-count]")
            ?.attr("value")?.toIntOrNull()
        val unlikesCount = parseBody.selectFirst("input[name=unlikes-count]")
            ?.attr("value")?.toIntOrNull()
        val videoDetailWrapper = parseBody.selectFirst("div[class=video-details-wrapper]")
        val videoCaptionText = videoDetailWrapper?.selectFirst("div[class^=video-caption-text]")
        val chineseTitle = videoCaptionText?.previousElementSibling()?.ownText()
        val introduction = videoCaptionText?.ownText()
        val uploadTimeWithViews = videoDetailWrapper?.selectFirst("div > div > div")?.text()
        val uploadTimeWithViewsGroups = uploadTimeWithViews?.let {
            ParserRegex.viewAndUploadTime.find(it)?.groups
        }
        val uploadTime = uploadTimeWithViewsGroups?.get(3)?.value?.let { time ->
            runCatching {
                LocalDate.parse(time, LOCAL_DATE_FORMAT)
            }.getOrNull()
        }

        val views = uploadTimeWithViewsGroups?.get(2)?.value

        val tags = parseBody.getElementsByClass("single-video-tag")
        val tagListWithLikeNum = mutableListOf<String>()
        tags.forEach { tag ->
            val child = tag.childOrNull(0)
            if (child != null && child.hasAttr("href")) {
                tagListWithLikeNum.add(child.text())
            }
        }
        val tagList = tagListWithLikeNum.map {
            it.substringBefore(" (")
                .removePrefix("#")
                .trim()
        }
        val myListCheckboxWrapper = parseBody.select("div[class~=playlist-checkbox-wrapper]")
        val myListInfo = mutableListOf<HanimeVideo.MyList.MyListInfo>()
        myListCheckboxWrapper.forEach {
            val listTitle = it.selectFirst("span")?.ownText()
                .logIfParseNull("hanimeVideo", "myListTitle", loginNeeded = true)
            val listInput = it.selectFirst("input")
            val listCode = listInput?.attr("id")
                .logIfParseNull("hanimeVideo", "myListCode", loginNeeded = true)
            val isSelected = listInput?.hasAttr("checked") == true
            if (listTitle != null && listCode != null) {
                myListInfo += HanimeVideo.MyList.MyListInfo(
                    code = listCode, title = listTitle, isSelected = isSelected
                )
            }
        }
        val isWatchLater = parseBody.getElementById("playlist-save-checkbox")
            ?.selectFirst("input")?.hasAttr("checked") == true
        val myList = HanimeVideo.MyList(isWatchLater = isWatchLater, myListInfo = myListInfo)

        val playlistWrapper = parseBody.selectFirst("div.video-playlist-wrapper")
            ?: parseBody.selectFirst("div[id=video-playlist-wrapper]")
        val playlist = playlistWrapper?.let {
            val playlistVideoList = mutableListOf<HanimeInfo>()
            val playlistScroll = it.getElementById("playlist-scroll")
            if (playlistScroll != null) {
                val children = playlistScroll.children()
                if (children.firstOrNull()?.hasClass("playlist-hover-wrap") == true) {
                    // 新版页面结构
                    val playlistName = it.selectFirst("#playlist-top-block h4 a")?.text()
                    children.forEach { child ->
                        val dataHref = child.attr("data-href")
                        val videoCode = dataHref.toVideoCode()
                            .throwIfParseNull("hanimeVideo", "videoCode")
                        val thumbContainer = child.selectFirst(".thumb-container")
                        val coverUrl = thumbContainer?.selectFirst("img.main-thumb")?.absUrl("src")
                            .throwIfParseNull("hanimeVideo", "playlistEachCoverUrl")
                        val title = child.selectFirst("h4.video-title a")?.text()
                            .throwIfParseNull("hanimeVideo", "playlistEachTitle")
                        val duration = thumbContainer?.selectFirst(".duration")?.text()
                        val statItems = thumbContainer?.select(".stat-item")
                        val reviews = statItems?.firstOrNull()?.ownText()?.trim()
                        val views = statItems?.getOrNull(1)?.text()
                        val isPlaying = child.hasClass("videos-scroll")
                        val artist = child.selectFirst(".meta-author a")?.text()
                        val genre = child.selectFirst(".meta-stats a")?.text()
                        val uploadTime = child.selectFirst(".meta-stats span")?.text()
                        playlistVideoList.add(
                            HanimeInfo(
                                title = title, coverUrl = coverUrl,
                                videoCode = videoCode,
                                duration = duration.logIfParseNull(
                                    "hanimeVideo",
                                    "$title duration"
                                ),
                                views = views.logIfParseNull(
                                    "hanimeVideo",
                                    "$title views"
                                ),
                                isPlaying = isPlaying,
                                itemType = HanimeInfo.NORMAL,
                                currentArtist = artist,
                                reviews = reviews,
                                genre = genre,
                                uploadTime = uploadTime
                            )
                        )
                    }
                    HanimeVideo.Playlist(playlistName = playlistName, video = playlistVideoList)
                } else {
                    // 旧版页面结构（兼容兜底）
                    val playlistName = it.selectFirst("div > div > h4")?.text()
                    children.forEach { parent ->
                        if (parent.tagName() == "a") {
                            return@forEach
                        }
                        val videoCode = parent.selectFirst("div > a")?.absUrl("href")?.toVideoCode()
                            .throwIfParseNull("hanimeVideo", "videoCode")
                        val cardMobilePanel = parent.selectFirst("div[class^=card-mobile-panel]")
                        val eachTitleCover = cardMobilePanel?.select("div > div > div > img")?.getOrNull(1)
                        val eachIsPlaying = cardMobilePanel?.select("div > div > div > div")
                            ?.firstOrNull()
                            ?.text()
                            ?.contains("播放") == true
                        val cardMobileDuration = cardMobilePanel?.select("div[class*=card-mobile-duration]")
                        val eachDuration = cardMobileDuration?.firstOrNull()?.text()
                        val eachViews = cardMobileDuration?.getOrNull(2)?.text()
                        val playlistEachCoverUrl = eachTitleCover?.absUrl("src")
                            .throwIfParseNull("hanimeVideo", "playlistEachCoverUrl")
                        val playlistEachTitle = eachTitleCover?.attr("alt")
                            .throwIfParseNull("hanimeVideo", "playlistEachTitle")
                        val artist = cardMobilePanel?.selectFirst("a.card-mobile-user")?.text()
                        val infoBoxes = cardMobilePanel?.select("div.card-mobile-duration.card-playlist-large")
                        val reviews = infoBoxes?.firstOrNull()?.ownText()?.trim()
                        playlistVideoList.add(
                            HanimeInfo(
                                title = playlistEachTitle, coverUrl = playlistEachCoverUrl,
                                videoCode = videoCode,
                                duration = eachDuration.logIfParseNull(
                                    "hanimeVideo",
                                    "$playlistEachTitle duration"
                                ),
                                views = eachViews.logIfParseNull(
                                    "hanimeVideo",
                                    "$playlistEachTitle views"
                                ),
                                isPlaying = eachIsPlaying,
                                itemType = HanimeInfo.NORMAL,
                                currentArtist = artist,
                                reviews = reviews
                            )
                        )
                    }
                    HanimeVideo.Playlist(playlistName = playlistName, video = playlistVideoList)
                }
            } else {
                null
            }
        }

        val relatedAnimeList = mutableListOf<HanimeInfo>()
        val relatedTabContent = parseBody.getElementById("related-tabcontent")

        relatedTabContent?.also {
            val children = it.childOrNull(0)?.children()
            val isSimplified =
                children?.getOrNull(0)?.select("a")?.getOrNull(0)
                    ?.getElementsByClass("home-rows-videos-div")
                    ?.firstOrNull() != null
            if (isSimplified) {
                for (each in children) {
                    val eachContent = each.selectFirst("a")
                    val homeRowsVideosDiv =
                        eachContent?.getElementsByClass("home-rows-videos-div")?.firstOrNull()

                    if (homeRowsVideosDiv != null) {
                        val eachVideoCode = eachContent.absUrl("href").toVideoCode() ?: continue
                        val eachCoverUrl = homeRowsVideosDiv.selectFirst("img")?.absUrl("src")
                            .throwIfParseNull("hanimeVideo", "eachCoverUrl")
                        val eachTitle =
                            homeRowsVideosDiv.selectFirst("div[class$=title]")?.text()
                                .throwIfParseNull("hanimeVideo", "eachTitle")
                        relatedAnimeList.add(
                            HanimeInfo(
                                title = eachTitle, coverUrl = eachCoverUrl,
                                videoCode = eachVideoCode,
                                itemType = HanimeInfo.SIMPLIFIED
                            )
                        )
                    }
                }
            } else {
                relatedAnimeList.addAll(relatedTabContent.extractHanimeInfo())
//                children?.forEachStep2 { each ->
//                    LogUtil.i("children",each.toString())
//                    relatedAnimeList.addAll(each.extractHanimeInfo())
////                    val item = each.select("div[class^=video-item-container]")[0]
////                    hanimeNormalItemVer2(item)?.let(relatedAnimeList::add)
//                }
            }
        }
        LogUtil.d("related_anime_list", relatedAnimeList.toString())

        val hanimeResolution = HanimeResolution()
        val videoClass = parseBody.selectFirst("video[id=player]")
        val videoCoverUrl = videoClass?.absUrl("poster").orEmpty()
        val videos = videoClass?.children()
        if (!videos.isNullOrEmpty()) {
            videos.forEach { source ->
                val resolution = source.attr("size") + "P"
                val sourceUrl = fixAvCdnHost(source.absUrl("src"), isAvSite)
                val videoType = source.attr("type")
                hanimeResolution.parseResolution(resolution, sourceUrl, videoType)
            }
        } else {
            val playerDivWrapper = parseBody.selectFirst("div[id=player-div-wrapper]")
            playerDivWrapper?.select("script")?.let { scripts ->
                for (script in scripts) {
                    val data = script.data()
                    if (data.isBlank()) continue
                    val result =
                        ParserRegex.videoSource.find(data)?.groups?.get(1)?.value ?: continue
                    //hanimeResolution.parseResolution(null, result)
                    hanimeResolution.parseResolution(null, fixAvCdnHost(result, isAvSite))
                    break
                }
            }
        }

        val artistAvatarUrl = parseBody
            .select("div.video-details-wrapper > div > a > div > img[style*='position: absolute'][style*='border-radius: 50%']")
            .attr("src")
        val artistNameCSS = parseBody.getElementById("video-artist-name")
        val artistGenre = artistNameCSS?.nextElementSibling()?.text()?.trim()
        val artistName = artistNameCSS?.text()?.trim()
        val postCSS = parseBody.getElementById("video-subscribe-form")
        val post = postCSS?.let {
            val userId = it.selectFirst("input[name=subscribe-user-id]")?.attr("value")
            val artistId = it.selectFirst("input[name=subscribe-artist-id]")?.attr("value")
            val isSubscribed = it.selectFirst("input[name=subscribe-status]")?.attr("value")
            if (userId != null && artistId != null && isSubscribed != null) {
                HanimeVideo.Artist.POST(
                    userId = userId,
                    artistId = artistId,
                    isSubscribed = isSubscribed == "1"
                )
            } else null
        }
        val artist = if (artistName != null && artistGenre != null) {
            HanimeVideo.Artist(
                name = artistName,
                avatarUrl = artistAvatarUrl,
                genre = artistGenre,
                post = post,
            )
        } else null
        val originalComic = parseBody.selectFirst("a.video-comic-btn")?.attr("href")

        return UiState.Success(
            HanimeVideo(
                title = title, coverUrl = videoCoverUrl,
                chineseTitle = chineseTitle.logIfParseNull(
                    "hanimeVideo",
                    "chineseTitle"
                ),
                uploadTime = uploadTime.logIfParseNull("hanimeVideo", "uploadTime"),
                views = views.logIfParseNull("hanimeVideo", "views"),
                introduction = introduction.logIfParseNull(
                    "hanimeVideo",
                    "introduction"
                ),
                videoUrls = hanimeResolution.toResolutionLinkMap(),
                tags = tagList,
                myList = myList,
                playlist = playlist,
                relatedHanimes = relatedAnimeList,
                artist = artist.logIfParseNull("hanimeVideo", "artist"),
                favTimes = likesCount,
                isFav = likeStatus == "1",
                unlikesCount = unlikesCount,
                isUnlike = unlikeStatus == "1",
                csrfToken = csrfToken,
                currentUserId = currentUserId,
                originalComic = originalComic
            )
        )
    }
}
