package io.github.daisukikaffuchino.han1meviewer.logic.parser

import android.annotation.SuppressLint
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoComments
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 评论 / 回复 / 检举回应解析。
 */
internal object HanimeCommentParser {

    @SuppressLint("BuildListAdds")
    fun comments(body: String): UiState<VideoComments> {
        val jsonObject = JSONObject(body)
        val commentBody = jsonObject.get("comments").toString()
        val parseBody = Jsoup.parse(commentBody).body()
        val csrfToken = parseBody.selectFirst("input[name=_token]")?.attr("value")
        val currentUserId = parseBody.selectFirst("input[name=comment-user-id]")?.attr("value")
        val commentList = mutableListOf<VideoComments.VideoComment>()
        val allCommentsClass = parseBody.getElementById("comment-start")

        buildList {
            allCommentsClass?.children()?.chunked(4)?.forEach { elements ->
                add(Element("div").apply { appendChildren(elements) })
            }
        }.forEach { child: Element ->
            val avatarUrl = child.selectFirst("img")?.absUrl("src")
                .throwIfParseNull("comments", "avatarUrl")
            val textClass = child.getElementsByClass("comment-index-text")
            val nameAndDateClass = textClass.firstOrNull()
            val username = nameAndDateClass?.selectFirst("a")?.ownText()?.trim()
                .throwIfParseNull("comments", "username")
            val date = nameAndDateClass?.selectFirst("span")?.ownText()?.trim()
                .throwIfParseNull("comments", "date")
            val content = textClass.getOrNull(1)?.text()
                .throwIfParseNull("comments", "content")
            val hasMoreReplies = child.selectFirst("div[class^=load-replies-btn]") != null
            val thumbUp = child.getElementById("comment-like-form-wrapper")
                ?.select("span[style]")?.getOrNull(1)
                ?.text()?.toIntOrNull()
            val id = child.selectFirst("div[id^=reply-section-wrapper]")
                ?.id()?.substringAfterLast("-")

            val foreignId = child.getElementById("foreign_id")?.attr("value")
            val isPositive = child.getElementById("is_positive")?.attr("value")
            val likeUserId = child.selectFirst("input[name=comment-like-user-id]")?.attr("value")
            val commentLikesCount =
                child.selectFirst("input[name=comment-likes-count]")?.attr("value")
            val commentLikesSum = child.selectFirst("input[name=comment-likes-sum]")?.attr("value")
            val likeCommentStatus =
                child.selectFirst("input[name=like-comment-status]")?.attr("value")
            val unlikeCommentStatus =
                child.selectFirst("input[name=unlike-comment-status]")?.attr("value")

            val post = VideoComments.VideoComment.POST(
                foreignId.logIfParseNull("comments", "foreignId", loginNeeded = true),
                isPositive == "1",
                likeUserId.logIfParseNull("comments", "likeUserId", loginNeeded = true),
                commentLikesCount?.toIntOrNull().logIfParseNull(
                    "comments",
                    "commentLikesCount", loginNeeded = true
                ),
                commentLikesSum?.toIntOrNull().logIfParseNull(
                    "comments",
                    "commentLikesSum", loginNeeded = true
                ),
                likeCommentStatus == "1",
                unlikeCommentStatus == "1",
            )
            val regex = """\d+""".toRegex()
            val replyCountText = child.select("div.load-replies-btn").text()
            val replyCount = regex.find(replyCountText)?.value?.toInt()
            val reportRedirectUrl = ""
            val reportableId = child.select("span.report-btn").first()?.attr("data-reportable-id")
            val reportableType = child.select("span.report-btn").first()?.attr("data-reportable-type")

            commentList.add(
                VideoComments.VideoComment(
                    avatar = avatarUrl, username = username, date = date,
                    content = content, hasMoreReplies = hasMoreReplies, replyCount = replyCount,
                    thumbUp = thumbUp.logIfParseNull("comments", "thumbUp"),
                    id = id.logIfParseNull("comments", "id"),
                    isChildComment = false, post = post,
                    redirectUrl = reportRedirectUrl, reportableId = reportableId, reportableType = reportableType
                )
            )
        }
        LogUtil.d("commentList", commentList.toString())
        return UiState.Success(
            VideoComments(
                commentList,
                currentUserId,
                csrfToken
            )
        )
    }

