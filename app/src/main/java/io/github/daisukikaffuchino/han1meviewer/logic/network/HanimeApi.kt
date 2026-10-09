package io.github.daisukikaffuchino.han1meviewer.logic.network

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

/**
 * 站点 HTTP 接口的参数拼装层。
 *
 * 只负责「请求哪个端点、参数如何拼」，把站点怪癖（`on` / `1` / 空串等魔法值）集中在这里；
 * 不解析响应、不做错误映射、不包装 UiState。
 */
internal object HanimeApi {

    //<editor-fold desc="Hanime">

    suspend fun getHomePage(): Response<ResponseBody> =
        HanimeNetwork.hanimeService.getHomePage(SettingsRepository.homeUrl)

    suspend fun getHanimeSearchResult(
        page: Int, query: String?, genre: String?, sort: String?, broad: Boolean,
        date: String?, duration: String?, tags: Set<String>, brands: Set<String>,
    ): Response<ResponseBody> = HanimeNetwork.hanimeService.getHanimeSearchResult(
        page, query, genre, sort,
        if (broad) "on" else null,
        date, duration, tags, brands
    )

    suspend fun getHanimeVideo(videoCode: String): Response<ResponseBody> =
        HanimeNetwork.hanimeService.getHanimeVideo(videoCode)

    suspend fun getHanimePreview(date: String): Response<ResponseBody> =
        HanimeNetwork.hanimeService.getHanimePreview(date)

    //获取订阅或者可以说是关注列表及它们的更新
    suspend fun getMySubscriptions(page: Int): Response<ResponseBody> =
        HanimeNetwork.hanimeService.getMySubscriptions(page)

    suspend fun getLoginPage(): Response<ResponseBody> = HanimeNetwork.hanimeService.getLoginPage()

    suspend fun login(token: String?, email: String, password: String): Response<ResponseBody> =
        HanimeNetwork.hanimeService.login(token, email, password)

    //</editor-fold>

    //<editor-fold desc="My List">

    suspend fun getMyListItems(
        userId: String,
        listType: String,
        page: Int,
    ): Response<ResponseBody> = HanimeNetwork.myListService.getMyListItems(userId, listType, page)

    suspend fun getMyPlayListItems(listCode: String, page: Int): Response<ResponseBody> =
        HanimeNetwork.myListService.getMyPlayListItems(listCode, page)

    suspend fun getOnlineWatchHistories(
        userId: String,
        sort: String,
        page: Int,
    ): Response<ResponseBody> = HanimeNetwork.myListService.getOnlineWatchHistories(userId, sort, page)

    suspend fun getUserAccountPage(userId: String): Response<ResponseBody> =
        HanimeNetwork.myListService.getUserAccountPage(userId)

    suspend fun updateUserAccountProfile(
        userId: String,
        csrfToken: String?,
        name: String,
        email: String,
    ): Response<ResponseBody> = HanimeNetwork.myListService.updateUserAccountProfile(
        userId = userId,
        csrfToken = csrfToken,
        name = name,
        email = email,
    )

    suspend fun updateUserAccountPassword(
        userId: String,
        csrfToken: String?,
        oldPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
    ): Response<ResponseBody> = HanimeNetwork.myListService.updateUserAccountPassword(
        userId = userId,
        csrfToken = csrfToken,
        oldPassword = oldPassword,
        newPassword = newPassword,
        newPasswordConfirm = newPasswordConfirm,
    )

    suspend fun updateUserAccountAvatar(
        userId: String,
        csrfToken: String?,
        avatarFile: File,
    ): Response<ResponseBody> {
        val imageRequestBody = avatarFile.asRequestBody("image/jpeg".toMediaType())
        val imagePart = MultipartBody.Part.createFormData(
            "photo",
            avatarFile.name,
            imageRequestBody,
        )
        return HanimeNetwork.myListService.updateUserAccountAvatar(
            userId = userId,
            csrfToken = (csrfToken ?: EMPTY_STRING).toRequestBody("text/plain".toMediaType()),
            method = "patch".toRequestBody("text/plain".toMediaType()),
            type = "photo".toRequestBody("text/plain".toMediaType()),
            photo = imagePart,
        )
    }

    suspend fun deleteOnlineWatchHistory(
        videoCode: String,
        csrfToken: String?,
    ): Response<ResponseBody> = HanimeNetwork.myListService.deleteOnlineWatchHistory(
        videoCode = videoCode,
        csrfToken = csrfToken,
    )

    suspend fun deleteMyListItems(
        listType: String,
        videoCode: String,
        csrfToken: String?,
    ): Response<ResponseBody> = HanimeNetwork.myListService.deleteMyListItems(
        listType, videoCode,
        csrfToken = csrfToken
    )

