package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.StateSet
import android.view.HapticFeedbackConstants
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.mediarouter.app.MediaRouteButton
import coil3.compose.AsyncImage
import com.google.android.gms.cast.framework.CastButtonFactory
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.ui.component.FilledIconButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.FilledTonalButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.FilledTonalIconButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.IconButton
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackEngine
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackQuality
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlayerDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoPlayerUi(
    modifier: Modifier = Modifier,
    playbackEngine: PlaybackEngine? = null,
    posterUrl: String? = null,
    title: String,
    currentTime: String,
    totalTime: String,
    progress: Float,
    bufferedProgress: Float,
    currentVolume: Float,
    currentBrightness: Float,
    showControls: Boolean = true,
    isFullscreen: Boolean = false,
    isPlaying: Boolean = false,
    isLooping: Boolean = false,
    isPlaybackEnded: Boolean = false,
    showCastButton: Boolean = false,
    isCasting: Boolean = false,
    castDeviceName: String? = null,
    isLocked: Boolean = false,
    showPoster: Boolean = false,
    showResumeButton: Boolean = false,
    showLoading: Boolean = false,
    showRetry: Boolean = false,
    onPlayClick: () -> Unit = {},
    onLoopToggle: () -> Unit = {},
    onReplay: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    onResumeClick: () -> Unit = onPlayClick,
    onRetry: () -> Unit = {},
    qualities: List<PlaybackQuality> = emptyList(),
    selectedQuality: String? = null,
    onQualitySelected: (Int) -> Unit = {},
    playbackSpeed: Float = PlayerDefaults.DEFAULT_SPEED,
    onPlaybackSpeedSelected: (Float) -> Unit = {},
    superResolutionLabel: String,
    superResolutionOptions: List<String> = emptyList(),
    selectedSuperResolutionIndex: Int = 0,
    onSuperResolutionSelected: (Int) -> Unit = {},
    hKeyframeLabel: String,
    isHKeyframesEnabled: Boolean = true,
    hKeyframeOptions: List<String> = emptyList(),
    hKeyframes: List<HKeyframeEntity.Keyframe> = emptyList(),
    isHKeyframeLocal: Boolean = false,
    onHKeyframeSelected: (Int) -> Unit = {},
    onHKeyframeUpdated: (HKeyframeEntity.Keyframe, HKeyframeEntity.Keyframe) -> Unit = { _, _ -> },
    onHKeyframeDeleted: (HKeyframeEntity.Keyframe) -> Unit = {},
    onHKeyframeLongPress: () -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onProgressGesture: (Float) -> Unit = onProgressChange,
    progressGestureSensitivity: Float = PlayerDefaults.DEFAULT_PROGRESS_SLIDE_SENSITIVITY.toFloat(),
    countdownLabel: String? = null,
    videoAspectRatio: Float = 16f / 9f,
) {
    var showControlsState by remember { mutableStateOf(true) }
    var gestureType by remember { mutableStateOf<GestureIndicatorType?>(null) }
    var gesturePercent by remember { mutableFloatStateOf(0.5f) }
    var dragStartedOnLeft by remember { mutableStateOf(true) }
    var progressDirection by remember { mutableStateOf<ProgressGestureDirection?>(null) }
    var isProgressGestureActive by remember { mutableStateOf(false) }
    var isLongPressSpeedActive by remember { mutableStateOf(false) }
    var suppressTapUntilMs by remember { mutableLongStateOf(0L) }
    var activeSidePanel by remember { mutableStateOf<PlayerSidePanel?>(null) }
    var displayedSidePanel by remember { mutableStateOf<PlayerSidePanel?>(null) }
    var showUnlockButton by remember { mutableStateOf(false) }
    var unlockButtonTimeoutToken by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val view = LocalView.current
    var deviceTime by remember(context) {
        mutableStateOf(DateFormat.getTimeFormat(context).format(Date()))
    }

    LaunchedEffect(context) {
        while (true) {
            deviceTime = DateFormat.getTimeFormat(context).format(Date())
            delay(60_000L.milliseconds)
        }
    }

    LaunchedEffect(showControlsState, isPlaying, activeSidePanel) {
        if (showControlsState && isPlaying && activeSidePanel == null) {
            delay(3000.milliseconds)
            showControlsState = false
        }
    }

    val effectiveShowControls = showControls && showControlsState && !isLocked
    val playerUiVisible = effectiveShowControls && activeSidePanel == null
    val speedSelectedIndex = PlayerDefaults.speeds.indexOfFirst { it == playbackSpeed }
        .takeIf { it >= 0 }
        ?: PlayerDefaults.speeds.indexOfFirst { it == PlayerDefaults.DEFAULT_SPEED }
    val resolvedQualityLabel =
        selectedQuality ?: qualities.lastOrNull()?.label
        ?: stringResource(R.string.player_auto_quality)
    val qualitySelectedIndex = qualities.indexOfFirst { it.label == resolvedQualityLabel }
    val latestProgress by rememberUpdatedState(progress)
    val latestVolume by rememberUpdatedState(currentVolume)
    val latestBrightness by rememberUpdatedState(currentBrightness)
    val latestProgressSensitivity by rememberUpdatedState(progressGestureSensitivity)
    val latestOnProgressGesture by rememberUpdatedState(onProgressGesture)
    val latestOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val latestOnBrightnessChange by rememberUpdatedState(onBrightnessChange)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val latestOnLongPressEnd by rememberUpdatedState(onLongPressEnd)

    LaunchedEffect(isPlaying) {
        if (!isPlaying) isLongPressSpeedActive = false
    }

    LaunchedEffect(activeSidePanel) {
        if (activeSidePanel != null) {
            displayedSidePanel = activeSidePanel
            showControlsState = true
        }
    }

    LaunchedEffect(effectiveShowControls, isLocked) {
        if (!effectiveShowControls || isLocked) {
            activeSidePanel = null
        }
    }

    LaunchedEffect(isLocked, unlockButtonTimeoutToken) {
        if (isLocked) {
            showControlsState = false
            showUnlockButton = true
            delay(3000.milliseconds)
            showUnlockButton = false
        } else {
            showUnlockButton = false
            showControlsState = true
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures(onTap = { unlockButtonTimeoutToken++ })
                } else {
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    detectTapGestures(
                        onPress = {
                            coroutineScope {
                                var activated = false
                                val activationJob = launch {
                                    delay(longPressTimeout.milliseconds)
                                    if (latestIsPlaying) {
                                        activated = true
                                        isLongPressSpeedActive = true
                                        showControlsState = false
                                        suppressTapUntilMs = Long.MAX_VALUE
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        latestOnLongPressStart()
                                    }
                                }
                                val released = tryAwaitRelease()
                                activationJob.cancel()
                                if (activated && released) {
                                    isLongPressSpeedActive = false
                                    showControlsState = false
                                    if (suppressTapUntilMs == Long.MAX_VALUE) {
                                        suppressTapUntilMs = SystemClock.uptimeMillis() + 500L
                                    }
                                    latestOnLongPressEnd()
                                }
                            }
                        },
                        onTap = {
                            if (SystemClock.uptimeMillis() <= suppressTapUntilMs) {
                                suppressTapUntilMs = 0L
                            } else {
                                showControlsState = !showControlsState
                            }
                        },
                        onDoubleTap = { onPlayClick() },
                    )
                }
            }
    ) {

        /**
         * 视频渲染层
         */
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val safeAspectRatio = if (videoAspectRatio > 0f) videoAspectRatio else 16f / 9f
            val containerAspectRatio = if (maxHeight.value > 0f) {
                maxWidth.value / maxHeight.value
            } else {
                safeAspectRatio
            }
            val videoModifier = if (safeAspectRatio >= containerAspectRatio) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(safeAspectRatio)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(safeAspectRatio)
            }

            if (playbackEngine != null && !isCasting) {
                key(playbackEngine, safeAspectRatio) {
                    Box(
                        modifier = videoModifier
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                SurfaceView(context).apply {
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            playbackEngine.attachSurface(holder.surface)
                                        }

                                        override fun surfaceChanged(
                                            holder: SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int,
                                        ) {
                                            if (playbackEngine is io.github.daisukikaffuchino.han1meviewer.ui.player.MpvPlaybackEngine) {
                                                playbackEngine.updateSurfaceSize(width, height)
                                            }
                                        }

                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            playbackEngine.detachSurface(holder.surface)
                                        }
                                    })
                                }
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = videoModifier.background(Color.Black)
                )
            }

            if (isCasting) {
                Surface(
                    modifier = Modifier.offset(y = (-48).dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Text(
                        text = stringResource(
                            R.string.player_casting_to,
                            castDeviceName ?: stringResource(R.string.player_cast_device),
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        var gestureStartProgress = 0f
                        var gestureStartVolume = 0f
                        var gestureStartBrightness = 0f
                        var longPressOwnsDrag = false
                        detectDragGestures(
                            onDragStart = { offset ->
                                longPressOwnsDrag = isLongPressSpeedActive
                                if (longPressOwnsDrag) {
                                    gestureType = null
                                    progressDirection = null
                                    isProgressGestureActive = false
                                    return@detectDragGestures
                                }
                                dragStartedOnLeft = offset.x < size.width / 2f
                                gestureType = null
                                progressDirection = null
                                isProgressGestureActive = false
                                gestureStartProgress = latestProgress
                                gestureStartVolume = latestVolume
                                gestureStartBrightness = latestBrightness
                            },
                            onDragEnd = {
                                if (longPressOwnsDrag) {
                                    isLongPressSpeedActive = false
                                    showControlsState = false
                                    if (suppressTapUntilMs == Long.MAX_VALUE) {
                                        suppressTapUntilMs = SystemClock.uptimeMillis() + 500L
                                    }
                                    latestOnLongPressEnd()
                                    longPressOwnsDrag = false
                                }
                                gestureType = null
                                isProgressGestureActive = false
                            },
                            onDragCancel = {
                                if (longPressOwnsDrag) {
                                    isLongPressSpeedActive = false
                                    showControlsState = false
                                    latestOnLongPressEnd()
                                    longPressOwnsDrag = false
                                }
                                gestureType = null
                                isProgressGestureActive = false
                            },
                            onDrag = { _, dragAmount ->
                                if (longPressOwnsDrag || isLongPressSpeedActive) {
                                    return@detectDragGestures
                                }
                                val type =
                                    gestureType ?: if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                        GestureIndicatorType.Progress
                                    } else if (dragStartedOnLeft) {
                                        GestureIndicatorType.Brightness
                                    } else {
                                        GestureIndicatorType.Volume
                                    }
                                if (gestureType == null) {
                                    gesturePercent = when (type) {
                                        GestureIndicatorType.Progress -> gestureStartProgress
                                        GestureIndicatorType.Brightness -> gestureStartBrightness
                                        GestureIndicatorType.Volume -> gestureStartVolume
                                    }
                                    isProgressGestureActive = type == GestureIndicatorType.Progress
                                }
                                gestureType = type
                                val next = when (type) {
                                    GestureIndicatorType.Progress -> {
                                        progressDirection = if (dragAmount.x < 0f) {
                                            ProgressGestureDirection.Backward
                                        } else {
                                            ProgressGestureDirection.Forward
                                        }
                                        (gesturePercent + dragAmount.x /
                                                (size.width * latestProgressSensitivity.coerceAtLeast(
                                                    1f
                                                )))
                                            .coerceIn(0f, 1f)
                                    }

                                    else ->
                                        (gesturePercent - dragAmount.y /
                                                (size.height * 0.8f).coerceAtLeast(1f)).coerceIn(
                                            0f,
                                            1f
                                        )
                                }
                                gesturePercent = next
                                when (type) {
                                    GestureIndicatorType.Brightness -> latestOnBrightnessChange(next)
                                    GestureIndicatorType.Volume -> latestOnVolumeChange(next)
                                    GestureIndicatorType.Progress -> latestOnProgressGesture(next)
                                }
                            },
                        )
                    }
                },
        )

        gestureType?.let { type ->
            GestureIndicatorOverlay(
                visible = true,
                type = type,
                percent = gesturePercent,
                progressDirection = progressDirection,
                modifier = Modifier.fillMaxSize(),
            )
        }

        /**
         * 封面
         */
        if (showPoster && posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        /**
         * 顶部渐变
         */
        AnimatedVisibility(
            visible = playerUiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        /**
         * 底部渐变
         */
        AnimatedVisibility(
            visible = playerUiVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        }

        /**
         * 顶部控制栏
         */
        AnimatedVisibility(
            visible = playerUiVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 4.dp,
                    )
            ) {

                /**
                 * Background
                 */
                if (isFullscreen) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(20.dp))
                    ) {

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.graphicsLayer {
                                            renderEffect =
                                                RenderEffect
                                                    .createBlurEffect(
                                                        32f,
                                                        32f,
                                                        Shader.TileMode.CLAMP
                                                    )
                                                    .asComposeRenderEffect()
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .background(
                                    Color.Black.copy(alpha = 0.18f)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(20.dp)
                                )
                        )
                    }
                }

                /**
                 * Content
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    /**
                     * Back
                     */
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_ios),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    /**
                     * Home
                     */
                    IconButton(
                        onClick = onHomeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    /**
                     * Title
                     */
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    if (showCastButton) {
                        AndroidView(
                            modifier = Modifier.size(24.dp),
                            factory = { context ->
                                MediaRouteButton(context).also { button ->
                                    button.minimumWidth = 0
                                    button.minimumHeight = 0
                                    button.contentDescription = context.getString(R.string.enable_google_cast)
                                    CastButtonFactory.setUpMediaRouteButton(context, button)
                                    button.setRemoteIndicatorDrawable(createGoogleCastIndicator(context))
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onLoopToggle,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_repeat),
                            contentDescription = stringResource(R.string.loop_play),
                            tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (isFullscreen) {
                        PlayerMenuChip(
                            label = hKeyframeLabel,
                            onClick = {
                                if (isHKeyframesEnabled) {
                                    activeSidePanel = PlayerSidePanel.HKeyframe
                                } else {
                                    SonnerToast.info(R.string.h_keyframes_not_enabled)
                                }
                            },
                            onLongClick = {
                                if (isHKeyframesEnabled) {
                                    onHKeyframeLongPress()
                                } else {
                                    SonnerToast.info(R.string.h_keyframes_not_enabled)
                                }
                            },
                        )

                        if (superResolutionOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))

                            PlayerMenuChip(
                                label = superResolutionLabel,
                                onClick = { activeSidePanel = PlayerSidePanel.SuperResolution },
                            )


                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = deviceTime,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            PlayerBatteryIndicator()
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isLongPressSpeedActive,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.46f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fast_forward),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
        }

        /**
         * 中间播放按钮
         */
        AnimatedVisibility(
            visible =
                !isLocked &&
                        activeSidePanel == null &&
                        gestureType == null &&
                        !isPlaybackEnded &&
                        (!isPlaying || effectiveShowControls),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (showLoading && !isProgressGestureActive) {
                    ContainedLoadingIndicator()
                } else if (!isPlaying) {
                    FilledTonalIconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                } else {
                    // Playing: small pause button when controls are visible
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }

        /**
         * 锁定按钮
         */
        AnimatedVisibility(
            visible = if (isLocked) showUnlockButton else playerUiVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FilledIconButton(
                onClick = onLockClick,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(42.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    painter = if (isLocked)
                        painterResource(R.drawable.ic_lock)
                    else
                        painterResource(R.drawable.ic_unlock),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        /**
         * 底部控制栏
         */
        AnimatedVisibility(
            visible = playerUiVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 4.dp,
                    )
            ) {

                /**
                 * Background
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.graphicsLayer {
                                        renderEffect =
                                            RenderEffect
                                                .createBlurEffect(
                                                    32f,
                                                    32f,
                                                    Shader.TileMode.CLAMP
                                                )
                                                .asComposeRenderEffect()
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                Color.Black.copy(alpha = 0.18f)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(18.dp)
                            )
                    )
                }

                /**
                 * Content
                 */
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {

                    /**
                     * Ultra Thin Slider
                     */
                    PlayerSlider(
                        value = progress,
                        buffered = bufferedProgress,
                        onValueChange = onProgressChange,
                        modifier = Modifier.height(12.dp)
                    )

                    /**
                     * Bottom Controls
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        /**
                         * Play
                         */
                        IconButton(
                            onClick = onPlayClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                painter = if (isPlaying)
                                    painterResource(R.drawable.ic_pause)
                                else
                                    painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        /**
                         * Time
                         */
                        Text(
                            text = stringResource(
                                R.string.player_time_format,
                                currentTime,
                                totalTime
                            ),
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        PlayerMenuChip(
                            label = stringResource(R.string.player_speed_format, playbackSpeed),
                            onClick = { activeSidePanel = PlayerSidePanel.Speed },
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        PlayerMenuChip(
                            label = resolvedQualityLabel,
                            onClick = { activeSidePanel = PlayerSidePanel.Quality },
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        /**
                         * Fullscreen
                         */
                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_fullscreen),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        /**
         * Resume 按钮
         */
        AnimatedVisibility(
            visible = showResumeButton && activeSidePanel == null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ElevatedButton(
                onClick = onResumeClick,
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.player_play_from_beginning))
            }
        }

        AnimatedVisibility(
            visible = isPlaybackEnded && activeSidePanel == null && !isLocked,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = HanimeDefaults.Colors.pageSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.playback_finished),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(onClick = onReplay) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(stringResource(R.string.replay))
                    }
                }
            }
        }

        /**
         * Retry
         */
        AnimatedVisibility(
            visible = showRetry && activeSidePanel == null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = HanimeDefaults.Colors.pageSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(28.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = stringResource(R.string.video_loading_failed),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = onRetry
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }

        /**
         * Timer
         */
        AnimatedVisibility(
            visible = countdownLabel != null && activeSidePanel == null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 90.dp,
                    start = 12.dp
                ),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
            ) {
                Text(
                    text = countdownLabel.orEmpty(),
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
        }

        AnimatedVisibility(
            visible = activeSidePanel != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            activeSidePanel = null
                        }
                )

            }
        }

        AnimatedVisibility(
            visible = activeSidePanel != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (displayedSidePanel) {
                    PlayerSidePanel.HKeyframe -> {
                        PlayerSidePanelSheet(
                            options = hKeyframeOptions,
                            selectedIndex = null,
                            panelWidth = 216.dp,
                            hKeyframes = hKeyframes,
                            isHKeyframeLocal = isHKeyframeLocal,
                            emptyText = stringResource(R.string.here_is_empty) + "\n" +
                                    stringResource(R.string.long_press_to_add_h_keyframe),
                            onSelected = { index ->
                                activeSidePanel = null
                                onHKeyframeSelected(index)
                            },
                            onHKeyframeUpdated = onHKeyframeUpdated,
                            onHKeyframeDeleted = onHKeyframeDeleted,
                        )
                    }

                    PlayerSidePanel.Speed -> {
                        PlayerSidePanelSheet(
                            options = PlayerDefaults.speeds.map {
                                stringResource(R.string.player_speed_format, it)
                            },
                            selectedIndex = speedSelectedIndex,
                            panelWidth = 156.dp,
                            onSelected = { index ->
                                activeSidePanel = null
                                onPlaybackSpeedSelected(PlayerDefaults.speeds[index])
                            },
                        )
                    }

                    PlayerSidePanel.SuperResolution -> {
                        PlayerSidePanelSheet(
                            options = superResolutionOptions,
                            selectedIndex = selectedSuperResolutionIndex,
                            panelWidth = 156.dp,
                            onSelected = { index ->
                                activeSidePanel = null
                                onSuperResolutionSelected(index)
                            },
                        )
                    }

                    PlayerSidePanel.Quality -> {
                        PlayerSidePanelSheet(
                            options = qualities.map(PlaybackQuality::label),
                            selectedIndex = qualitySelectedIndex.takeIf { it >= 0 },
                            panelWidth = 156.dp,
                            onSelected = { index ->
                                activeSidePanel = null
                                onQualitySelected(index)
                            },
                        )
                    }

                    null -> Unit
                }
            }
        }
    }
}