    fun commentReply(body: String): UiState<VideoComments> {
        val jsonObject = JSONObject(body)
        val replyBody = jsonObject.get("replies").toString()
        val replyList = mutableListOf<VideoComments.VideoComment>()
        val parseBody = Jsoup.parse(replyBody).body()
        val replyStart = parseBody.selectFirst("div[id^=reply-start]")
        replyStart?.let {
            val allRepliesClass = it.children()
            for (i in allRepliesClass.indices step 2) {
                val basicClass = allRepliesClass.getOrNull(i)
                val postClass = allRepliesClass.getOrNull(i + 1)

                val avatarUrl = basicClass?.selectFirst("img")?.absUrl("src")
                    .throwIfParseNull("commentReply", "avatarUrl")
                val textClass = basicClass?.getElementsByClass("comment-index-text")
                val nameAndDateClass = textClass?.firstOrNull()
                val username = nameAndDateClass?.selectFirst("a")?.ownText()?.trim()
                    .throwIfParseNull("commentReply", "name")
                val date = nameAndDateClass?.selectFirst("span")?.ownText()?.trim()
                    .throwIfParseNull("commentReply", "date")
                val content = textClass?.getOrNull(1)?.text()
                    .throwIfParseNull("commentReply", "content")
                val thumbUp = postClass
                    ?.select("span[style]")?.getOrNull(1)
                    ?.text()?.toIntOrNull()

                val foreignId =
                    postClass?.getElementById("foreign_id")?.attr("value")
                val isPositive =
                    postClass?.getElementById("is_positive")?.attr("value")
                val likeUserId =
                    postClass?.selectFirst("input[name=comment-like-user-id]")?.attr("value")
                val commentLikesCount =
                    postClass?.selectFirst("input[name=comment-likes-count]")?.attr("value")
                val commentLikesSum =
                    postClass?.selectFirst("input[name=comment-likes-sum]")?.attr("value")
                val likeCommentStatus =
                    postClass?.selectFirst("input[name=like-comment-status]")?.attr("value")
                val unlikeCommentStatus =
                    postClass?.selectFirst("input[name=unlike-comment-status]")?.attr("value")
                val post = VideoComments.VideoComment.POST(
                    foreignId.logIfParseNull(
                        "commentReply",
                        "foreignId",
                        loginNeeded = true
                    ),
                    isPositive == "1",
                    likeUserId.logIfParseNull(
                        "commentReply",
                        "likeUserId",
                        loginNeeded = true
                    ),
                    commentLikesCount?.toIntOrNull().logIfParseNull(
                        "commentReply",
                        "commentLikesCount", loginNeeded = true
                    ),
                    commentLikesSum?.toIntOrNull().logIfParseNull(
                        "commentReply",
                        "commentLikesSum", loginNeeded = true
                    ),
                    likeCommentStatus == "1",
                    unlikeCommentStatus == "1",
                )
                val reportRedirectUrl = ""
                val reportableId = basicClass?.select("span.report-btn")?.first()?.attr("data-reportable-id")
                val reportableType = basicClass?.select("span.report-btn")?.first()?.attr("data-reportable-type")
                replyList.add(
                    VideoComments.VideoComment(
                        avatar = avatarUrl, username = username, date = date,
                        content = content,
                        thumbUp = thumbUp.logIfParseNull("commentReply", "thumbUp"),
                        id = null,
                        isChildComment = true, post = post, reportableId = reportableId,
                        reportableType = reportableType, redirectUrl = reportRedirectUrl
                    )
                )
            }
        }

        return UiState.Success(VideoComments(replyList))
    }

    fun reportCommentResponse(body: String): UiState<String> {
        // 暂时无法判断是否举报成功
        return UiState.Success("已成功檢舉該則評論，我們會儘快處理您的檢舉。")
    }
}
