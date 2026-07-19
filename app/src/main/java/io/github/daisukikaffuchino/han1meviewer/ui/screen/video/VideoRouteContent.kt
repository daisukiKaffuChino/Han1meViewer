package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
import io.github.daisukikaffuchino.han1meviewer.ui.bridge.VideoPageHost
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.CommentViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.VideoViewModel
import io.github.daisukikaffuchino.utils.application

@Composable
fun VideoRouteContent(
    videoCode: String,
    videoState: VideoLoadingState<*>,
    videoViewModel: VideoViewModel,
    commentViewModel: CommentViewModel,
    fromDownload: Boolean,
    pendingDownloadPrompt: DownloadPromptState?,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo.Artist) -> Unit,
    onToggleFavorite: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo) -> Unit,
    onRateVideo: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo, Boolean) -> Unit,
    onManageMyList: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo.MyList?, List<Boolean>) -> Unit,
    onPrepareDownload: (String, io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo?) -> Unit,
    onConfirmDownloadPrompt: (io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo?) -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (String) -> Unit,
    onOpenShare: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
    stringLongPressShare: String,
    pageHost: VideoPageHost,
) {
    val hostUiState by videoViewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val disableComments = remember {
        PreferenceManager.getDefaultSharedPreferences(application)
            .getBoolean("disable_comments", false)
    }
    val tabs = remember(disableComments, hostUiState.commentBadgeCount, fromDownload) {
        buildList {
            add(VideoTabItem(R.string.introduction))
            if (!fromDownload && !disableComments) {
                add(VideoTabItem(R.string.comment, badgeCount = hostUiState.commentBadgeCount))
            }
        }
    }

    VideoScreen(
        state = videoState,
        onRetry = onRetry,
    ) {
        VideoTabsContent(
            tabs = tabs,
            selectedTabIndex = hostUiState.selectedTabIndex,
            onSelectedTabChange = { videoViewModel.setSelectedTabIndex(videoCode, it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = with(LocalDensity.current) { hostUiState.appBarBottomInsetPx.toDp() }),
        ) { page ->
            if (page == 0) {
                RenderVideoIntroductionContent(
                    videoCode = videoCode,
                    viewModel = videoViewModel,
                    pendingDownloadPrompt = pendingDownloadPrompt,
                    onPendingDownloadPromptChange = onPendingDownloadPromptChange,
                    onOpenVideo = onOpenVideo,
                    onOpenArtist = onOpenArtist,
                    onNavigateToSearch = onNavigateToSearch,
                    onToggleSubscribe = onToggleSubscribe,
                    onToggleFavorite = onToggleFavorite,
                    onRateVideo = onRateVideo,
                    onManageMyList = onManageMyList,
                    onPrepareDownload = onPrepareDownload,
                    onConfirmDownloadPrompt = onConfirmDownloadPrompt,
                    onRequestOpenOfficialDownloadPage = onRequestOpenOfficialDownloadPage,
                    onOpenWebPage = onOpenWebPage,
                    onOpenOriginalComic = onOpenOriginalComic,
                    onOpenShare = onOpenShare,
                    onCopyText = onCopyText,
                    onIntroductionLinkClick = onIntroductionLinkClick,
                    stringLongPressShare = stringLongPressShare,
                )
            } else {
                RenderVideoCommentContent(
                    viewModel = commentViewModel,
                    reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow() },
                    getMessageText = { message ->
                        if (message.args.isNotEmpty()) {
                            application.getString(message.resId, *message.args.toTypedArray())
                        } else {
                            application.getString(message.resId)
                        }
                    },
                    pageHost = pageHost,
                )
            }
        }
    }
}
