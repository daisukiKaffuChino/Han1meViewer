package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 加载更多底部组件
 * @param state 加载状态
 * @param modifier 修饰符
 * @param textColor 文字颜色
 * @param loadedPage 已加载页数
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadMoreFooter(
    state: PagedUiState<*>,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    loadedPage: Int? = null,
    isLoadingMore: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state is PagedUiState.Loading || isLoadingMore -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.loading),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state is PagedUiState.NoMoreData -> {
                Text(
                    text = if (loadedPage == null)
                        stringResource(R.string.load_complete)
                    else
                        stringResource(R.string.load_complete_with_pages, loadedPage),
                    color = textColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state is PagedUiState.Success<*> -> {}

            state is PagedUiState.Error -> {
                Text(
                    text = stringResource(R.string.load_failed_retry),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadMoreFooterPreview() {
    var loadMoreState by remember { mutableStateOf<PagedUiState<*>>(PagedUiState.Loading) }

    LazyColumn {
        items(20) { index ->
            Text("Item $index")
        }

        item {
            LoadMoreFooter(
                state = loadMoreState,
                modifier = Modifier.fillMaxWidth(),
                loadedPage = 100
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(5000.milliseconds)
        loadMoreState = PagedUiState.Loading
        delay(5000.milliseconds)
        loadMoreState = PagedUiState.NoMoreData
    }
}
