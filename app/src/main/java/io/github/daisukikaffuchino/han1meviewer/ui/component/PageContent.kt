package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.LoadingContent

@Composable
fun PageContent(
    isLoading: Boolean,
    isError: Boolean,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    loadingMessage: String = "",
    errorMessage: String = "",
    onRetry: () -> Unit = {},
    loading: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingContent(message = loadingMessage)
        }
    },
    error: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorContent(message = errorMessage, onRetry = onRetry)
        }
    },
    empty: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(hint = "")
        }
    },
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        when {
            isError -> error()
            isLoading -> loading()
            isEmpty -> empty()
            else -> content()
        }
    }
}
