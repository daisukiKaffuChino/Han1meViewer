package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository.isAlreadyLogin
import io.github.daisukikaffuchino.han1meviewer.logic.exception.LoginStateExpiredException
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HomePage
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.toVideoCode
import io.github.daisukikaffuchino.utils.LogUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment

/**
 * 首页解析。
 */
internal object HanimeHomeParser {

    fun parse(body: String): UiState<HomePage> =
        parseHomePageBody(body, isAvSite = isAvSiteFromSettings, isLoggedIn = isAlreadyLogin)

    fun parseHomePageBody(
        body: String,
        isAvSite: Boolean,
        isLoggedIn: Boolean,
    ): UiState<HomePage> {
        val parseBody = Jsoup.parse(body).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value") // csrf token
        val homePageParse = parseBody.select("div[id=home-rows-wrapper] > div")

        // 用户信息
        val userInfo = parseBody.selectFirst("div[id=user-modal-dp-wrapper]")
        val avatarUrl: String? = userInfo?.selectFirst("img")?.absUrl("src")
        val username: String? = userInfo?.getElementById("user-modal-name")?.text()
        val userHomePageLink = parseBody.getElementById("user-modal-trigger")?.attr("href")?:""

        if (isLoggedIn && isLoginStateExpired(userHomePageLink, username)) {
            return UiState.Error(LoginStateExpiredException())
        }

        val userIdRegex = Regex("""/user/(\d+)""")
        val userId: String = userIdRegex.find(userHomePageLink)?.groupValues?.get(1) ?: ""
        LogUtil.i("userInfo","name:$username;id:$userId")

        // 头图及其描述
        val bannerCSS = parseBody.selectFirst("div[id=home-banner-wrapper]")
        val bannerImg = bannerCSS?.previousElementSibling()
        val bannerTitle = bannerImg?.selectFirst("img")?.attr("alt")
            .logIfParseNull("homePageVer2", "bannerTitle")
        val bannerPic = bannerImg?.select("img")?.let { imgList ->
            imgList.getOrNull(1)?.absUrl("src") ?: imgList.getOrNull(0)?.absUrl("src")
        }?.logIfParseNull("homePageVer2", "bannerPic")
        val bannerDesc = bannerCSS?.selectFirst("h4")?.ownText()
        val bannerVideoCodeScript = parseBody.select("script")
            .firstOrNull{ it.data().contains("watch?v=")}
            ?.data()
        val regex = Regex("""watch\?v=(\d+)""")
        var bannerVideoCode = bannerVideoCodeScript?.let { script ->
            regex.find(script)?.groupValues?.get(1)
        }
        // 目前先判断注释里的，以后可能会有变化
        if (bannerVideoCode == null) {
            bannerCSS?.traverse { node, _ ->
                if (node is Comment) {
                    node.data.toVideoCode()?.let {
                        bannerVideoCode = it
                        return@traverse
                    }
                }
            }
        }
        bannerVideoCode.logIfParseNull("homePageVer2", "bannerVideoCode")
        val banner = if (bannerTitle != null && bannerPic != null) {
            HomePage.Banner(
                title = bannerTitle, description = bannerDesc,
                picUrl = bannerPic, videoCode = bannerVideoCode,
            )
        } else null

        // 主页模块
        val latestReleaseClass = homePageParse.getOrNull(0) // 最新上市
        val latestUploadClass = homePageParse.getOrNull(1)  //最新上传
        val ecchiAnimeClass = homePageParse.getOrNull(2)  //里番
        val shortEpisodeAnimeClass = homePageParse.getOrNull(3)  // 泡面番
        val motionAnimeClass = homePageParse.getOrNull(5)  // Motion Anime
        val threeDCGClass = homePageParse.getOrNull(6)  //3DCG
        val twoPointFiveDAnimeClass = homePageParse.getOrNull(7)  // 2.5D
        val twoDAnimeClass = homePageParse.getOrNull(8)  // 2D
        val aiGeneratedClass = homePageParse.getOrNull(10)  // AI生成
        val mmdClass = homePageParse.getOrNull(11)  //  MMD
        val cosplayClass = homePageParse.getOrNull(12)  // Cosplay
        val watchingNowClass = homePageParse.getOrNull(13)  // 他们在看

        val newAnimeTrailerClass = homePageParse.getOrNull(if (isAvSite) 13 else 12)

        val latestReleaseList = latestReleaseClass.extractHanimeInfo()
        val latestHanimeList = mutableListOf<HanimeInfo>()
        if (isAvSite) {
            latestHanimeList.addAll(latestUploadClass.extractHanimeInfo())
        } else {
            latestHanimeList.addAll(latestUploadClass.extractHanimeInfo())
        }
        val ecchiAnimeList = ecchiAnimeClass.extractHanimeInfo()
        val shortEpisodeAnimeList = shortEpisodeAnimeClass.extractHanimeInfo()
        val motionAnimeList = motionAnimeClass.extractHanimeInfo()
        val threeDCGList = threeDCGClass.extractHanimeInfo()
        val twoPointFiveDAnimeList = twoPointFiveDAnimeClass.extractHanimeInfo()
        val twoDAnimeList = mutableListOf<HanimeInfo>()
        if (isAvSite) {
            twoDAnimeList.addAll(twoDAnimeClass.extractHanimeInfo())
        } else {
            twoDAnimeList.addAll(twoDAnimeClass.extractHanimeInfo())
        }

        val aiGeneratedList = aiGeneratedClass.extractHanimeInfo()
        val mmdList = mmdClass.extractHanimeInfo()
        val cosplayList = cosplayClass.extractHanimeInfo()
        val watchingNowList = watchingNowClass.extractHanimeInfo()

        val newAnimeTrailerList = mutableListOf<HanimeInfo>()
        if (isAvSite) {
            newAnimeTrailerList.addAll(newAnimeTrailerClass.extractHanimeInfo())
        } else {
            val newAnimeTrailerItems =
                newAnimeTrailerClass?.select("a")
            newAnimeTrailerItems?.forEach { newAnimeTrailerItem ->
                val videoCode = newAnimeTrailerItem.attr("href").toVideoCode()

                val coverUrl = newAnimeTrailerItem.selectFirst("img")?.attr("src")
                val title = newAnimeTrailerItem.selectFirst("div.home-rows-videos-title")?.text()
                if (title == null || coverUrl == null || videoCode == null) return@forEach
                newAnimeTrailerList.add(
                    HanimeInfo(
                        title = title,
                        coverUrl = coverUrl,
                        videoCode = videoCode,
                        duration = "",
                        currentArtist = null,
                        views = null,
                        uploadTime = null,
                        genre = null,
                        itemType = HanimeInfo.SIMPLIFIED
                    )
                )
            }
        }

        // emit!
        return UiState.Success(
            HomePage(
                csrfToken,
                avatarUrl, username, banner = banner,
                latestHanime = latestHanimeList,
                latestRelease = latestReleaseList,
                ecchiAnime = ecchiAnimeList,
                shortEpisodeAnime = shortEpisodeAnimeList,
                twoPointFiveDAnime = twoPointFiveDAnimeList,
                threeDCG = threeDCGList,
                motionAnime = motionAnimeList,
                twoDAnime = twoDAnimeList,
                aiGenerated = aiGeneratedList,
                mmd = mmdList,
                cosplay = cosplayList,
                watchingNow = watchingNowList,
                newAnimeTrailer = newAnimeTrailerList,
                userId = userId
            )
        )
    }

    private fun isLoginStateExpired(userHomePageLink: String, username: String?): Boolean {
        return userHomePageLink.contains("/login") || username.isNullOrBlank()
    }
}