private fun createGoogleCastIndicator(context: Context): Drawable = StateListDrawable().apply {
    addState(
        intArrayOf(android.R.attr.state_checked),
        whiteDrawable(context, androidx.media3.cast.R.drawable.media_route_button_connected),
    )
    addState(
        intArrayOf(android.R.attr.state_checkable),
        whiteDrawable(context, androidx.media3.cast.R.drawable.media_route_button_disconnected),
    )
    addState(
        StateSet.WILD_CARD,
        whiteDrawable(context, androidx.media3.cast.R.drawable.media_route_button_disconnected),
    )
}

private fun whiteDrawable(context: Context, drawableRes: Int): Drawable =
    requireNotNull(ContextCompat.getDrawable(context, drawableRes)).mutate().also { drawable ->
        DrawableCompat.setTint(drawable, android.graphics.Color.WHITE)
    }

@Composable
private fun PlayerMenuChip(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.06f),
                shape,
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private enum class PlayerSidePanel {
    HKeyframe,
    Speed,
    SuperResolution,
    Quality,
}

@Composable
private fun BoxScope.PlayerSidePanelSheet(
    options: List<String>,
    selectedIndex: Int?,
    onSelected: (Int) -> Unit,
    emptyText: String? = null,
    panelWidth: Dp = 156.dp,
    hKeyframes: List<HKeyframeEntity.Keyframe> = emptyList(),
    isHKeyframeLocal: Boolean = false,
    onHKeyframeUpdated: (HKeyframeEntity.Keyframe, HKeyframeEntity.Keyframe) -> Unit = { _, _ -> },
    onHKeyframeDeleted: (HKeyframeEntity.Keyframe) -> Unit = {},
) {
    val isHKeyframePanel = hKeyframes.isNotEmpty() || isHKeyframeLocal || emptyText != null
    var editingKeyframe by remember { mutableStateOf<HKeyframeEntity.Keyframe?>(null) }
    var deletingKeyframe by remember { mutableStateOf<HKeyframeEntity.Keyframe?>(null) }
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(panelWidth)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(32f, 32f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(Color.Black.copy(alpha = 0.72f))
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            if (isHKeyframePanel && hKeyframes.isNotEmpty()) {
                itemsIndexed(hKeyframes) { index, keyframe ->
                    val label = options.getOrNull(index).orEmpty()
                    val separatorIndex = label.indexOf(' ')
                    val marker = if (separatorIndex >= 0) {
                        label.substring(0, separatorIndex)
                    } else {
                        stringResource(R.string.player_keyframe_index, index + 1)
                    }
                    val time =
                        if (separatorIndex >= 0) label.substring(separatorIndex + 1) else label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(index) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = marker,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = time,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (isHKeyframeLocal) {
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { editingKeyframe = keyframe },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = Color.White.copy(alpha = 0.76f),
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit),
                                        contentDescription = stringResource(R.string.edit),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { deletingKeyframe = keyframe },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = Color.White.copy(alpha = 0.76f),
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        keyframe.prompt?.takeIf(String::isNotBlank)?.let { prompt ->
                            Text(
                                text = prompt,
                                color = Color.White.copy(alpha = 0.68f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else if (options.isEmpty()) {
                item {
                    Text(
                        text = emptyText.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp),
                    )
                }
            } else {
                itemsIndexed(options) { index, option ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(index) }
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(vertical = 11.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                Color.White
                            },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    editingKeyframe?.let { keyframe ->
        var positionText by remember(keyframe) { mutableStateOf(keyframe.position.toString()) }
        var promptText by remember(keyframe) { mutableStateOf(keyframe.prompt.orEmpty()) }
        var isPositionError by remember(keyframe) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { editingKeyframe = null },
            title = { Text(stringResource(R.string.modify_h_keyframe)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = positionText,
                        onValueChange = {
                            positionText = it
                            isPositionError = false
                        },
                        label = { Text(stringResource(R.string.position_ms)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isPositionError,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text(stringResource(R.string.prompt)) },
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val position = positionText.toLongOrNull()
                        if (position == null || position < 0L) {
                            isPositionError = true
                        } else {
                            onHKeyframeUpdated(
                                keyframe,
                                keyframe.copy(
                                    position = position,
                                    prompt = promptText.ifBlank { null },
                                ),
                            )
                            editingKeyframe = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKeyframe = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deletingKeyframe?.let { keyframe ->
        AlertDialog(
            onDismissRequest = { deletingKeyframe = null },
            title = { Text(stringResource(R.string.sure_to_delete)) },
            text = { Text(keyframe.position.toString()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHKeyframeDeleted(keyframe)
                        deletingKeyframe = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingKeyframe = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun PlayerSlider(
    value: Float,
    buffered: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,

        /**
         * Thumb
         */
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp),
                contentAlignment = Alignment.Center
            ) {

                /**
                 * Glow
                 */
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .background(
                            Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                )

                /**
                 * Real Thumb
                 */
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            Color.White,
                            CircleShape
                        )
                )
            }
        },

        /**
         * Track
         */
        track = {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {

                /**
                 * Background Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Color.White.copy(alpha = 0.14f)
                        )
                )

                /**
                 * Buffered Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth(buffered.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Color.White.copy(alpha = 0.32f)
                        )
                )

                /**
                 * Active Track
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
            }
        }
    )
}

enum class GestureIndicatorType {
    Brightness,
    Volume,
    Progress,
}

private enum class ProgressGestureDirection {
    Backward,
    Forward,
}

@Composable
private fun GestureIndicatorOverlay(
    visible: Boolean,
    type: GestureIndicatorType,
    percent: Float,
    modifier: Modifier = Modifier,
    progressDirection: ProgressGestureDirection? = null,
    text: String? = null,
) {
    val displayText = text ?: stringResource(
        R.string.player_progress_percent,
        (percent * 100).toInt(),
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            /**
             * Glass Container
             */
            Box(
                modifier = Modifier
                    .size(
                        width = 170.dp,
                        height = 190.dp
                    )
                    .clip(RoundedCornerShape(36.dp))
            ) {

                /**
                 * Blur Layer
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect =
                                        RenderEffect
                                            .createBlurEffect(
                                                55f,
                                                55f,
                                                Shader.TileMode.CLAMP
                                            )
                                            .asComposeRenderEffect()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            Color.Black.copy(alpha = 0.32f)
                        )
                )

                /**
                 * Glass Gradient
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                /**
                 * Border
                 */
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(36.dp)
                        )
                )

                /**
                 * Content
                 */
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        painter = when (type) {
                            GestureIndicatorType.Brightness -> painterResource(R.drawable.ic_light_mode)
                            GestureIndicatorType.Volume -> painterResource(R.drawable.ic_volume_up)
                            GestureIndicatorType.Progress -> when (progressDirection) {
                                ProgressGestureDirection.Backward -> painterResource(R.drawable.ic_fast_rewind)
                                else -> painterResource(R.drawable.ic_fast_forward)
                            }
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (type) {
                            GestureIndicatorType.Brightness -> stringResource(R.string.player_gesture_brightness)
                            GestureIndicatorType.Volume -> stringResource(R.string.player_gesture_volume)
                            GestureIndicatorType.Progress -> stringResource(R.string.player_gesture_progress)
                        },
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100)),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayText,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        VideoPlayerUiPreviewContent()
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540,
)
@Composable
private fun VideoPlayerUiFullscreenPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme(),
    ) {
        VideoPlayerUiPreviewContent(isFullscreen = true, isPlaying = true)
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiLoadingPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        VideoPlayerUiPreviewContent(
            showLoading = true,
            isPlaying = true,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun VideoPlayerUiRetryPreview() {
    ComponentPreview {
        VideoPlayerUiPreviewContent(
            showRetry = true,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 960,
    heightDp = 540
)
@Composable
private fun GestureIndicatorOverlayPreview() {
    ComponentPreview {
        GestureIndicatorOverlay(
            visible = true,
            type = GestureIndicatorType.Brightness,
            percent = 0.5f,
        )
    }
}

@Composable
private fun VideoPlayerUiPreviewContent(
    isFullscreen: Boolean = false,
    isPlaying: Boolean = false,
    showLoading: Boolean = false,
    showRetry: Boolean = false,
) {
    VideoPlayerUi(
        title = VideoPlayerUiPreviewData.title,
        currentTime = VideoPlayerUiPreviewData.currentTime,
        totalTime = VideoPlayerUiPreviewData.totalTime,
        progress = VideoPlayerUiPreviewData.progress,
        bufferedProgress = VideoPlayerUiPreviewData.bufferedProgress,
        currentVolume = 0.7f,
        currentBrightness = 0.65f,
        isFullscreen = isFullscreen,
        isPlaying = isPlaying,
        showLoading = showLoading,
        showRetry = showRetry,
        qualities = listOf(PlaybackQuality(label = "1080p", uri = "")),
        selectedQuality = "1080p",
        superResolutionLabel = stringResource(R.string.player_anime4k_label),
        superResolutionOptions = listOf(
            stringResource(R.string.super_resolution_off),
            stringResource(R.string.super_resolution_performance),
            stringResource(R.string.super_resolution_quality),
        ),
        hKeyframeLabel = stringResource(R.string.player_h_keyframe),
    )
}

private object VideoPlayerUiPreviewData {
    const val title = "视频标题标题标题标题"
    const val currentTime = "12:36"
    const val totalTime = "24:12"
    const val progress = 0.45f
    const val bufferedProgress = 0.72f
}
