package io.github.daisukikaffuchino.han1meviewer.logic

import android.util.Log
import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.Preferences.isAlreadyLogin
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.exception.CloudFlareBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.HanimeNotFoundException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.IPBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import io.github.daisukikaffuchino.han1meviewer.logic.model.CommentPlace
import io.github.daisukikaffuchino.han1meviewer.logic.model.CreatorSort
import io.github.daisukikaffuchino.han1meviewer.logic.model.ModifiedPlaylistArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.MyListType
import io.github.daisukikaffuchino.han1meviewer.logic.model.OnlineWatchHistorySort
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoCommentArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoComments
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeNetwork
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.utils.applicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import javax.net.ssl.SSLHandshakeException

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:38
 */
object NetworkRepo {

    //<editor-fold desc="Hanime">

    fun getHomePage() = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getHomePage(Preferences.homeUrl) },
        action = Parser::homePageVer2
    )

    fun getHanimeSearchResult(
        page: Int, query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<String>, brands: Set<String>,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.hanimeService.getHanimeSearchResult(
                page, query, genre, sort,
                if (broad) "on" else null,
                date, duration, tags, brands
            )
        },
        action = Parser::hanimeSearch
    )

    fun getHanimeVideo(videoCode: String) = videoIOFlow(
        request = { HanimeNetwork.hanimeService.getHanimeVideo(videoCode) },
        action = Parser::hanimeVideoVer2
    )

    fun getHanimePreview(date: String) = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getHanimePreview(date) },
        action = Parser::hanimePreview
    )

    //获取订阅或者可以说是关注列表及它们的更�?
    fun getMySubscriptions(page: Int) = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getMySubscriptions(page) },
        action = Parser::getMySubscriptions
    )
    //</editor-fold>

    //<editor-fold desc="My List">

    fun getMyListItems(userId: String, listType: Any, page: Int) = pageIOFlow(
        request = {
            when (listType) {
                is String ->
                    HanimeNetwork.myListService.getMyListItems(userId, listType, page)

                is MyListType ->
                    HanimeNetwork.myListService.getMyListItems(userId, listType.value, page)

                else ->
                    throw IllegalArgumentException("typeOrId must be String or MyListType")
            }
        },
        action = Parser::myListItems
    )

    fun getMyPlayListItems(page: Int = 1, listCode: String = "0") = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getMyPlayListItems(listCode, page)
        },
        action = Parser::myPlayListItems
    )

    fun getOnlineWatchHistories(
        userId: String,
        sort: OnlineWatchHistorySort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getOnlineWatchHistories(userId, sort.value, page)
        },
        action = Parser::onlineWatchHistoryItems,
    )

    fun getUserAccountPage(userId: String) = websiteIOFlow(
        request = { HanimeNetwork.myListService.getUserAccountPage(userId) },
        action = Parser::userAccountPage,
    )

    fun getUploadedVideos(
        userId: String,
        sort: CreatorSort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getUploadedVideos(userId, sort.value, page)
        },
        action = Parser::creatorUploadedItems,
    )

    fun getUploadingVideos(
        userId: String,
        sort: CreatorSort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getUploadingVideos(userId, sort.value, page)
        },
        action = Parser::creatorUploadingItems,
    )

    fun updateUserAccountProfile(
        userId: String,
        csrfToken: String?,
        name: String,
        email: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.updateUserAccountProfile(
                userId = userId,
                csrfToken = csrfToken,
                name = name,
                email = email,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun updateUserAccountPassword(
        userId: String,
        csrfToken: String?,
        oldPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.updateUserAccountPassword(
                userId = userId,
                csrfToken = csrfToken,
                oldPassword = oldPassword,
                newPassword = newPassword,
                newPasswordConfirm = newPasswordConfirm,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun updateUserAccountAvatar(
        userId: String,
        csrfToken: String?,
        avatarFile: File,
    ) = websiteIOFlow(
        request = {
            val imageRequestBody = avatarFile.asRequestBody("image/jpeg".toMediaType())
            val imagePart = MultipartBody.Part.createFormData(
                "photo",
                avatarFile.name,
                imageRequestBody,
            )
            HanimeNetwork.myListService.updateUserAccountAvatar(
                userId = userId,
                csrfToken = (csrfToken ?: EMPTY_STRING).toRequestBody("text/plain".toMediaType()),
                method = "patch".toRequestBody("text/plain".toMediaType()),
                type = "photo".toRequestBody("text/plain".toMediaType()),
                photo = imagePart,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun deleteOnlineWatchHistory(
        videoCode: String,
        position: Int,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.deleteOnlineWatchHistory(
                videoCode = videoCode,
                csrfToken = csrfToken,
            )
        },
    ) {
        val jsonObject = JSONObject(it)
        val success = jsonObject.optBoolean("success", false)
        if (success) {
            WebsiteState.Success(position)
        } else {
            WebsiteState.Error(IllegalStateException("cannot delete it ?!"))
        }
    }

    fun deleteMyListItems(
        typeOrCode: Any,
        videoCode: String,
        position: Int,
        token: String?,
    ) = websiteIOFlow(
        request = {
            when (typeOrCode) {
                is String ->
                    HanimeNetwork.myListService.deleteMyListItems(
                        typeOrCode, videoCode,
                        csrfToken = token
                    )

                is MyListType ->
                    HanimeNetwork.myListService.deleteMyListItems(
                        typeOrCode.value, videoCode,
                        csrfToken = token
                    )

                else ->
                    throw IllegalArgumentException("typeOrId must be String or MyListType")
            }
        }
    ) { deleteBody ->
        val jsonObject = JSONObject(deleteBody)
        val returnVideoCode = jsonObject.get("video_id").toString()
        if (videoCode == returnVideoCode) {
            return@websiteIOFlow WebsiteState.Success(position)
        }

        return@websiteIOFlow WebsiteState.Error(IllegalStateException("cannot delete it ?!"))
    }

    fun getPlaylists(page: Int, userId: String ) = websiteIOFlow(
        request = { HanimeNetwork.myListService.getPlaylists(userId, page) },
        action = Parser::playlists
    )

    fun addToMyFavVideo(
        videoCode: String,
        likeStatus: Boolean, // false => "": add fav; true => "1": cancel fav;
        currentUserId: String?,
        token: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.addToMyFavVideo(
                videoCode, if (likeStatus) "1" else EMPTY_STRING,
                token, currentUserId
            )
        }
    ) {
        Log.d("add_to_fav_body", it)
        return@websiteIOFlow WebsiteState.Success(likeStatus)
    }

    fun rateVideo(
        videoCode: String,
        isPositive: Boolean,
        likeStatus: Boolean,
        unlikeStatus: Boolean,
        likesCount: Int,
        unlikesCount: Int,
        currentUserId: String?,
        token: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.rateVideo(
                videoCode = videoCode,
                isPositive = if (isPositive) 1 else 0,
                likeStatus = if (likeStatus) "1" else EMPTY_STRING,
                unlikeStatus = if (unlikeStatus) "1" else EMPTY_STRING,
                likesCount = likesCount,
                unlikesCount = unlikesCount,
                csrfToken = token,
                userId = currentUserId,
            )
        }
    ) {
        Log.d("rate_video_body", it)
        return@websiteIOFlow WebsiteState.Success(isPositive)
    }

    fun createPlaylist(
        videoCode: String,
        title: String,
        description: String,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.createPlaylist(
                csrfToken, videoCode, title, description
            )
        },
        permittedSuccessCode = intArrayOf(500)
    ) {
        Log.d("create_playlist_body", it)
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun addToMyList(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        position: Int,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.addToMyList(
                csrfToken, listCode, videoCode, isChecked
            )
        }
    ) {
        Log.d("add_to_playlist_body", it)
        return@websiteIOFlow WebsiteState.Success(position)
    }

    fun modifyPlaylist(
        listCode: String,
        title: String,
        description: String,
        delete: Boolean,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.modifyPlaylist(
                listCode, title, description,
                if (delete) "on" else null,
                csrfToken
            )
        },
        permittedSuccessCode = intArrayOf(302)
    ) {
        Log.d("modify_playlist_body", it)
        return@websiteIOFlow WebsiteState.Success(
            ModifiedPlaylistArgs(
                title = title, desc = description, isDeleted = delete,
            )
        )
    }

    //</editor-fold>

    //<editor-fold desc="Comment">

    fun getComments(type: String, code: String) = websiteIOFlow(
        request = { HanimeNetwork.commentService.getComments(type, code) },
        action = Parser::comments
    )

    fun getCommentReply(commentId: String) = websiteIOFlow(
        request = { HanimeNetwork.commentService.getCommentReply(commentId) },
        action = Parser::commentReply
    )

    fun postComment(
        csrfToken: String?,
        currentUserId: String,
        targetUserId: String,
        type: String,
        text: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.postComment(
                csrfToken, currentUserId,
                type, targetUserId, text
            )
        }
    ) {
        Log.d("post_comment_body", it)
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun postCommentReply(
        csrfToken: String?,
        replyCommentId: String,
        text: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.postCommentReply(
                csrfToken, replyCommentId, text
            )
        }
    ) {
        Log.d("post_comment_reply_body", it)
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun likeComment(
        csrfToken: String?,
        commentPlace: CommentPlace,
        foreignId: String?,
        isPositive: Boolean, // 你選擇的是讚還是踩，1是讚�?是踩
        likeUserId: String?,
        commentLikesCount: Int,
        commentLikesSum: Int,
        likeCommentStatus: Boolean, // 你之前有沒有點過讚，1�?�?
        unlikeCommentStatus: Boolean, // 你之前有沒有點過踩，1�?�?
        commentPosition: Int, comment: VideoComments.VideoComment,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.likeComment(
                csrfToken, commentPlace.value, foreignId,
                if (isPositive) 1 else 0,
                likeUserId, commentLikesCount, commentLikesSum,
                if (likeCommentStatus) 1 else 0,
                if (unlikeCommentStatus) 1 else 0
            )
        }
    ) {
        Log.d("like_comment_body", it)
        return@websiteIOFlow WebsiteState.Success(
            VideoCommentArgs(
                commentPosition, isPositive, comment
            )
        )
    }

    fun reportComment(
        csrfToken: String?,
        reason: String,
        currentUserId: String?,
        redirectUrl: String,
        reportableType: String?,
        reportableId: String?
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.submitReport(
                userId = currentUserId,
                csrfToken = csrfToken,
                redirectUrl = redirectUrl,
                reportableId = reportableId,
                reportableType = reportableType,
                reason = reason
            )
        },
        action = Parser::reportCommentResponse
    )

    //</editor-fold>

    //<editor-fold desc="Subscription">

    fun subscribeArtist(
        csrfToken: String?,
        userId: String,
        artistId: String,
        // 这里表示目标状�?
        status: Boolean,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.subscriptionService.subscribeArtist(
                csrfToken, userId, artistId,
                if (status) "" else "1"
            )
        }
    ) {
        Log.d("subscribe_artist_body", it)
        return@websiteIOFlow WebsiteState.Success(status)
    }

    //</editor-fold>

    //<editor-fold desc="Base">

    fun login(email: String, password: String) = flow {
        emit(WebsiteState.Loading)
        // 首先获取token
        val loginPage = HanimeNetwork.hanimeService.getLoginPage()
        val token = loginPage.body()?.string()?.let(Parser::extractTokenFromLoginPage)
        val req = HanimeNetwork.hanimeService.login(token, email, password)
        if (req.isSuccessful) {
            // 再次获取登录页面，如果失败则返回 cookie
            // 因为登录成功再次访问 login �?404，这是判断是否登录成功的方法
            val loginPageAgain = HanimeNetwork.hanimeService.getLoginPage()
            if (loginPageAgain.code() == 404) {
                // Cookie 會返�?XSRF-TOKEN �?hanime1_session，我們只需要後�?
                // 错误的，还需�?remember_web 字段！但我没找到�?
                Log.d("login_headers", req.headers().toMultimap().toString())
                emit(WebsiteState.Success(req.headers().values("Set-Cookie")))
            } else {
                emit(WebsiteState.Error(IllegalStateException(getString(R.string.account_or_password_wrong))))
            }
        } else {
            // 雙重保險
            emit(WebsiteState.Error(IllegalStateException(getString(R.string.account_or_password_wrong))))
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于单网页的情况
     *
     * @param permittedSuccessCode 用于处理特殊情况，比如[NetworkRepo.modifyPlaylist]需�?02成功
     */
    private fun <T> websiteIOFlow(
        request: suspend () -> Response<ResponseBody>,
        permittedSuccessCode: IntArray? = null,
        action: (String) -> WebsiteState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = requestResult.body()?.string()
        val permitted = permittedSuccessCode?.contains(requestResult.code()) == true
        if ((permitted || requestResult.isSuccessful)) {
            emit(action.invoke(resultBody ?: EMPTY_STRING))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于有page分页的情�?
     */
    private fun <T> pageIOFlow(
        request: suspend () -> Response<ResponseBody>,
        action: (String) -> PageLoadingState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = requestResult.body()?.string()
        if (requestResult.isSuccessful && resultBody != null) {
            emit(action.invoke(resultBody))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(PageLoadingState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于影片界面
     */
    private fun <T> videoIOFlow(
        request: suspend () -> Response<ResponseBody>,
        action: (String) -> VideoLoadingState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = requestResult.body()?.string()
        if (requestResult.isSuccessful && resultBody != null) {
            emit(action.invoke(resultBody))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(VideoLoadingState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    internal fun Response<ResponseBody>.throwRequestException(): Nothing {
        val body = errorBody()?.string()
        when (val code = code()) {
            403 -> if (!body.isNullOrBlank()) {
                when {
                    "you have been blocked" in body ->
                        throw IPBlockedException(getString(R.string.cloudflare_ip_block_warning))

                    "Just a moment" in body ->
                        throw CloudFlareBlockedException(getString(R.string.cloudflare_network_mismatch))

                    else ->
                        throw HanimeNotFoundException(getString(R.string.video_might_not_exist)) // 主要出現在影片界面，當你v數不大時會報403
                }
            } else throw IllegalStateException("$code ${message()}")

            500 -> throw HanimeNotFoundException(getString(R.string.video_might_not_exist)) // 主要出現在影片界面，當你v數很大時會報500

            404 -> if (!isAlreadyLogin) {
                throw IllegalStateException(getString(R.string.not_logged_in_currently))
            } else {
                throw IllegalStateException("$code ${message()}")
            }

            else -> throw IllegalStateException("$code ${message()}")
        }
    }

    internal fun handleException(e: Throwable): Throwable {
        return when (e) {
            is CancellationException -> throw e
            is ParseException -> {
                e.printStackTrace()
                ParseException(getString(R.string.parse_error_msg))
            }

            is SSLHandshakeException -> {
                e.printStackTrace()
                SSLHandshakeException(getString(R.string.ssl_handshake_error))
            }

            else -> {
                e.printStackTrace()
                e
            }
        }
    }

    //</editor-fold>

    private fun getString(resId: Int) = applicationContext.getString(resId)
}
