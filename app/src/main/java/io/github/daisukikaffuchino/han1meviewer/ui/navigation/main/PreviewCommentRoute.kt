package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.PREVIEW_COMMENT_PREFIX
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.screen.video.ChildCommentScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.video.CommentMessage
import io.github.daisukikaffuchino.han1meviewer.ui.screen.video.CommentScreen
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.CommentViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.PreviewCommentPrefetcher
import io.github.daisukikaffuchino.utils.application
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewCommentRouteScreen(
    activity: MainActivity,
    route: PreviewCommentRoute,
    onBack: () -> Unit,
) {
    val viewModel: CommentViewModel = viewModel(viewModelStoreOwner = activity)
    val comments = viewModel.videoCommentFlow
    val commentState = viewModel.videoCommentStateFlow
    val commentUiState = remember(route.dateCode) {
        viewModel.getCommentUiState(route.dateCode)
    }
    var childCommentId by rememberSaveable { mutableStateOf(commentUiState.childCommentId) }
    val childSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded,
        ),
    )
    val scope = rememberCoroutineScope()
    val prefetchedComments = PreviewCommentPrefetcher.here(viewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
    val hasPrefetchedComments = prefetchedComments.isNotEmpty()
    val reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow<CommentMessage>() }

    LaunchedEffect(route.dateCode, hasPrefetchedComments, prefetchedComments) {
        viewModel.code = route.dateCode
        if (hasPrefetchedComments) {
            viewModel.updateComments(prefetchedComments)
        } else {
            viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(viewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        }
    }

    LaunchedEffect(Unit) {
        commentState.collect { state ->
            if (state is UiState.Success) {
                viewModel.currentUserId = state.data.currentUserId
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postCommentFlow.collect { state ->
            if (state is UiState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postReplyFlow.collect { state ->
            if (state is UiState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.commentLikeFlow.collect { state ->
            if (state is UiState.Success) {
                viewModel.handleCommentLike(state.data)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reportMessage.collect { msg ->
            val text = if (msg.args.isNotEmpty()) {
                activity.getString(msg.resId, *msg.args.toTypedArray())
            } else {
                activity.getString(msg.resId)
            }
            reportMessages.emit(CommentMessage(text))
        }
    }

    childCommentId?.let { currentCommentId ->
        ModalBottomSheet(
            onDismissRequest = {
                childCommentId = null
                viewModel.setChildCommentId(route.dateCode, null)
                viewModel.clearVideoReplyList()
            },
            sheetState = childSheetState,
            containerColor = HanimeDefaults.Colors.pageSurface,
        ) {
            LaunchedEffect(currentCommentId) {
                viewModel.getCommentReply(currentCommentId)
            }
            val mappedReportFlow = remember(viewModel.reportMessage) {
                viewModel.reportMessage.map { message ->
                    val text = if (message.args.isNotEmpty()) {
                        application.getString(message.resId, *message.args.toTypedArray())
                    } else {
                        application.getString(message.resId)
                    }
                    CommentMessage(text)
                }
            }
            ChildCommentScreen(
                commentsFlow = viewModel.videoReplyFlow,
                commentStateFlow = viewModel.videoReplyStateFlow,
                reportMessageFlow = mappedReportFlow,
                postReplyStateFlow = viewModel.postReplyFlow,
                commentLikeStateFlow = viewModel.commentLikeFlow,
                reportReasons = viewModel.reportReason,
                isAlreadyLogin = SettingsRepository.isAlreadyLogin,
                onRefresh = { viewModel.getCommentReply(currentCommentId) },
                onReply = { _, text ->
                    viewModel.postReply(currentCommentId, text)
                },
                onReport = { comment, reason ->
                    viewModel.reportComment(
                        reason.reasonKey ?: reason.value,
                        viewModel.currentUserId,
                        "${SettingsRepository.baseUrl}watch?v=${viewModel.code}",
                        comment.reportableType,
                        comment.reportableId,
                    )
                },
                onThumbUp = { comment ->
                    viewModel.likeChildComment(
                        true, 0, comment,
                        likeCommentStatus = comment.post.likeCommentStatus,
                    )
                },
                onThumbDown = { comment ->
                    viewModel.likeChildComment(
                        false, 0, comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus,
                    )
                },
                onCommentLikeSuccess = viewModel::handleCommentLike,
                onReplyStateChange = { isReplying ->
                    if (isReplying) {
                        scope.launch { childSheetState.expand() }
                    }
                },
            )
        }
    }

    HanimeScaffold(
        title = stringResource(R.string.latest_hanime_comment, route.date),
        onBack = onBack,
    ) { paddingValues ->
        CommentScreen(
            commentsFlow = comments,
            commentStateFlow = commentState,
            reportMessageFlow = reportMessages,
            currentSortType = viewModel.currentSortType,
            reportReasons = viewModel.reportReason,
            isPreviewCommentPrefetched = hasPrefetchedComments,
            isAlreadyLogin = SettingsRepository.isAlreadyLogin,
            onRefresh = { viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode) },
            onReply = { comment, text ->
                if (!SettingsRepository.isAlreadyLogin) return@CommentScreen
                val replyTargetId = comment.replyTargetIdOrNull
                if (replyTargetId == null) {
                    scope.launch {
                        reportMessages.emit(CommentMessage(activity.getString(R.string.there_is_a_small_issue)))
                    }
                    return@CommentScreen
                }
                viewModel.postReply(replyTargetId, text)
            },
            onReport = { comment, reason ->
                viewModel.reportComment(
                    reason.reasonKey ?: reason.value,
                    viewModel.currentUserId,
                    "${SettingsRepository.baseUrl}watch?v=${viewModel.code}",
                    comment.reportableType,
                    comment.reportableId,
                )
            },
            onThumbUp = { comment ->
                if (!SettingsRepository.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                }
            },
            onThumbDown = { comment ->
                if (!SettingsRepository.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                }
            },
            onViewMoreReplies = { comment ->
                comment.replyTargetIdOrNull?.let {
                    childCommentId = it
                    viewModel.setChildCommentId(route.dateCode, it)
                }
            },
            onSortChange = viewModel::setSortType,
            onComposeComment = { text ->
                viewModel.currentUserId?.let { id ->
                    viewModel.postComment(id, viewModel.code, PREVIEW_COMMENT_PREFIX, text)
                } ?: scope.launch {
                    reportMessages.emit(CommentMessage(activity.getString(R.string.there_is_a_small_issue)))
                }
            },
            listContentPadding = PaddingValues(vertical = 8.dp),
            initialFirstVisibleItemIndex = commentUiState.firstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = commentUiState.firstVisibleItemScrollOffset,
            onCommentScrollChange = { index, offset ->
                viewModel.setCommentScrollState(route.dateCode, index, offset)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
