package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.HanimeErrorKind
import io.github.daisukikaffuchino.han1meviewer.logic.state.toHanimeError
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimePreview
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.pienization
import io.github.daisukikaffuchino.han1meviewer.ui.component.CardContainerSurface
import io.github.daisukikaffuchino.han1meviewer.ui.component.FilledTonalButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.IconButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeTopAppBar
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.EmptyContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.LoadingContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticButton as Button
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewContent(
    uiState: PreviewUiState,
    onEvent: (PreviewEvent) -> Unit,
    previewPagerState: PagerState,
    previewInfoList: List<HanimePreview.PreviewInfo>,
    modifier: Modifier = Modifier,
) {
    val loadingHint = rememberRandomLoadingHint()
    HanimeScaffold(
        modifier = modifier,
        topBar = {
            HanimeTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = uiState.currentDateLabel,
                        transitionSpec = {
                            val forward = uiState.monthAnimationDirection >= 0
                            (slideInVertically(
                                animationSpec = tween(320, easing = LinearOutSlowInEasing),
                                initialOffsetY = { height -> if (forward) height / 2 else -height / 2 }
                            ) + fadeIn(
                                animationSpec = tween(
                                    260,
                                    delayMillis = 40,
                                    easing = LinearOutSlowInEasing
                                )
                            )) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(220, easing = FastOutLinearInEasing),
                                        targetOffsetY = { height -> if (forward) -height / 2 else height / 2 }
                                    ) + fadeOut(
                                        animationSpec = tween(170, easing = FastOutLinearInEasing)
                                    ))
                        },
                        label = "preview_month_title",
                    ) { animatedDateLabel ->
                        Text(stringResource(R.string.latest_hanime_list_monthly, animatedDateLabel))
                    }
                },
                onBack = { onEvent(PreviewEvent.OnBack) },
                actions = {
                    IconButton(onClick = {
                        onEvent(
                            PreviewEvent.OnOpenComment(
                                uiState.currentDateLabel,
                                uiState.routeState.currentDateCode
                            )
                        )
                    }) {
                        BadgedBox(
                            badge = {
                                if (uiState.commentCount > 0) {
                                    Badge(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 20.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.commentCount > 999) "999+" else uiState.commentCount.toString(),
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_comment),
                                contentDescription = stringResource(R.string.comment),
                            )
                        }
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PreviewSourceNoticeCard(
                        onOpenWeb = { onEvent(PreviewEvent.OnOpenWebPreview) },
                        onOpenGetchu = { onEvent(PreviewEvent.OnOpenGetchuPreview) },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                item {
                    AnimatedContent(
                        targetState = uiState.monthHeaderState,
                        contentKey = { it.dateCode },
                        transitionSpec = {
                            val forward = uiState.monthAnimationDirection >= 0
                            (slideInHorizontally(
                                animationSpec = tween(420, easing = LinearOutSlowInEasing),
                                initialOffsetX = { width -> if (forward) width else -width }
                            ) + fadeIn(
                                animationSpec = tween(
                                    320,
                                    delayMillis = 70,
                                    easing = LinearOutSlowInEasing
                                )
                            )) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(260, easing = FastOutLinearInEasing),
                                        targetOffsetX = { width -> if (forward) -width else width }
                                    ) + fadeOut(
                                        animationSpec = tween(
                                            190,
                                            easing = FastOutLinearInEasing
                                        )
                                    ))
                        },
                        label = "preview_month_header",
                    ) { animatedHeaderState ->
                        PreviewHeaderSection(
                            headerImageUrl = animatedHeaderState.headerImageUrl,
                            prevLabel = animatedHeaderState.prevLabel,
                            nextLabel = animatedHeaderState.nextLabel,
                            canPrev = animatedHeaderState.canPrev,
                            canNext = animatedHeaderState.canNext,
                            onPrev = { onEvent(PreviewEvent.OnPrevMonth(animatedHeaderState.dateCode)) },
                            onNext = { onEvent(PreviewEvent.OnNextMonth(animatedHeaderState.dateCode)) },
                        )
                    }
                }

                when (uiState.displayState) {
                    is UiState.Loading -> item {
                        LoadingContent(
                            message = loadingHint
                        )
                    }

                    is UiState.Empty -> item {
                        EmptyContent(
                            hint = stringResource(R.string.empty_content),
                        )
                    }

                    is UiState.Error -> item {
                        val isPreviewEmpty =
                            uiState.displayState.throwable.toHanimeError().kind == HanimeErrorKind.NotFound
                        ErrorContent(
                            title = stringResource(R.string.hanime_list),
                            message = if (isPreviewEmpty) {
                                stringResource(R.string.preview_month_not_updated)
                            } else {
                                uiState.displayState.throwable.pienization.toString()
                            },
                            onRetry = if (isPreviewEmpty) null else {
                                { onEvent(PreviewEvent.OnRetryLoad) }
                            },
                        )
                    }

                    is UiState.Success -> {
                        item {
                            PreviewTourRow(
                                latestHanime = uiState.displayState.data.latestHanime,
                                selectedIndex = uiState.routeState.selectedIndex,
                                onSelect = { onEvent(PreviewEvent.OnSelectTourItem(it)) },
                            )
                        }

                        item {
                            if (previewInfoList.isEmpty()) {
                                EmptyContent(
                                    hint = stringResource(R.string.empty_content),
                                    subHint = stringResource(R.string.new_anime_trailers)
                                )
                            } else {
                                HorizontalPager(
                                    state = previewPagerState,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 620.dp)
                                        .animateContentSize(),
                                    verticalAlignment = Alignment.Top,
                                ) { page ->
                                    PreviewInfoCard(
                                        previewInfo = previewInfoList[page],
                                        onOpenVideo = { code ->
                                            onEvent(PreviewEvent.OnOpenVideo(code))
                                        },
                                        onOpenImage = { index, imageUrls ->
                                            onEvent(PreviewEvent.OnOpenImage(index, imageUrls))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSourceNoticeCard(
    onOpenWeb: () -> Unit,
    onOpenGetchu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardContainerSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.preview_discontinued_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpenWeb) {
                    Text(stringResource(R.string.visit_web_version))
                }
                Button(onClick = onOpenGetchu) {
                    Text(stringResource(R.string.view_getchu_preview))
                }
            }
        }
    }
}

@Composable
private fun PreviewHeaderSection(
    headerImageUrl: String?,
    prevLabel: String,
    nextLabel: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = headerImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onPrev,
                enabled = canPrev,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(prevLabel)
            }

            FilledTonalButton(
                onClick = onNext,
                enabled = canNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(nextLabel)
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null
                )
            }
        }
    }
}
