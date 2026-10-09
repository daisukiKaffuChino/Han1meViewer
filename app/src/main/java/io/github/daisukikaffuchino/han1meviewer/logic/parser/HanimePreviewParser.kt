package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimePreview
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 预告 / 预览页解析。
 */
internal object HanimePreviewParser {

    fun parse(body: String): UiState<HanimePreview> {
        val parseBody = Jsoup.parse(body).body()

        // latest hanime
        val latestHanimeList = mutableListOf<HanimeInfo>()
        val latestHanimeClass = parseBody.selectFirst("div[class$=owl-theme]")
        latestHanimeClass?.let {
            val latestHanimeItems = latestHanimeClass.select("div[class=home-rows-videos-div]")
            latestHanimeItems.forEach { latestHanimeItem ->
                val coverUrl = latestHanimeItem.selectFirst("img")?.absUrl("src")
                    .throwIfParseNull("hanimePreview", "coverUrl")
                val title = latestHanimeItem.selectFirst("div[class$=title]")?.text()
                    .throwIfParseNull("hanimePreview", "title")
                latestHanimeList.add(
                    HanimeInfo(
                        coverUrl = coverUrl,
                        title = title,
                        videoCode = EMPTY_STRING /* empty string here! */,
                        itemType = HanimeInfo.SIMPLIFIED
                    )
                )
            }
        }

        val contentPaddingClass = parseBody.select("div[class=content-padding] > div")
        val previewInfo = mutableListOf<HanimePreview.PreviewInfo>()
        for (i in 0 until contentPaddingClass.size / 2) {

            val firstPart = contentPaddingClass.getOrNull(i * 2)
            val secondPart = contentPaddingClass.getOrNull(i * 2 + 1)

            val videoCode = firstPart?.id()
            val title = firstPart?.selectFirst("h4")?.text()
            val coverUrl =
                firstPart?.selectFirst("div[class=preview-info-cover] > img")?.absUrl("src")
            val previewInfoContentClass =
                firstPart?.getElementsByClass("preview-info-content-padding")?.firstOrNull()
            val videoTitle = previewInfoContentClass?.selectFirst("h4")?.text()
            val brand = previewInfoContentClass?.selectFirst("h5")?.selectFirst("a")?.text()
            val releaseDate = previewInfoContentClass?.select("h5")?.getOrNull(1)?.ownText()

            val introduction = secondPart?.selectFirst("h5")?.text()
            val tagClass = secondPart?.select("div[class=single-video-tag] > a")
            val tags = mutableListOf<String>()
            tagClass?.forEach { tag: Element? ->
                tag?.let { tags.add(tag.text()) }
            }
            val relatedPicClass = secondPart?.select("img[class=preview-image-modal-trigger]")
            val relatedPics = mutableListOf<String>()
            relatedPicClass?.forEach { relatedPic: Element? ->
                relatedPic?.let { relatedPics.add(relatedPic.absUrl("src")) }
            }

            previewInfo.add(
                HanimePreview.PreviewInfo(
                    title = title,
                    videoTitle = videoTitle,
                    coverUrl = coverUrl,
                    introduction = introduction.logIfParseNull(
                        "hanimePreview",
                        "$title introduction"
                    ),
                    brand = brand.logIfParseNull("hanimePreview", "$title brand"),
                    releaseDate = releaseDate.logIfParseNull(
                        "hanimePreview",
                        "$title releaseDate"
                    ),
                    videoCode = videoCode.logIfParseNull(
                        "hanimePreview",
                        "$title videoCode"
                    ),
                    tags = tags,
                    relatedPicsUrl = relatedPics
                )
            )
        }

        val header = parseBody.selectFirst("div[id=player-div-wrapper]")
        val headerPicUrl = header?.selectFirst("img")?.absUrl("src")
        val hasPrevious = parseBody.getElementsByClass("hidden-md hidden-lg").firstOrNull()
            ?.select("div[style*=left]")?.firstOrNull() != null
        val hasNext = parseBody.getElementsByClass("hidden-md hidden-lg").firstOrNull()
            ?.select("div[style*=right]")?.firstOrNull() != null

        return UiState.Success(
            HanimePreview(
                headerPicUrl = headerPicUrl.logIfParseNull(
                    "hanimePreview",
                    "headerPicUrl"
                ),
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                latestHanime = latestHanimeList,
                previewInfo = previewInfo
            )
        )
    }
}