    suspend fun getPlaylists(userId: String, page: Int): Response<ResponseBody> =
        HanimeNetwork.myListService.getPlaylists(userId, page)

    suspend fun addToMyFavVideo(
        videoCode: String,
        likeStatus: Boolean, // false => "": add fav; true => "1": cancel fav;
        csrfToken: String?,
        currentUserId: String?,
    ): Response<ResponseBody> = HanimeNetwork.myListService.addToMyFavVideo(
        videoCode, if (likeStatus) "1" else EMPTY_STRING,
        csrfToken, currentUserId
    )

    suspend fun rateVideo(
        videoCode: String,
        isPositive: Boolean,
        likeStatus: Boolean,
        unlikeStatus: Boolean,
        likesCount: Int,
        unlikesCount: Int,
        csrfToken: String?,
        userId: String?,
    ): Response<ResponseBody> = HanimeNetwork.myListService.rateVideo(
        videoCode = videoCode,
        isPositive = if (isPositive) 1 else 0,
        likeStatus = if (likeStatus) "1" else EMPTY_STRING,
        unlikeStatus = if (unlikeStatus) "1" else EMPTY_STRING,
        likesCount = likesCount,
        unlikesCount = unlikesCount,
        csrfToken = csrfToken,
        userId = userId,
    )

    suspend fun createPlaylist(
        csrfToken: String?,
        videoCode: String,
        title: String,
        description: String,
    ): Response<ResponseBody> = HanimeNetwork.myListService.createPlaylist(
        csrfToken, videoCode, title, description
    )

    suspend fun addToMyList(
        csrfToken: String?,
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
    ): Response<ResponseBody> = HanimeNetwork.myListService.addToMyList(
        csrfToken, listCode, videoCode, isChecked
    )

    suspend fun modifyPlaylist(
        listCode: String,
        title: String,
        description: String,
        delete: Boolean,
        csrfToken: String?,
    ): Response<ResponseBody> = HanimeNetwork.myListService.modifyPlaylist(
        listCode, title, description,
        if (delete) "on" else null,
        csrfToken
    )

    //</editor-fold>

    //<editor-fold desc="Comment">

    suspend fun getComments(type: String, code: String): Response<ResponseBody> =
        HanimeNetwork.commentService.getComments(type, code)

    suspend fun getCommentReply(commentId: String): Response<ResponseBody> =
        HanimeNetwork.commentService.getCommentReply(commentId)

    suspend fun postComment(
        csrfToken: String?,
        currentUserId: String,
        targetUserId: String,
        type: String,
        text: String,
    ): Response<ResponseBody> = HanimeNetwork.commentService.postComment(
        csrfToken, currentUserId,
        type, targetUserId, text
    )

    suspend fun postCommentReply(
        csrfToken: String?,
        replyCommentId: String,
        text: String,
    ): Response<ResponseBody> = HanimeNetwork.commentService.postCommentReply(
        csrfToken, replyCommentId, text
    )

    suspend fun likeComment(
        csrfToken: String?,
        commentPlace: String,
        foreignId: String?,
        isPositive: Boolean, // 你選擇的是讚還是踩，1是讚，0是踩
        likeUserId: String?,
        commentLikesCount: Int,
        commentLikesSum: Int,
        likeCommentStatus: Boolean, // 你之前有沒有點過讚，1是0否
        unlikeCommentStatus: Boolean, // 你之前有沒有點過踩，1是0否
    ): Response<ResponseBody> = HanimeNetwork.commentService.likeComment(
        csrfToken, commentPlace, foreignId,
        if (isPositive) 1 else 0,
        likeUserId, commentLikesCount, commentLikesSum,
        if (likeCommentStatus) 1 else 0,
        if (unlikeCommentStatus) 1 else 0
    )

    suspend fun submitReport(
        userId: String?,
        csrfToken: String?,
        redirectUrl: String,
        reportableId: String?,
        reportableType: String?,
        reason: String,
    ): Response<ResponseBody> = HanimeNetwork.commentService.submitReport(
        userId = userId,
        csrfToken = csrfToken,
        redirectUrl = redirectUrl,
        reportableId = reportableId,
        reportableType = reportableType,
        reason = reason
    )

    //</editor-fold>

    //<editor-fold desc="Subscription">

    suspend fun subscribeArtist(
        csrfToken: String?,
        userId: String,
        artistId: String,
        // 这里表示目标状态
        status: Boolean,
    ): Response<ResponseBody> = HanimeNetwork.subscriptionService.subscribeArtist(
        csrfToken, userId, artistId,
        if (status) "" else "1"
    )

    //</editor-fold>
}
