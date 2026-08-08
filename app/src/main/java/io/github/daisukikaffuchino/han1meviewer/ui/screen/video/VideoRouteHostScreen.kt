package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Rational
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.BuildConfig
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeVideoLink
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.model.SearchOption
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoLandscapeLayoutStyle
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.bridge.VideoPageHost
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.HomeRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.VideoRoute
import io.github.daisukikaffuchino.han1meviewer.ui.player.ComposePlaybackController
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackEngineFactory
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackPhase
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlaybackQuality
import io.github.daisukikaffuchino.han1meviewer.ui.player.PlayerKernel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.CommentViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.VideoViewModel
import io.github.daisukikaffuchino.utils.loadAssetAs
import io.github.daisukikaffuchino.utils.OrientationManager
import io.github.daisukikaffuchino.utils.SonnerToast
import io.github.daisukikaffuchino.utils.isX86_64Device
import io.github.daisukikaffuchino.utils.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.utils.rememberShareText
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Suppress("DEPRECATION")
@OptIn(ExperimentalTime::class)
@Composable
fun VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val copyTextToClipboard = rememberCopyTextToClipboard()
    val shareText = rememberShareText()
    val viewModel: VideoViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val kernel = remember { PlayerKernel.fromPreference(SettingsRepository.switchPlayerKernel) }
    val playbackEngine = remember(route.videoCode, route.localUri, kernel) {
        PlaybackEngineFactory.create(
            context = activity,
            kernel = kernel,
            allowCast = SettingsRepository.enableGoogleCast &&
                    route.localUri == null && route.videoCode != "-1",
        )
    }
    val playbackController = remember(playbackEngine) { ComposePlaybackController(playbackEngine) }
    val playbackState by playbackController.state.collectAsStateWithLifecycle()
    val appSettings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val isLargeScreenDevice =
        LocalConfiguration.current.smallestScreenWidthDp >= LARGE_SCREEN_MIN_WIDTH_DP
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
    val video = viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value
    val relatedItems = video?.relatedHanimes.orEmpty()
    val restoreLightSystemBars = when (SettingsRepository.useDarkMode) {
        "always_on" -> false
        "always_off" -> true
        else -> !isSystemInDarkTheme()
    }

    LaunchedEffect(playbackController) {
        playbackController.setPlaybackSpeed(SettingsRepository.playerSpeed)
    }
    LaunchedEffect(isLargeScreenDevice) {
        val currentSettings = SettingsRepository.current
        if (
            isLargeScreenDevice &&
            !currentSettings.tabletMode &&
            !currentSettings.largeScreenTabletModeHintShown
        ) {
            SettingsRepository.update {
                it.copy(largeScreenTabletModeHintShown = true)
            }
            SonnerToast.info(R.string.large_screen_tablet_mode_hint)
        }
    }
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }
    val genres = remember(SettingsRepository.baseUrl) {
        loadAssetAs<List<SearchOption>>(
            if (SettingsRepository.baseUrl == io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_URL[3]) {
                "search_options/genre_av.json"
            } else {
                "search_options/genre.json"
            }
        ).orEmpty()
    }

    var checkedQuality by remember(
        route.videoCode,
        route.localUri
    ) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf("") }
    var isSideRelatedCollapsed by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isPlayerLocked by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(currentScreenBrightness(activity)) }
    var previousScreenBrightness by remember { mutableStateOf<Float?>(null) }
    var speedBeforeLongPress by remember { mutableStateOf<Float?>(null) }
    var showResumeButton by remember { mutableStateOf(false) }
    var pendingPlayback by remember { mutableStateOf<PendingPlayback?>(null) }
    var mobilePlaybackConfirmed by remember(route.videoCode, route.localUri) {
        mutableStateOf(false)
    }
    var playerBounds by remember { mutableStateOf<Rect?>(null) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var hKeyframes by remember { mutableStateOf<HKeyframeEntity?>(null) }
    var superResolutionIndex by remember { mutableStateOf(0) }
    var pendingUnsubscribeArtist by remember { mutableStateOf<HanimeVideo.Artist?>(null) }
    var showNotificationPermissionReason by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showNotificationPermissionReason = true
    }
    var showDialog by remember { mutableStateOf(false) }

    val actions = remember(activity, scope, viewModel, genres) {
        VideoRouteActions(
            context = activity,
            scope = scope,
            viewModel = viewModel,
            genres = genres,
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            getCheckedQuality = { checkedQuality },
            setCheckedQuality = { checkedQuality = it },
            onOpenUri = uriHandler::openUri,
            onCopyText = copyTextToClipboard,
            onRequestUnsubscribe = { pendingUnsubscribeArtist = it },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }

    fun setSystemBars(hidden: Boolean) {
        val controller =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity.window.statusBarColor = Color.BLACK
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            activity.window.decorView.post {
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setSystemBars(false)
        previousScreenBrightness?.let { brightness ->
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = brightness
            }
            previousScreenBrightness = null
        }
        brightness = currentScreenBrightness(activity)
    }

    fun enterFullscreen(forceLandscape: Boolean = false) {
        isFullscreen = true
        val engineState = playbackController.state.value.engine
        activity.requestedOrientation = if (
            !forceLandscape &&
            engineState.videoWidth > 0 &&
            engineState.videoHeight > engineState.videoWidth
        ) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        setSystemBars(true)
    }

    val backCallback = remember(activity) {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    exitFullscreen()
                } else {
                    isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    fun updatePipAction() {
        if (!activity.isInPictureInPictureMode) return
        val isPlaying = playbackController.state.value.engine.isPlaying
        val icon = Icon.createWithResource(
            activity,
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
        )
        val intent = PendingIntent.getBroadcast(
            activity,
            0,
            android.content.Intent(MainActivity.ACTION_TOGGLE_PLAY)
                .setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        activity.setPictureInPictureParams(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(
                    listOf(
                        RemoteAction(
                            icon,
                            activity.getString(R.string.play_pause),
                            activity.getString(R.string.play_pause),
                            intent,
                        )
                    )
                )
                .build()
        )
    }

    val pageHost = remember(activity, playbackController, viewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) = viewModel.setCommentBadgeCount(count)

            override fun shouldEnterPip(): Boolean {
                val state = playbackController.state.value.engine
                return !state.isCasting &&
                        state.phase == PlaybackPhase.Ready &&
                        (state.isPlaying || state.positionMs > 0L)
            }

            override fun enterPipMode() {
                val intent = PendingIntent.getBroadcast(
                    activity,
                    0,
                    android.content.Intent(MainActivity.ACTION_TOGGLE_PLAY)
                        .setPackage(activity.packageName),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val state = playbackController.state.value.engine
                activity.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .setActions(
                            listOf(
                                RemoteAction(
                                    Icon.createWithResource(
                                        activity,
                                        if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                                    ),
                                    activity.getString(R.string.play_pause),
                                    activity.getString(R.string.play_pause),
                                    intent,
                                )
                            )
                        )
                        .apply { playerBounds?.let(::setSourceRectHint) }
                        .build()
                )
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                viewModel.setPipMode(isInPip)
                updatePipAction()
            }

            override fun togglePlayPause() {
                playbackController.togglePlayPause()
                updatePipAction()
            }
        }
    }

    SideEffect {
        activity.window.statusBarColor = Color.BLACK
        activity.window.navigationBarColor = Color.TRANSPARENT
        val controller =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        activity.window.isStatusBarContrastEnforced = false
        activity.window.isNavigationBarContrastEnforced = false
    }

    DisposableEffect(activity, playbackController, pageHost) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity.registerCurrentVideoHost(pageHost)
        activity.onBackPressedDispatcher.addCallback(lifecycleOwner, backCallback)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.registerCurrentVideoHost(null)
            playbackController.release()
            exitFullscreen()
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.navigationBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = restoreLightSystemBars
                isAppearanceLightNavigationBars = restoreLightSystemBars
            }
        }
    }

    DisposableEffect(
        lifecycleOwner,
        activity,
        playbackController,
        route.videoCode,
        appSettings.tabletMode,
    ) {
        val orientationManager = OrientationManager(activity) { orientation ->
            if (!appSettings.tabletMode) {
                if (orientation.isLandscape && !isFullscreen) {
                    enterFullscreen(forceLandscape = true)
                } else if (!orientation.isLandscape && isFullscreen) {
                    exitFullscreen()
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (route.videoCode != "-1") {
                        val progress = playbackController.state.value.engine.positionMs
                        scope.launch {
                            DatabaseRepo.WatchHistory.updateProgress(
                                route.videoCode,
                                progress
                            )
                        }
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (!activity.isInPictureInPictureMode &&
                        !playbackController.state.value.engine.isCasting
                    ) {
                        playbackController.pause()
                        exitFullscreen()
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        videoTitle = ""
        viewModel.videoCode = route.videoCode
        viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(route.videoCode, route.localUri, playbackController, viewModel.fromDownload) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.hanimeVideoStateFlow.collect { state ->
                when (state) {
                    is VideoLoadingState.Error -> {
                        state.throwable.localizedMessage?.let(SonnerToast::error)
                        if (state.throwable is ParseException) {
                            uriHandler.openUri(getHanimeVideoLink(route.videoCode))
                        }
                    }

                    is VideoLoadingState.Loading -> Unit

                    is VideoLoadingState.Success -> {
                        val info = state.info
                        videoTitle = info.title
                        val qualities = info.videoUrls.map { (label, link) ->
                            PlaybackQuality(
                                label = label,
                                uri = link.link,
                                mimeType = link.subtype?.let { "video/$it" },
                            )
                        }
                        if (qualities.isEmpty()) {
                            SonnerToast.error(R.string.fail_to_get_video_link)
                            uriHandler.openUri(getHanimeVideoLink(route.videoCode))
                        } else {
                            val history = DatabaseRepo.WatchHistory.findBy(route.videoCode)
                            showResumeButton = SettingsRepository.allowResumePlayback &&
                                    (history?.progress ?: 0L) > 5_000L
                            val request = PendingPlayback(
                                title = info.title,
                                qualities = qualities,
                                preferredQuality = SettingsRepository.videoQuality,
                                artworkUri = info.coverUrl,
                                startPositionMs = history?.progress ?: 0L,
                            )
                            if (!viewModel.fromDownload &&
                                !SettingsRepository.disableMobileDataWarning &&
                                !mobilePlaybackConfirmed &&
                                isActiveNetworkMetered(activity)
                            ) {
                                pendingPlayback = request
                            } else {
                                playbackController.load(
                                    title = request.title,
                                    qualities = request.qualities,
                                    preferredQuality = request.preferredQuality,
                                    artworkUri = request.artworkUri,
                                    startPositionMs = request.startPositionMs,
                                    playWhenReady = true,
                                )
                            }
                        }
                        if (!viewModel.fromDownload) {
                            viewModel.insertWatchHistoryWithCover(
                                WatchHistoryEntity(
                                    info.coverUrl,
                                    info.title,
                                    info.uploadTimeMillis,
                                    kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                    route.videoCode,
                                )
                            )
                        }
                    }

                    is VideoLoadingState.NoContent -> SonnerToast.error(R.string.video_might_not_exist)
                }
            }
        }
    }

    LaunchedEffect(viewModel, route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.loadDownloadedFlow.collect { entity ->
                val newQuality = checkedQuality ?: return@collect
                pendingDownloadPrompt = DownloadPromptState(
                    newQuality = newQuality,
                    oldQuality = entity?.quality,
                    oldGroupId = entity?.groupId,
                )
            }
        }
    }

    LaunchedEffect(route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.observeKeyframe(route.videoCode).collect {
                hKeyframes = it
                viewModel.hKeyframes = it
            }
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            launch {
                viewModel.localFavoriteActionFlow.collect { state ->
                    when (state) {
                        is WebsiteState.Error -> SonnerToast.error(R.string.add_failed)
                        is WebsiteState.Success -> SonnerToast.success(R.string.add_success)
                        WebsiteState.Loading -> Unit
                    }
                }
            }
            launch {
                viewModel.localMyListActionFlow.collect { state ->
                    when (state) {
                        is WebsiteState.Error -> SonnerToast.error(R.string.modify_failed)
                        is WebsiteState.Success -> SonnerToast.success(R.string.modify_success)
                        WebsiteState.Loading -> Unit
                    }
                }
            }
        }
    }

    LaunchedEffect(playbackState.engine.isPlaying) {
        viewModel.setScrollDisabled(playbackState.engine.isPlaying)
        updatePipAction()
    }

    LaunchedEffect(isFullscreen) {
        backCallback.isEnabled = isFullscreen
    }

    LaunchedEffect(showResumeButton) {
        if (showResumeButton) {
            kotlinx.coroutines.delay(5_000L.milliseconds)
            showResumeButton = false
        }
    }

    val countdownLabel = remember(playbackState.engine.positionMs, hKeyframes, isFullscreen) {
        if (!isFullscreen || !SettingsRepository.hKeyframesEnable) {
            null
        } else {
            hKeyframes?.keyframes.orEmpty().mapIndexedNotNull { index, keyframe ->
                val remaining = keyframe.position - playbackState.engine.positionMs
                if (remaining in 0L until SettingsRepository.whenCountdownRemind) {
                    val seconds = remaining / 1000L
                    val time = if (seconds >= 1L) {
                        (seconds + 1L).toString()
                    } else {
                        "%.1f".format(remaining / 1000f)
                    }
                    if (SettingsRepository.showCommentWhenCountdown && !keyframe.prompt.isNullOrBlank()) {
                        "#${index + 1} ${keyframe.prompt}\n$time"
                    } else {
                        time
                    }
                } else {
                    null
                }
            }.firstOrNull()
        }
    }

    val resolvedPlayerHeightDp = when {
        hostUiState.isInPipMode -> null
        appSettings.tabletMode -> if (isSideRelatedCollapsed) 500.dp else 400.dp
        else -> 250.dp
    }

    LaunchedEffect(resolvedPlayerHeightDp, hostUiState.playerHeightDp) {
        if (hostUiState.playerHeightDp != resolvedPlayerHeightDp) {
            viewModel.setPlayerHeightDp(resolvedPlayerHeightDp)
        }
    }

    VideoShellContent(
        isTabletMode = appSettings.tabletMode,
        isInPipMode = hostUiState.isInPipMode,
        isFullscreen = isFullscreen,
        playerHeightDp = resolvedPlayerHeightDp,
        playbackEngine = playbackEngine,
        posterUrl = video?.coverUrl,
        title = videoTitle,
        currentTime = formatPlaybackTime(playbackState.engine.positionMs),
        totalTime = formatPlaybackTime(playbackState.engine.durationMs),
        progress = playbackProgress(
            playbackState.engine.positionMs,
            playbackState.engine.durationMs
        ),
        bufferedProgress = playbackProgress(
            playbackState.engine.bufferedPositionMs,
            playbackState.engine.durationMs,
        ),
        currentVolume = volume,
        currentBrightness = brightness,
        isPlaying = playbackState.engine.isPlaying,
        isPlaybackEnded = playbackState.engine.phase == PlaybackPhase.Ended,
        showCastButton = playbackState.engine.isCastSupported,
        isCasting = playbackState.engine.isCasting,
        castDeviceName = playbackState.engine.castDeviceName,
        isLocked = isPlayerLocked,
        showPoster = !playbackState.engine.hasRenderedFirstFrame,
        showLoading =
            videoState is VideoLoadingState.Loading ||
                    playbackState.engine.phase == PlaybackPhase.Preparing,
        showRetry = playbackState.engine.phase == PlaybackPhase.Error,
        showResumeButton = showResumeButton,
        onPlayClick = playbackController::togglePlayPause,
        onReplay = playbackController::replay,
        onBackClick = { activity.onBackPressedDispatcher.onBackPressed() },
        onHomeClick = {
            activity.mainBackStack.popTo(HomeRoute)
        },
        onFullscreenClick = {
            if (isFullscreen) exitFullscreen() else enterFullscreen()
        },
        onLockClick = { isPlayerLocked = !isPlayerLocked },
        onProgressChange = { value ->
            val duration = playbackState.engine.durationMs
            if (duration > 0L) playbackController.seekTo((duration * value).toLong())
        },
        onRetry = {
            video?.let { info ->
                val qualities =
                    info.videoUrls.map { (label, link) ->
                        PlaybackQuality(label, link.link, mimeType = link.subtype?.let { "video/$it" })
                    }
                playbackController.load(
                    title = info.title,
                    qualities = qualities,
                    preferredQuality = SettingsRepository.videoQuality,
                    artworkUri = info.coverUrl,
                )
            }
        },
        onResumeClick = {
            playbackController.seekTo(0L)
            showResumeButton = false
        },
        qualities = playbackState.qualities,
        selectedQuality = playbackState.qualities
            .getOrNull(playbackState.selectedQualityIndex)
            ?.label,
        onQualitySelected = playbackController::selectQuality,
        playbackSpeed = playbackState.engine.playbackSpeed,
        onPlaybackSpeedSelected = playbackController::setPlaybackSpeed,
        superResolutionLabel = stringResource(R.string.player_anime4k_label),
        superResolutionOptions = if (kernel == PlayerKernel.MpvPlayer && !playbackState.engine.isCasting) {
            listOf(
                activity.getString(R.string.super_resolution_off),
                activity.getString(R.string.super_resolution_performance),
                activity.getString(R.string.super_resolution_quality),
            )
        } else {
            emptyList()
        },
        selectedSuperResolutionIndex = superResolutionIndex,
        onSuperResolutionSelected = { index ->
            superResolutionIndex = index
            (playbackEngine as? io.github.daisukikaffuchino.han1meviewer.ui.player.MpvPlaybackEngine)
                ?.setSuperResolution(index)
        },
        hKeyframeLabel = stringResource(R.string.player_h_keyframe),
        isHKeyframesEnabled = SettingsRepository.hKeyframesEnable,
        hKeyframeOptions = hKeyframes?.keyframes.orEmpty().mapIndexed { index, keyframe ->
            stringResource(
                R.string.player_keyframe_option,
                index + 1,
                formatPlaybackTime(keyframe.position),
            )
        },
        hKeyframes = hKeyframes?.keyframes.orEmpty(),
        isHKeyframeLocal = hKeyframes?.author == null,
        onHKeyframeSelected = { index ->
            hKeyframes?.keyframes?.getOrNull(index)?.position?.let(playbackController::seekTo)
        },
        onHKeyframeUpdated = { oldKeyframe, newKeyframe ->
            viewModel.modifyHKeyframe(route.videoCode, oldKeyframe, newKeyframe)
        },
        onHKeyframeDeleted = { keyframe ->
            viewModel.removeHKeyframe(route.videoCode, keyframe)
        },
        onHKeyframeLongPress = {
            if (playbackState.engine.isPlaying) {
                SonnerToast.info(R.string.pause_then_long_press)
            } else {
                showAddHKeyframeDialog = playbackState.engine.positionMs to videoTitle.ifBlank {
                    activity.getString(R.string.player_untitled_video)
                }
            }
        },
        onLongPressStart = {
            if (playbackState.engine.isPlaying) {
                val currentSpeed = playbackState.engine.playbackSpeed
                speedBeforeLongPress = currentSpeed
                playbackController.setPlaybackSpeed(
                    (currentSpeed * SettingsRepository.longPressSpeedTime).coerceAtMost(5f)
                )
            }
        },
        onLongPressEnd = {
            speedBeforeLongPress?.let(playbackController::setPlaybackSpeed)
            speedBeforeLongPress = null
        },
        onVolumeChange = { value ->
            volume = value
            playbackController.setVolume(value)
        },
        onBrightnessChange = { value ->
            brightness = value
            if (previousScreenBrightness == null) {
                previousScreenBrightness = activity.window.attributes.screenBrightness
            }
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = value.coerceIn(0.01f, 1f)
            }
        },
        onProgressGesture = { value ->
            val duration = playbackState.engine.durationMs
            if (duration > 0L) playbackController.seekTo((duration * value).toLong())
        },
        progressGestureSensitivity = realProgressSensitivity(SettingsRepository.slideSensitivity),
        countdownLabel = countdownLabel,
        videoAspectRatio = if (
            playbackState.engine.videoWidth > 0 &&
            playbackState.engine.videoHeight > 0
        ) {
            playbackState.engine.videoWidth.toFloat() / playbackState.engine.videoHeight.toFloat()
        } else {
            16f / 9f
        },
        onPlayerBoundsChanged = { playerBounds = it },
        tabsContent = {
            VideoRouteContent(
                videoCode = route.videoCode,
                videoState = videoState,
                videoViewModel = viewModel,
                commentViewModel = commentViewModel,
                fromDownload = viewModel.fromDownload,
                pendingDownloadPrompt = pendingDownloadPrompt,
                onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                onRetry = { viewModel.getHanimeVideo(route.videoCode, route.localUri) },
                onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
                onOpenArtist = actions::openArtistSearch,
                onNavigateToSearch = actions::openTagSearch,
                onToggleSubscribe = actions::toggleArtistSubscription,
                onToggleFavorite = actions::toggleFavorite,
                onRateVideo = actions::rateVideo,
                onManageMyList = actions::updateMyListSelection,
                onQuickCheckIn = actions::quickCheckIn,
                onPrepareDownload = { quality, item ->
                    checkedQuality = quality
                    item?.let(actions::startDownloadFlow)
                },
                onConfirmDownloadPrompt = { item, autoCreateGroup ->
                    item?.let {
                        actions.confirmPendingDownload(
                            it,
                            pendingDownloadPrompt,
                            autoCreateGroup,
                        )
                    }
                },
                onRequestOpenOfficialDownloadPage = actions::openOfficialDownloadPage,
                onOpenWebPage = actions::openVideoWebPage,
                onOpenOriginalComic = actions::openOriginalComic,
                onOpenShare = shareText,
                onCopyText = {
                    copyTextToClipboard(it)
                    SonnerToast.success(R.string.copy_to_clipboard)
                },
                onIntroductionLinkClick = actions::openIntroductionLink,
                stringLongPressShare = stringLongPressShare,
                pageHost = pageHost,
            )
        },
        classicTabletLayout = if (
            appSettings.tabletMode &&
            appSettings.videoLandscapeLayoutStyle == VideoLandscapeLayoutStyle.Classic
        ) {
            ClassicTabletLayoutConfig(
                relatedItems = relatedItems,
                onHideRelatedInIntroChange = { viewModel.hideRelatedInIntro = it },
                onSideRelatedCollapsedChange = { isSideRelatedCollapsed = it },
                onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
            )
        } else {
            null
        },
        modifier = Modifier.fillMaxSize(),
    )

    showAddHKeyframeDialog?.let { (currentPosition, title) ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.add_to_h_keyframe),
            message = buildString {
                appendLine(activity.getString(R.string.sure_to_add_to_h_keyframe))
                append(activity.getString(R.string.current_position_d_ms, currentPosition))
            },
            confirmText = activity.getString(R.string.confirm),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                viewModel.appendHKeyframe(
                    route.videoCode,
                    title,
                    HKeyframeEntity.Keyframe(position = currentPosition, prompt = null),
                )
                showAddHKeyframeDialog = null
            },
            onDismiss = { showAddHKeyframeDialog = null },
        )
    }

    pendingUnsubscribeArtist?.let { artist ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.unsubscribe_artist),
            message = activity.getString(R.string.sure_to_unsubscribe),
            confirmText = activity.getString(R.string.sure),
            dismissText = activity.getString(R.string.no),
            onConfirm = {
                actions.confirmUnsubscribe(artist)
                pendingUnsubscribeArtist = null
            },
            onDismiss = { pendingUnsubscribeArtist = null },
        )
    }

    ConfirmDialog(
        visible = showNotificationPermissionReason,
        title = activity.getString(R.string.allow_post_notification),
        message = activity.getString(R.string.reason_for_download_notification),
        confirmText = activity.getString(R.string.allow),
        dismissText = activity.getString(R.string.deny),
        onConfirm = {
            showNotificationPermissionReason = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismiss = {
            showNotificationPermissionReason = false
            SonnerToast.warning(R.string.msg_deny_download_notification)
        },
    )

    ConfirmDialog(
        visible = pendingPlayback != null,
        title = activity.getString(R.string.warning),
        message = activity.getString(R.string.mobile_data_playback_warning),
        confirmText = activity.getString(R.string.confirm),
        dismissText = activity.getString(R.string.cancel),
        onConfirm = {
            val request = pendingPlayback
            pendingPlayback = null
            mobilePlaybackConfirmed = true
            request?.let {
                playbackController.load(
                    title = it.title,
                    qualities = it.qualities,
                    preferredQuality = it.preferredQuality,
                    artworkUri = it.artworkUri,
                    startPositionMs = it.startPositionMs,
                    playWhenReady = true,
                )
            }
        },
        onDismiss = { pendingPlayback = null },
    )

    if (showDialog) {
        Base64Dialog(onDismiss = { showDialog = false })
    }

    LaunchedEffect(Unit) {
        if (!isX86_64Device) {
            val isFailed = getString() == String(
                Base64.decode("ZmFpbGVk", Base64.DEFAULT),
                Charsets.UTF_8
            )

            when {
                isFailed -> SonnerToast.error(
                    String(
                        Base64.decode(
                            "5qCh6aqM5bSp5rqD77yM6K+35ZCR5byA5Y+R6ICF5Y+N6aaI",
                            Base64.DEFAULT
                        ),
                        Charsets.UTF_8
                    )
                )

                else -> showDialog = !BuildConfig.DEBUG && !svc()
            }
        }
    }
}

