package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.graphics.Rect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackEngine
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackQuality
import io.github.daisukikaffuchino.utils.VibrationUtil
import kotlin.math.roundToInt

data class ClassicTabletLayoutConfig(
    val relatedItems: List<HanimeInfo>,
    val onHideRelatedInIntroChange: (Boolean) -> Unit,
    val onSideRelatedCollapsedChange: (Boolean) -> Unit,
    val onOpenVideo: (HanimeInfo) -> Unit,
)

@Composable
fun VideoShellContent(
    isTabletMode: Boolean,
    isInPipMode: Boolean,
    isFullscreen: Boolean,
    playerHeightDp: Dp?,
    playbackEngine: PlaybackEngine,
    posterUrl: String?,
    title: String,
    currentTime: String,
    totalTime: String,
    progress: Float,
    bufferedProgress: Float,
    currentVolume: Float,
    currentBrightness: Float,
    isPlaying: Boolean,
    isLooping: Boolean,
    isPlaybackEnded: Boolean,
    showCastButton: Boolean,
    isCasting: Boolean,
    castDeviceName: String?,
    isLocked: Boolean,
    showPoster: Boolean,
    showLoading: Boolean,
    showRetry: Boolean,
    showResumeButton: Boolean,
    onPlayClick: () -> Unit,
    onLoopToggle: () -> Unit,
    onReplay: () -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onLockClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onRetry: () -> Unit,
    onResumeClick: () -> Unit,
    qualities: List<PlaybackQuality>,
    selectedQuality: String?,
    onQualitySelected: (Int) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedSelected: (Float) -> Unit,
    superResolutionLabel: String,
    superResolutionOptions: List<String>,
    selectedSuperResolutionIndex: Int,
    onSuperResolutionSelected: (Int) -> Unit,
    hKeyframeLabel: String,
    isHKeyframesEnabled: Boolean,
    hKeyframeOptions: List<String>,
    hKeyframes: List<HKeyframeEntity.Keyframe>,
    isHKeyframeLocal: Boolean,
    onHKeyframeSelected: (Int) -> Unit,
    onHKeyframeUpdated: (HKeyframeEntity.Keyframe, HKeyframeEntity.Keyframe) -> Unit,
    onHKeyframeDeleted: (HKeyframeEntity.Keyframe) -> Unit,
    onHKeyframeLongPress: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onProgressGesture: (Float) -> Unit,
    progressGestureSensitivity: Float,
    countdownLabel: String?,
    videoAspectRatio: Float,
    onPlayerBoundsChanged: (Rect) -> Unit,
    tabsContent: @Composable () -> Unit,
    classicTabletLayout: ClassicTabletLayoutConfig?,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        isTabletMode && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSideRelated = isTabletLandscape && !isInPipMode && !isFullscreen
    val showClassicSideRelated = showSideRelated && classicTabletLayout != null
    var isSideRelatedCollapsed by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(showClassicSideRelated) {
        if (showClassicSideRelated) {
            classicTabletLayout.onHideRelatedInIntroChange.invoke(true)
        }
        onDispose {
            if (showClassicSideRelated) {
                classicTabletLayout.onHideRelatedInIntroChange.invoke(false)
                classicTabletLayout.onSideRelatedCollapsedChange.invoke(false)
            }
        }
    }

    LaunchedEffect(showClassicSideRelated, isSideRelatedCollapsed) {
        if (!showClassicSideRelated) isSideRelatedCollapsed = false
        classicTabletLayout?.onSideRelatedCollapsedChange?.invoke(
            showClassicSideRelated && isSideRelatedCollapsed
        )
    }

    @Composable
    fun PlayerContent(modifier: Modifier) {
        Box(modifier = modifier) {
            if (!isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color.Black)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
            ) {
                VideoPlayerUi(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInWindow()
                            onPlayerBoundsChanged(
                                Rect(
                                    bounds.left.roundToInt(),
                                    bounds.top.roundToInt(),
                                    bounds.right.roundToInt(),
                                    bounds.bottom.roundToInt(),
                                )
                            )
                        },
                    playbackEngine = playbackEngine,
                    posterUrl = posterUrl,
                    title = title,
                    currentTime = currentTime,
                    totalTime = totalTime,
                    progress = progress,
                    bufferedProgress = bufferedProgress,
                    currentVolume = currentVolume,
                    currentBrightness = currentBrightness,
                    isFullscreen = isFullscreen,
                    isPlaying = isPlaying,
                    isLooping = isLooping,
                    isPlaybackEnded = isPlaybackEnded,
                    showCastButton = showCastButton,
                    isCasting = isCasting,
                    castDeviceName = castDeviceName,
                    isLocked = isLocked || isInPipMode,
                    showPoster = showPoster,
                    showControls = !isInPipMode,
                    showLoading = showLoading,
                    showRetry = showRetry,
                    showResumeButton = showResumeButton,
                    onPlayClick = onPlayClick,
                    onLoopToggle = onLoopToggle,
                    onReplay = onReplay,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onFullscreenClick = onFullscreenClick,
                    onLockClick = onLockClick,
                    onProgressChange = onProgressChange,
                    onRetry = onRetry,
                    onResumeClick = onResumeClick,
                    qualities = qualities,
                    selectedQuality = selectedQuality,
                    onQualitySelected = onQualitySelected,
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                    superResolutionLabel = superResolutionLabel,
                    superResolutionOptions = superResolutionOptions,
                    selectedSuperResolutionIndex = selectedSuperResolutionIndex,
                    onSuperResolutionSelected = onSuperResolutionSelected,
                    hKeyframeLabel = hKeyframeLabel,
                    isHKeyframesEnabled = isHKeyframesEnabled,
                    hKeyframeOptions = hKeyframeOptions,
                    hKeyframes = hKeyframes,
                    isHKeyframeLocal = isHKeyframeLocal,
                    onHKeyframeSelected = onHKeyframeSelected,
                    onHKeyframeUpdated = onHKeyframeUpdated,
                    onHKeyframeDeleted = onHKeyframeDeleted,
                    onHKeyframeLongPress = onHKeyframeLongPress,
                    onLongPressStart = onLongPressStart,
                    onLongPressEnd = onLongPressEnd,
                    onVolumeChange = onVolumeChange,
                    onBrightnessChange = onBrightnessChange,
                    onProgressGesture = onProgressGesture,
                    progressGestureSensitivity = progressGestureSensitivity,
                    countdownLabel = countdownLabel,
                    videoAspectRatio = videoAspectRatio,
                )
            }
        }
    }

    @Composable
    fun MainContent(contentModifier: Modifier) {
        Box(modifier = contentModifier) {
            if (!isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Color.Black)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
            ) {
                VideoPlayerUi(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isFullscreen && playerHeightDp != null) {
                                Modifier.height(playerHeightDp)
                            } else {
                                Modifier.weight(1f)
                            }
                        )
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInWindow()
                            onPlayerBoundsChanged(
                                Rect(
                                    bounds.left.roundToInt(),
                                    bounds.top.roundToInt(),
                                    bounds.right.roundToInt(),
                                    bounds.bottom.roundToInt(),
                                )
                            )
                        },
                    playbackEngine = playbackEngine,
                    posterUrl = posterUrl,
                    title = title,
                    currentTime = currentTime,
                    totalTime = totalTime,
                    progress = progress,
                    bufferedProgress = bufferedProgress,
                    currentVolume = currentVolume,
                    currentBrightness = currentBrightness,
                    isFullscreen = isFullscreen,
                    isPlaying = isPlaying,
                    isLooping = isLooping,
                    isPlaybackEnded = isPlaybackEnded,
                    showCastButton = showCastButton,
                    isCasting = isCasting,
                    castDeviceName = castDeviceName,
                    isLocked = isLocked || isInPipMode,
                    showPoster = showPoster,
                    showControls = !isInPipMode,
                    showLoading = showLoading,
                    showRetry = showRetry,
                    showResumeButton = showResumeButton,
                    onPlayClick = onPlayClick,
                    onLoopToggle = onLoopToggle,
                    onReplay = onReplay,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onFullscreenClick = onFullscreenClick,
                    onLockClick = onLockClick,
                    onProgressChange = onProgressChange,
                    onRetry = onRetry,
                    onResumeClick = onResumeClick,
                    qualities = qualities,
                    selectedQuality = selectedQuality,
                    onQualitySelected = onQualitySelected,
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                    superResolutionLabel = superResolutionLabel,
                    superResolutionOptions = superResolutionOptions,
                    selectedSuperResolutionIndex = selectedSuperResolutionIndex,
                    onSuperResolutionSelected = onSuperResolutionSelected,
                    hKeyframeLabel = hKeyframeLabel,
                    isHKeyframesEnabled = isHKeyframesEnabled,
                    hKeyframeOptions = hKeyframeOptions,
                    hKeyframes = hKeyframes,
                    isHKeyframeLocal = isHKeyframeLocal,
                    onHKeyframeSelected = onHKeyframeSelected,
                    onHKeyframeUpdated = onHKeyframeUpdated,
                    onHKeyframeDeleted = onHKeyframeDeleted,
                    onHKeyframeLongPress = onHKeyframeLongPress,
                    onLongPressStart = onLongPressStart,
                    onLongPressEnd = onLongPressEnd,
                    onVolumeChange = onVolumeChange,
                    onBrightnessChange = onBrightnessChange,
                    onProgressGesture = onProgressGesture,
                    progressGestureSensitivity = progressGestureSensitivity,
                    countdownLabel = countdownLabel,
                    videoAspectRatio = videoAspectRatio,
                )
                if (!isInPipMode && !isFullscreen) {
                    Box(modifier = Modifier.weight(1f)) {
                        tabsContent()
                    }
                }
            }
        }
    }

    if (showClassicSideRelated) {
        val indicatorWidth = 28.dp
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val sideWidth by animateDpAsState(
                targetValue = if (isSideRelatedCollapsed) indicatorWidth else maxWidth * 0.38f,
                animationSpec = tween(durationMillis = 300),
                label = "sideRelatedWidth",
            )
            Row(modifier = Modifier.fillMaxSize()) {
                MainContent(
                    contentModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Row(
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RelatedCollapseIndicator(
                        collapsed = isSideRelatedCollapsed,
                        onClick = { isSideRelatedCollapsed = !isSideRelatedCollapsed },
                        modifier = Modifier
                            .width(indicatorWidth)
                            .fillMaxHeight(),
                    )
                    if (!isSideRelatedCollapsed) {
                        RelatedVideosSection(
                            videos = classicTabletLayout.relatedItems,
                            onOpenVideo = classicTabletLayout.onOpenVideo,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    } else if (showSideRelated) {
        Row(modifier = modifier.fillMaxSize()) {
            PlayerContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .consumeWindowInsets(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth(0.38f),
            ) {
                tabsContent()
            }
        }
    } else {
        MainContent(contentModifier = modifier.fillMaxSize())
    }
}

@Composable
private fun RelatedCollapseIndicator(
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .clickable {
                VibrationUtil.performHapticFeedback(view)
                onClick()
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = if (collapsed) {
                painterResource(R.drawable.ic_chevron_left)
            } else {
                painterResource(R.drawable.ic_chevron_right)
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        )
    }
}
