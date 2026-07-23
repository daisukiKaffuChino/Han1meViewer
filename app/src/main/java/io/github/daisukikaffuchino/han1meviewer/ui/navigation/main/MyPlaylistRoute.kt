package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeShareText
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist.PlaylistScreen
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import io.github.daisukikaffuchino.han1meviewer.ui.util.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.utils.SonnerToast

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyPlayListViewModelV2 = viewModel()
    val copyTextToClipboard = rememberCopyTextToClipboard()
    PlaylistScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            SonnerToast.success(R.string.copy_to_clipboard)
        },
    )
}
