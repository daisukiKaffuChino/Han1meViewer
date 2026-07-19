package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeSearchShareText
import io.github.daisukikaffuchino.han1meviewer.getHanimeShareText
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.SubscriptionScreen
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import io.github.daisukikaffuchino.utils.copyTextToClipboard
import io.github.daisukikaffuchino.utils.showShortToast

@Composable
fun SubscriptionRouteScreen(
    onBack: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MySubscriptionsViewModel = viewModel()
    SubscriptionScreen(
        navigateBack = onBack,
        viewModel = viewModel,
        onClickArtist = { onNavigateToSearch(it) },
        onLongClickArtist = { artistName ->
            copyTextToClipboard(getHanimeSearchShareText(artistName))
            showShortToast(R.string.copy_to_clipboard)
        },
        onClickVideosItem = onNavigateToVideo,
        onLongClickVideosItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            showShortToast(R.string.copy_to_clipboard)
        },
    )
}
