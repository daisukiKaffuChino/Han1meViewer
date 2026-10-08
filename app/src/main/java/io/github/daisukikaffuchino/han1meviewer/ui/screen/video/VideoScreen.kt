package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.LoadingContent
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview

@Composable
fun VideoScreen(
    state: UiState<*>,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()

        if (LocalInspectionMode.current) {
            when (state) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingContent(message = stringResource(R.string.loading))
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = "视频加载失败",
                            message = state.throwable.message,
                            onRetry = onRetry,
                        )
                    }
                }

                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(hint = "该影片可能不存在")
                    }
                }

                else -> Unit
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenLoadingPreview() {
    ComponentPreview {
        VideoScreen(
            state = UiState.Loading,
            onRetry = {},
            content = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenErrorPreview() {
    ComponentPreview {
        VideoScreen(
            state = UiState.Error(Throwable("network error")),
            onRetry = {},
            content = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun VideoScreenNoContentPreview() {
    ComponentPreview {
        VideoScreen(
            state = UiState.Empty,
            onRetry = {},
            content = {},
        )
    }
}