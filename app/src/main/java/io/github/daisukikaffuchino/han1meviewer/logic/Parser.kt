package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimePreview
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeSearchResult
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HomePage
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListItems
import io.github.daisukikaffuchino.han1meviewer.logic.model.MySubscriptions
import io.github.daisukikaffuchino.han1meviewer.logic.model.Playlists
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccount
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoComments
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeCommentParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeHomeParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeListParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeLoginParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimePlaylistParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimePreviewParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeSearchParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeSubscriptionParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeUserParser
import io.github.daisukikaffuchino.han1meviewer.logic.parser.HanimeVideoParser
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/07/31 031 16:43
 */
object Parser {

    fun extractTokenFromLoginPage(body: String): String =
        HanimeLoginParser.extractTokenFromLoginPage(body)

    fun homePageVer2(body: String): UiState<HomePage> = HanimeHomeParser.parse(body)

    internal fun parseHomePageBody(
        body: String,
        isAvSite: Boolean,
        isLoggedIn: Boolean,
    ): UiState<HomePage> = HanimeHomeParser.parseHomePageBody(body, isAvSite, isLoggedIn)

    fun hanimeSearch(body: String): PagedUiState<HanimeSearchResult> = HanimeSearchParser.parse(body)

    fun hanimeVideo(body: String): UiState<HanimeVideo> = HanimeVideoParser.parse(body)

    internal fun parseHanimeVideoBody(body: String, isAvSite: Boolean): UiState<HanimeVideo> =
        HanimeVideoParser.parseHanimeVideoBody(body, isAvSite)

    fun hanimePreview(body: String): UiState<HanimePreview> = HanimePreviewParser.parse(body)

    fun myListItems(body: String): PagedUiState<MyListItems<HanimeInfo>> =
        HanimeListParser.myListItems(body)

    fun myPlayListItems(body: String): PagedUiState<MyListItems<HanimeInfo>> =
        HanimeListParser.myPlayListItems(body)

    fun onlineWatchHistoryItems(body: String): PagedUiState<MyListItems<HanimeInfo>> =
        HanimeListParser.onlineWatchHistoryItems(body)

    fun userAccountPage(body: String): UiState<UserAccount> = HanimeUserParser.userAccountPage(body)

    fun playlists(body: String): UiState<Playlists> = HanimePlaylistParser.playlists(body)

    fun comments(body: String): UiState<VideoComments> = HanimeCommentParser.comments(body)

    fun commentReply(body: String): UiState<VideoComments> = HanimeCommentParser.commentReply(body)

    fun reportCommentResponse(body: String): UiState<String> =
        HanimeCommentParser.reportCommentResponse(body)

    fun getMySubscriptions(body: String): UiState<MySubscriptions> =
        HanimeSubscriptionParser.getMySubscriptions(body)
}
