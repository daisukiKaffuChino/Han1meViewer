package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.model.CommentPlace
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoCommentArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoComments
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.websiteIOFlow
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil

/**
 * 评论 / 回复 / 点赞 / 检举。
 */
object CommentRepository {

    fun getComments(type: String, code: String) = websiteIOFlow(
        request = { HanimeApi.getComments(type, code) },
        action = Parser::comments
    )

    fun getCommentReply(commentId: String) = websiteIOFlow(
        request = { HanimeApi.getCommentReply(commentId) },
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
            HanimeApi.postComment(
                csrfToken, currentUserId,
                targetUserId, type, text
            )
        }
    ) {
        LogUtil.d("post_comment_body", it)
        return@websiteIOFlow UiState.Success(Unit)
    }

    fun postCommentReply(
        csrfToken: String?,
        replyCommentId: String,
        text: String,
    ) = websiteIOFlow(
        request = {
            HanimeApi.postCommentReply(
                csrfToken, replyCommentId, text
            )
        }
    ) {
        LogUtil.d("post_comment_reply_body", it)
        return@websiteIOFlow UiState.Success(Unit)
    }

    fun likeComment(
        csrfToken: String?,
        commentPlace: CommentPlace,
        foreignId: String?,
        isPositive: Boolean, // 你選擇的是讚還是踩，1是讚，0是踩
        likeUserId: String?,
        commentLikesCount: Int,
        commentLikesSum: Int,
        likeCommentStatus: Boolean, // 你之前有沒有點過讚，1是0否
        unlikeCommentStatus: Boolean, // 你之前有沒有點過踩，1是0否
        commentPosition: Int, comment: VideoComments.VideoComment,
    ) = websiteIOFlow(
        request = {
            HanimeApi.likeComment(
                csrfToken, commentPlace.value, foreignId,
                isPositive,
                likeUserId, commentLikesCount, commentLikesSum,
                likeCommentStatus,
                unlikeCommentStatus
            )
        }
    ) {
        LogUtil.d("like_comment_body", it)
        return@websiteIOFlow UiState.Success(
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
            HanimeApi.submitReport(
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
}
