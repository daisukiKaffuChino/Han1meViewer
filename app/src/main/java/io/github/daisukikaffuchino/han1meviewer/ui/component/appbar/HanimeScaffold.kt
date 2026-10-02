package io.github.daisukikaffuchino.han1meviewer.ui.component.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.daisukikaffuchino.han1meviewer.ui.component.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.verticalBounce
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

/**
 * 通用页面脚手架。
 *
 * 页面框架（[HanimePageSurface]）统一负责左右边距与圆角裁剪，
 * 各页面内容不应再自行维护页面级左右边距，参考 MomoQR 的 `TopAppBarScaffold`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanimeScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    HanimeScaffold(
        topBar = {
            HanimeTopAppBar(
                title = title,
                onBack = onBack,
                subtitle = subtitle,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanimeScaffold(
    title: @Composable () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    HanimeScaffold(
        topBar = {
            HanimeTopAppBar(
                title = title,
                onBack = onBack,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
fun HanimeScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = HanimeDefaults.Colors.pageSurface,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .verticalBounce()
                .fillMaxSize(),
        ) {
            HanimePageSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = HanimeDefaults.Spacing.contentHorizontal),
            ) {
                content(PaddingValues())
            }
        }
    }
}

@Preview
@Composable
private fun HanimeScaffoldPreview() {
    ComponentPreview {
        HanimeScaffold(
            title = "组件标题",
            subtitle = {
                Text(
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = "副标题"
                )
            },
            onBack = {},
            actions = {
                FilledIconButton(onClick = { }, enabled = true) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.start_all),
                    )
                }
                FilledIconButton(onClick = { }, enabled = false) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = stringResource(R.string.pause_all),
                    )
                }
            }
        ) {
            EmptyContent("空空的")
        }
    }
}
