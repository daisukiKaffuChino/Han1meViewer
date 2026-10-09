package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.ReportReason
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoCommentArgs
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoComments
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.CommentReplyBar
import io.github.daisukikaffuchino.han1meviewer.ui.component.CommentReportDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.VideoCommentCard
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.LoadingContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.toUiMessage
import io.github.daisukikaffuchino.han1meviewer.util.toUiMessage as toUiMessageWithContext
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeCommentList
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.util.parseTimeStrToMinutes
import io.github.daisukikaffuchino.han1meviewer.util.safeSortedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildCommentScreen(
    commentsFlow: StateFlow<List<VideoComments.VideoComment>>,
    commentStateFlow: StateFlow<UiState<VideoComments>>,
    reportMessageFlow: Flow<CommentMessage>,
    postReplyStateFlow: Flow<UiState<Unit>>,
    commentLikeStateFlow: Flow<UiState<VideoCommentArgs>>,
    reportReasons: List<ReportReason>,
    isAlreadyLogin: Boolean,
    onRefresh: () -> Unit,
    onReply: (VideoComments.VideoComment, String) -> Unit,
    onReport: (VideoComments.VideoComment, ReportReason) -> Unit,
    onThumbUp: (VideoComments.VideoComment) -> Unit,
    onThumbDown: (VideoComments.VideoComment) -> Unit,
    onCommentLikeSuccess: (VideoCommentArgs) -> Unit,
    onReplyStateChange: (Boolean) -> Unit = {},
) {
    val comments by commentsFlow.collectAsStateWithLifecycle()
    val state by commentStateFlow.collectAsStateWithLifecycle()
    val containerSize = LocalWindowInfo.current.containerSize
    val maxScreenWidth = containerSize.width.dp

    var replyingComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var replyText by remember { mutableStateOf(TextFieldValue("")) }
    var reportComment by remember { mutableStateOf<VideoComments.VideoComment?>(null) }
    var selectedReasonIndex by remember { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loginFirstText = stringResource(R.string.login_first)
    val sendFailedText = stringResource(R.string.send_failed)
    val sendSuccessText = stringResource(R.string.send_success)
    val sendingReplyText = stringResource(R.string.sending_reply)
    val commentTooShortText = stringResource(R.string.comment_too_short)

    LaunchedEffect(reportMessageFlow) {
        reportMessageFlow.collect { message ->
            if (message.text.isNotBlank()) {
                snackbarHostState.showSnackbar(message.text)
            }
        }
    }

    LaunchedEffect(postReplyStateFlow) {
        postReplyStateFlow.collect { replyState ->
            when (replyState) {
                is UiState.Error -> snackbarHostState.showSnackbar(sendFailedText)
                UiState.Empty -> Unit
                UiState.Loading -> snackbarHostState.showSnackbar(sendingReplyText)
                is UiState.Success -> {
                    snackbarHostState.showSnackbar(sendSuccessText)
                    onRefresh()
                }
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(commentLikeStateFlow) {
        commentLikeStateFlow.collect { likeState ->
            when (likeState) {
                is UiState.Error -> {
                    snackbarHostState.showSnackbar(likeState.throwable.toUiMessageWithContext(context))
                }

                UiState.Loading -> Unit

                UiState.Empty -> Unit

                is UiState.Success -> onCommentLikeSuccess(likeState.data)
            }
        }
    }

    val sortedComments = remember(comments) {
        comments.safeSortedBy({ parseTimeStrToMinutes(it.date) }, descending = false)
    }
    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    BackHandler(enabled = replyingComment != null) {
        replyingComment = null
        replyText = TextFieldValue("")
    }

    LaunchedEffect(replyingComment) {
        if (replyingComment != null) {
            onReplyStateChange(true)
        }
    }

    Scaffold(
        modifier = Modifier.widthIn(max = maxScreenWidth)
            .background(color = HanimeDefaults.Colors.pageSurface),
        containerColor = HanimeDefaults.Colors.pageSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = replyingComment != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                CommentReplyBar(
                    text = replyText,
                    onTextChange = { replyText = it },
                    onSend = {
                        replyingComment?.let { target ->
                            val prefix = "@${target.username}"
                            val contentLength =
                                replyText.text.trim().removePrefix(prefix).trimStart().length
                            if (contentLength < 5) {
                                scope.launch { snackbarHostState.showSnackbar(commentTooShortText) }
                            } else {
                                onReply(target, replyText.text)
                                replyingComment = null
                                replyText = TextFieldValue("")
                            }
                        }
                    },
                    placeholder = stringResource(R.string.reply_child_comment),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.child_comment),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (sortedComments.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.comment_count, sortedComments.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val initialLoading = state is UiState.Loading && sortedComments.isEmpty()
            val initialError = state is UiState.Error && sortedComments.isEmpty()
            val loadingHint = rememberRandomLoadingHint()
            PageContent(
                isLoading = initialLoading,
                isError = initialError,
                isEmpty = sortedComments.isEmpty(),
                errorMessage = (state as? UiState.Error)?.throwable?.toUiMessage() ?: "",
                onRetry = onRefresh,
                loading = {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        message = loadingHint,
                    )
                },
                error = {
                    ErrorContent(
                        title = stringResource(R.string.load_reply_failed),
                        message = (state as UiState.Error).throwable.toUiMessage(),
                        onRetry = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                },
                empty = {
                    EmptyContent(hint = stringResource(R.string.comment_not_found))
                },
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollInterop),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedComments, key = { it.stableKey }) { comment ->
                        VideoCommentCard(
                            comment = comment,
                            onReply = {
                                if (!isAlreadyLogin) {
                                    scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                } else {
                                    replyingComment = comment
                                    replyText = TextFieldValue("@${comment.username} ")
                                }
                            },
                            onThumbUp = {
                                if (!isAlreadyLogin) {
                                    scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                } else {
                                    onThumbUp(comment)
                                }
                            },
                            onThumbDown = {
                                if (!isAlreadyLogin) {
                                    scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                } else {
                                    onThumbDown(comment)
                                }
                            },
                            onReport = {
                                if (!isAlreadyLogin) {
                                    scope.launch { snackbarHostState.showSnackbar(loginFirstText) }
                                } else {
                                    reportComment = comment
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (reportComment != null) {
        CommentReportDialog(
            reportReasons = reportReasons,
            selectedReasonIndex = selectedReasonIndex,
            onSelectReason = { selectedReasonIndex = it },
            onConfirm = {
                val reason = reportReasons.getOrNull(selectedReasonIndex)
                val target = reportComment
                if (reason != null && target != null) {
                    onReport(target, reason)
                }
                reportComment = null
                selectedReasonIndex = -1
            },
            onDismiss = {
                reportComment = null
                selectedReasonIndex = -1
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ChildCommentScreenPreview() {
    ComponentPreview {
        ChildCommentScreen(
            commentsFlow = MutableStateFlow(fakeCommentList),
            commentStateFlow = MutableStateFlow(UiState.Success(VideoComments(fakeCommentList.toMutableList()))),
            reportMessageFlow = flowOf(CommentMessage("")),
            postReplyStateFlow = flowOf(UiState.Success(Unit)),
            commentLikeStateFlow = flowOf(
                UiState.Success(
                    VideoCommentArgs(
                        isPositive = true,
                        commentPosition = 0,
                        comment = fakeCommentList.first(),
                    )
                )
            ),
            reportReasons = listOf(
                ReportReason(
                    lang = ReportReason.Language(
                        zhrTW = "垃圾訊息",
                        zhrCN = "垃圾信息",
                        en = "Spam",
                    ),
                    reasonKey = "spam",
                )
            ),
            isAlreadyLogin = true,
            onRefresh = {},
            onReply = { _, _ -> },
            onReport = { _, _ -> },
            onThumbUp = {},
            onThumbDown = {},
            onCommentLikeSuccess = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ChildCommentScreenEmptyPreview() {
    ComponentPreview {
        ChildCommentScreen(
            commentsFlow = MutableStateFlow(emptyList()),
            commentStateFlow = MutableStateFlow(UiState.Success(VideoComments(mutableListOf()))),
            reportMessageFlow = flowOf(CommentMessage("")),
            postReplyStateFlow = flowOf(UiState.Loading),
            commentLikeStateFlow = flowOf(UiState.Loading),
            reportReasons = emptyList(),
            isAlreadyLogin = true,
            onRefresh = {},
            onReply = { _, _ -> },
            onReport = { _, _ -> },
            onThumbUp = {},
            onThumbDown = {},
            onCommentLikeSuccess = {},
        )
    }
}
