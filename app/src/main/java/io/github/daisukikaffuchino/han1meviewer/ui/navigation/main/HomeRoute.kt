package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeShareText
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.model.Announcement
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.component.AnnouncementDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.HomePageScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.HomeUiEvent
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.LocalSearchHistoryQuery
import io.github.daisukikaffuchino.han1meviewer.ui.util.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.utils.showShortToast
import kotlinx.coroutines.flow.first

@Composable
fun HomeRouteScreen(
    activity: MainActivity,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel = activity.viewModel
    val copyTextToClipboard = rememberCopyTextToClipboard()
    val uriHandler = LocalUriHandler.current
    val confirmToExit = stringResource(R.string.confirm_to_exit)
    val confirmExitMessage = stringResource(R.string.confirm_exit_message)
    val cancel = stringResource(R.string.cancel)
    val exit = stringResource(R.string.exit)
    var showExitDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    CompositionLocalProvider(
        LocalSearchHistoryQuery provides { keyword: String ->
            DatabaseRepo.SearchHistory.loadAll(keyword).first().map { it.query }
        }
    ) {
        HomePageScreen(
            viewModel = viewModel,
            isDrawerOpen = isDrawerOpen,
            onEvent = { event ->
                when (event) {
                    is HomeUiEvent.OpenDrawer -> onOpenDrawer()
                    is HomeUiEvent.NavigateToPreview -> onNavigateToPreview()
                    is HomeUiEvent.OpenSearchPage -> onNavigateToSearch(event.query)
                    is HomeUiEvent.NavigateToSearchAdvanced -> onNavigateToSearchAdvanced(event.params)
                    is HomeUiEvent.OpenVideo -> onNavigateToVideo(event.videoCode)
                    is HomeUiEvent.LongPressVideoCopy -> {
                        copyTextToClipboard(getHanimeShareText(event.videoTitle, event.videoCode))
                        showShortToast(R.string.copy_to_clipboard)
                    }
                    is HomeUiEvent.ShowAnnouncementDialog -> { announcement = event.announcement }
                    is HomeUiEvent.ShowExitDialog -> { showExitDialog = true }
                    is HomeUiEvent.OpenUpdatePage -> {
                        runCatching { uriHandler.openUri(event.downloadUrl) }
                            .onFailure { showShortToast(R.string.update_link_open_failed) }
                    }
                    is HomeUiEvent.IgnoreUpdate -> viewModel.ignoreUpdate(event.versionCode)
                }
            }
        )
    }

    if (showExitDialog) {
        ConfirmDialog(
            visible = true,
            title = confirmToExit,
            message = confirmExitMessage,
            confirmText = exit,
            dismissText = cancel,
            onConfirm = { activity.finish() },
            onDismiss = { showExitDialog = false },
        )
    }

    announcement?.let { data ->
        AnnouncementDialog(
            announcementData = data,
            onDismiss = { announcement = null },
        )
    }
}
