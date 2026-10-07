package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.dataOrNull
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageEmpty
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageError
import io.github.daisukikaffuchino.han1meviewer.logic.state.isFirstPageLoading
import io.github.daisukikaffuchino.han1meviewer.pienization
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.PreviewImageViewerDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.PreviewImageViewerState
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint

@Composable
fun GetchuPreviewDetailScreen(
    id: String,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
    viewModel: GetchuPreviewViewModel,
) {
    val detailState = remember(id) { viewModel.detailState(id) }
    val state = detailState.collectAsStateWithLifecycle().value
    var imageViewerState by remember { mutableStateOf<PreviewImageViewerState?>(null) }
    val loadingHint = rememberRandomLoadingHint()
    val isInspectionMode = LocalInspectionMode.current
    val imageLoader = rememberGetchuImageLoader()
    LaunchedEffect(id, isInspectionMode) {
        if (!isInspectionMode) viewModel.getDetail(id)
    }

    HanimeScaffold(
            title = stringResource(R.string.getchu_preview_detail),
            onBack = onBack,
    ) {
            PageContent(
            isLoading = state.isFirstPageLoading,
            isError = state.isFirstPageError,
            isEmpty = state.isFirstPageEmpty,
            errorMessage = (state as? PagedUiState.Error)?.throwable?.pienization.toString(),
            onRetry = { if (!isInspectionMode) viewModel.getDetail(id) },
            modifier = Modifier.fillMaxSize(),
            loadingMessage = loadingHint
            ) {
                state.dataOrNull?.let { detail ->
                    GetchuPreviewDetailContent(
                        detail = detail,
                        onOpenImage = { index, images ->
                            imageViewerState = PreviewImageViewerState(images, index)
                        },
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToVideoUrl = onNavigateToVideoUrl,
                        imageLoader = imageLoader
                    )
                }
            }
        }

    imageViewerState?.let { viewerState ->
        PreviewImageViewerDialog(
            imageUrls = viewerState.imageUrls,
            initialPage = viewerState.initialPage,
            onDismiss = { imageViewerState = null },
            imageLoader = imageLoader,
        )
    }
}