@Composable
fun Base64Dialog(
    onDismiss: () -> Unit
) {
    val decodedTitle = remember {
        String(Base64.decode("562+5ZCN5qCh6aqM5aSx6LSl", Base64.DEFAULT), Charsets.UTF_8)
    }
    val decodedContent = remember {
        String(
            Base64.decode(
                "5L2g5LiL6L295Yiw5LqG6KKr56+h5pS555qE5bqU55So44CC5pys5bqU55So5byA5rqQ5YWN6LS55peg5bm/5ZGK77yM5Lil56aB5aKZ5YaF5byV5rWB44CB5pCs6L+Q44CB5YCS5Y2W44CC5aaC5p6c5L2g6K6k5Li66L+Z5piv6K+v5oql77yM6K+35ZCR5byA5Y+R6ICF5Y+N6aaI44CC",
                Base64.DEFAULT
            ), Charsets.UTF_8
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = decodedTitle) },
        text = { Text(text = decodedContent) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    )
}

private fun playbackProgress(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

private fun currentScreenBrightness(activity: MainActivity): Float {
    val overrideBrightness = activity.window.attributes.screenBrightness
    if (overrideBrightness in 0f..1f) return overrideBrightness
    return runCatching {
        Settings.System.getInt(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
        ) / 255f
    }.getOrDefault(0.5f).coerceIn(0.01f, 1f)
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private external fun svc(): Boolean
private external fun getString(): String

private data class PendingPlayback(
    val title: String,
    val qualities: List<PlaybackQuality>,
    val preferredQuality: String?,
    val artworkUri: String?,
    val startPositionMs: Long,
)

private fun isActiveNetworkMetered(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    return connectivityManager?.isActiveNetworkMetered == true
}

private fun realProgressSensitivity(value: Int): Float {
    val clampedValue = value.coerceIn(1, 7)
    return 4f - (clampedValue - 1) * (3.5f / 6f)
}

private const val LARGE_SCREEN_MIN_WIDTH_DP = 600
