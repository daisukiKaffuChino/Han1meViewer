package io.github.daisukikaffuchino.han1meviewer.ui.screen.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.Rational
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import coil.load
import io.github.daisukikaffuchino.han1meviewer.BuildConfig
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.getHanimeVideoLink
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeVideo
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import io.github.daisukikaffuchino.han1meviewer.logic.model.SearchOption
import io.github.daisukikaffuchino.han1meviewer.logic.state.VideoLoadingState
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.bridge.VideoPageHost
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.VideoRoute
import io.github.daisukikaffuchino.han1meviewer.ui.view.video.ExoMediaKernel
import io.github.daisukikaffuchino.han1meviewer.ui.view.video.HJzvdStd
import io.github.daisukikaffuchino.han1meviewer.ui.view.video.HMediaKernel
import io.github.daisukikaffuchino.han1meviewer.ui.view.video.HanimeDataSource
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.CommentViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.VideoViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.util.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.han1meviewer.ui.util.rememberShareText
import io.github.daisukikaffuchino.han1meviewer.util.loadAssetAs
import io.github.daisukikaffuchino.utils.OrientationManager
import io.github.daisukikaffuchino.utils.dp
import io.github.daisukikaffuchino.utils.SonnerToast
import io.github.daisukikaffuchino.utils.startActivity
import io.github.daisukikaffuchino.utils.statusBarHeight
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Suppress("DEPRECATION")
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
    val kernel = remember { HMediaKernel.Type.fromString(Preferences.switchPlayerKernel) }
    val genres = remember(Preferences.baseUrl) {
        loadAssetAs<List<SearchOption>>(
            if (Preferences.baseUrl == io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_URL[3]) {
                "search_options/genre_av.json"
            } else {
                "search_options/genre.json"
            }
        ).orEmpty()
    }
    val player = remember(route.videoCode, route.localUri) {
        createVideoPlayerView(activity)
    }
    val shell = remember(route.videoCode, route.localUri) {
        VideoRouteShell(activity, player)
    }
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val relatedItems =
        viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val previousStatusBarColor = window.statusBarColor
        val previousNavigationBarColor = window.navigationBarColor
        val previousLightStatusBars = controller.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller.isAppearanceLightNavigationBars
        val previousStatusBarContrastEnforced =
            window.isStatusBarContrastEnforced
        val previousNavigationBarContrastEnforced =
            window.isNavigationBarContrastEnforced

        onDispose {
            window.statusBarColor = previousStatusBarColor
            window.navigationBarColor = previousNavigationBarColor
            controller.isAppearanceLightStatusBars = previousLightStatusBars
            controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            previousStatusBarContrastEnforced.let {
                window.isStatusBarContrastEnforced = it
            }
            previousNavigationBarContrastEnforced.let {
                window.isNavigationBarContrastEnforced = it
            }
        }
    }

    SideEffect {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.TRANSPARENT
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }

    commentViewModel.code = route.videoCode
    player.videoCode = route.videoCode
    viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    var checkedQuality by remember(
        route.videoCode,
        route.localUri
    ) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var isSideRelatedCollapsed by remember { mutableStateOf(false) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var pendingUnsubscribeArtist by remember { mutableStateOf<HanimeVideo.Artist?>(null) }
    var showNotificationPermissionReason by remember { mutableStateOf(false) }
    var showWifiWarning by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showNotificationPermissionReason = true
    }

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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
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

    val jzBackCallback = remember(activity) {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!Jzvd.backPress()) {
                    isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    fun setPlayerHeight(height: Int) {
        shell.setPlayerHeight(height)
    }

    fun updatePipAction() {
        if (!activity.isInPictureInPictureMode) return
        val isPlaying = (Jzvd.CURRENT_JZVD?.mediaInterface as? ExoMediaKernel)?.isPlaying == true
        val icon = if (isPlaying) {
            Icon.createWithResource(activity, R.drawable.ic_baseline_pause_24)
        } else {
            Icon.createWithResource(activity, R.drawable.ic_baseline_play_arrow_24)
        }
        val title = if (isPlaying) "Pause Video" else "Play Video"
        val intent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = RemoteAction(icon, title, activity.getString(R.string.play_pause), intent)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
        activity.setPictureInPictureParams(params)
    }

    fun changeScreenNormal() {
        if (player.screen == Jzvd.SCREEN_FULLSCREEN) {
            player.gotoNormalScreen()
        }
    }

    fun changeScreenFullLandscape(orientation: OrientationManager.ScreenOrientation) {
        if (player.screen != Jzvd.SCREEN_FULLSCREEN &&
            System.currentTimeMillis() - Jzvd.lastAutoFullscreenTime > 2000
        ) {
            player.autoFullscreen(orientation)
            Jzvd.lastAutoFullscreenTime = System.currentTimeMillis()
        }
    }

    val pageHost = remember(activity, player, shell, viewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) {
                viewModel.setCommentBadgeCount(count)
            }

            override fun shouldEnterPip(): Boolean {
                return player.state == Jzvd.STATE_PLAYING || player.state == Jzvd.STATE_PAUSE
            }

            override fun enterPipMode() {
                val intent = PendingIntent.getBroadcast(
                    activity,
                    0,
                    Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val icon =
                    Icon.createWithResource(activity, R.drawable.ic_baseline_pause_24)
                val action = RemoteAction(
                    icon,
                    activity.getString(R.string.play_pause),
                    activity.getString(R.string.play_pause),
                    intent,
                )
                val sourceRect = Rect()
                player.getGlobalVisibleRect(sourceRect)
                val params = PictureInPictureParams.Builder()
                    .setSourceRectHint(sourceRect)
                    .setAspectRatio(Rational(16, 9))
                    .setActions(listOf(action))
                    .build()
                activity.enterPictureInPictureMode(params)
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                viewModel.setPipMode(isInPip)
                if (isInPip) {
                    viewModel.setPlayerHeightDp(MATCH_PARENT)
                } else if (Preferences.tabletMode &&
                    activity.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                ) {
                    viewModel.setPlayerHeightDp(350.dp)
                } else {
                    viewModel.setPlayerHeightDp(250.dp)
                }
                setPlayerHeight(viewModel.videoHostUiStateFlow.value.playerHeightDp)
                shell.setTabsVisible(!isInPip)
                player.setControlsVisible(!isInPip)
                if (isInPip) {
                    updatePipAction()
                }
            }

            override fun togglePlayPause() {
                val mediaInterface = player.mediaInterface
                if (mediaInterface.isPlaying) {
                    mediaInterface.pause()
                } else {
                    mediaInterface.start()
                }
                updatePipAction()
            }
        }
    }

    DisposableEffect(
        activity,
        shell,
        player,
        pageHost,
        stringLongPressShare,
        route.videoCode,
        route.localUri
    ) {
        activity.registerCurrentVideoHost(pageHost)
        shell.setTabsHostContent {
            val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
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
                onPrepareDownload = { quality, video ->
                    checkedQuality = quality
                    video?.let(actions::startDownloadFlow)
                },
                onConfirmDownloadPrompt = { video ->
                    video?.let { actions.confirmPendingDownload(it, pendingDownloadPrompt) }
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
        }
        onDispose {
            activity.registerCurrentVideoHost(null)
            player.onVideoStateChanged = null
            player.fullscreenListener = null
            player.onGoHomeClickListener = null
            player.onKeyframeClickListener = null
            player.onKeyframeLongClickListener = null
            shell.clear()
            Jzvd.releaseAllVideos()
        }
    }

    DisposableEffect(lifecycleOwner, activity, player, shell, route.videoCode) {
        val orientationManager = OrientationManager(activity) { orientation ->
            if (!Preferences.tabletMode &&
                Jzvd.CURRENT_JZVD != null &&
                (player.state == Jzvd.STATE_PLAYING || player.state == Jzvd.STATE_PAUSE) &&
                player.screen != Jzvd.SCREEN_TINY &&
                Jzvd.FULLSCREEN_ORIENTATION != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ) {
                if (orientation.isLandscape && player.screen == Jzvd.SCREEN_NORMAL) {
                    changeScreenFullLandscape(orientation)
                } else if (orientation == OrientationManager.ScreenOrientation.PORTRAIT &&
                    player.screen == Jzvd.SCREEN_FULLSCREEN
                ) {
                    changeScreenNormal()
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val progress = player.currentPositionWhenPlaying
                    scope.launch {
                        DatabaseRepo.WatchHistory.updateProgress(route.videoCode, progress)
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (!activity.isInPictureInPictureMode) {
                        changeScreenNormal()
                    }
                    Jzvd.goOnPlayOnPause()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        activity.onBackPressedDispatcher.addCallback(lifecycleOwner, jzBackCallback)
        player.orientationManager = orientationManager
        player.onGoHomeClickListener = {
            if (activity.resources.getBoolean(R.bool.isTablet)) {
                activity.navController.popBackStack()
            }
            activity.startActivity<MainActivity>()
        }
        player.onKeyframeClickListener = { view ->
            player.clickHKeyframe(view)
        }
        player.onKeyframeLongClickListener = {
            val mediaInterface: JZMediaInterface? = player.mediaInterface
            if (mediaInterface != null && !mediaInterface.isPlaying) {
                val currentPosition = player.currentPositionWhenPlaying
                showAddHKeyframeDialog = Pair(currentPosition, videoTitle ?: "Untitled")
            } else {
                SonnerToast.info(R.string.pause_then_long_press)
            }
        }
        player.onWifiWarningRequested = { showWifiWarning = true }
        player.onVideoStateChanged = { state ->
            when (state) {
                Jzvd.STATE_PLAYING, Jzvd.STATE_PREPARING -> {
                    viewModel.setScrollDisabled(true)
                }

                Jzvd.STATE_PAUSE, Jzvd.STATE_AUTO_COMPLETE -> {
                    viewModel.setScrollDisabled(false)
                }
            }
        }
        player.fullscreenListener = object : HJzvdStd.FullscreenListener {
            override fun onFullscreenChanged(isFullscreen: Boolean) {
                jzBackCallback.isEnabled = isFullscreen
                Log.i("JZVD screen state", isFullscreen.toString())
            }
        }
        val initialHeight = if (Preferences.tabletMode) {
            350.dp
        } else {
            250.dp
        }
        viewModel.setPlayerHeightDp(initialHeight)
        setPlayerHeight(initialHeight)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.onWifiWarningRequested = null
        }
    }

    LaunchedEffect(
        hostUiState.isInPipMode,
        isSideRelatedCollapsed,
    ) {
        if (hostUiState.isInPipMode) return@LaunchedEffect
        val height = if (Preferences.tabletMode) {
            if (isSideRelatedCollapsed) 500.dp else 400.dp
        } else {
            250.dp
        }
        if (hostUiState.playerHeightDp != height) {
            viewModel.setPlayerHeightDp(height)
            setPlayerHeight(height)
        }
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        videoTitle = null
        viewModel.videoCode = route.videoCode
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(route.videoCode, route.localUri, player, kernel, viewModel.fromDownload) {
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
                        videoTitle = state.info.title
                        if (state.info.videoUrls.isEmpty()) {
                            player.startButton.setOnClickListener {
                                SonnerToast.error(R.string.fail_to_get_video_link)
                                uriHandler.openUri(getHanimeVideoLink(route.videoCode))
                            }
                        } else {
                            player.setUp(
                                HanimeDataSource(state.info.title, state.info.videoUrls),
                                Jzvd.SCREEN_NORMAL,
                                kernel,
                            )
                        }
                        player.posterImageView.load(state.info.coverUrl) {
                            crossfade(true)
                        }
                        if (!viewModel.fromDownload) {
                            viewModel.insertWatchHistoryWithCover(
                                WatchHistoryEntity(
                                    state.info.coverUrl,
                                    state.info.title,
                                    state.info.uploadTimeMillis,
                                    kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                    route.videoCode,
                                )
                            )
                        }
                        val history = DatabaseRepo.WatchHistory.findBy(route.videoCode)
                        player.savedProgress = history?.progress ?: 0L
                    }

                    is VideoLoadingState.NoContent -> {
                        SonnerToast.error(R.string.video_might_not_exist)
                    }
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
                    "5L2g5Y+v6IO95bey57uP6KKr6aqX5LqG77yM5LiL6L295Yiw5LqG6KKr56+h5pS555qE5bqU55So44CC5pys5bqU55So5byA5rqQ5YWN6LS55peg5bm/5ZGK77yM5Lil56aB5aKZ5YaF5byV5rWB44CB5pCs6L+Q44CB5YCS5Y2W44CC5Lmf5pyJ5bCP5qaC546H5piv5qCh6aqM562+5ZCN55qE5Luj56CB5Ye66ZSZ5LqG77yM5aaC5p6c5L2g5piv5Zyo5a6Y5pa5R2l0aHVi5LuT5bqT5LiL6L2955qE77yM6K+36IGU57O75byA5Y+R6ICF44CC",
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

    if (showDialog) {
        Base64Dialog(onDismiss = { showDialog = false })
    }

    LaunchedEffect(Unit) {
        if (getString() == String(
                Base64.decode(
                    "ZmFpbGVk",
                    Base64.DEFAULT
                ), Charsets.UTF_8
            )
        )
            SonnerToast.error(
                String(
                    Base64.decode(
                        "562+5ZCN5qCh6aqM5bSp5rqD77yM5peg5rOV6aqM6K+B5piv5ZCm6KKr56+h5pS577yM6K+36IGU57O75byA5Y+R6ICF",
                        Base64.DEFAULT
                    ), Charsets.UTF_8
                )
            )
        else
            showDialog = !BuildConfig.DEBUG && !svc()
    }

    @OptIn(ExperimentalTime::class)
    LaunchedEffect(route.videoCode, player) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.observeKeyframe(route.videoCode).collect {
                player.hKeyframe = it
                viewModel.hKeyframes = it
            }
        }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyHKeyframeFlow.collect { (_, reason) ->
                SonnerToast.info(reason)
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
                )
            }
        }
    }

    VideoShellContent(
        isTabletMode = Preferences.tabletMode,
        isInPipMode = hostUiState.isInPipMode,
        relatedItems = relatedItems,
        onHideRelatedInIntroChange = { viewModel.hideRelatedInIntro = it },
        onSideRelatedCollapsedChange = { isSideRelatedCollapsed = it },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        mainHostFactory = {
            shell.mainHostView.also { view ->
                (view.parent as? ViewGroup)?.removeView(view)
            }
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
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        },
        onDismiss = {
            showNotificationPermissionReason = false
            SonnerToast.warning(R.string.msg_deny_download_notification)
        },
    )

    ConfirmDialog(
        visible = showWifiWarning,
        title = activity.getString(R.string.warning),
        message = activity.getString(cn.jzvd.R.string.tips_not_wifi),
        confirmText = activity.getString(cn.jzvd.R.string.tips_not_wifi_confirm),
        dismissText = activity.getString(cn.jzvd.R.string.tips_not_wifi_cancel),
        onConfirm = {
            showWifiWarning = false
            player.confirmWifiPlayback()
        },
        onDismiss = {
            showWifiWarning = false
            player.cancelWifiPlayback()
        },
    )
}

private external fun svc(): Boolean
private external fun getString(): String

private fun createVideoPlayerView(activity: MainActivity): HJzvdStd {
    return HJzvdStd(ContextThemeWrapper(activity, activity.theme))
}

private class VideoRouteShell(
    context: Context,
    private val playerView: HJzvdStd,
) {
    private val rootView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        fitsSystemWindows = false
        layoutParams = ViewGroup.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
        )
    }

    private val statusBarSpacer = View(context).apply {
        setBackgroundColor(Color.BLACK)
        layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            statusBarHeight,
        )
    }

    private val videoPlayerHost = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT,
        )
    }

    private val videoTabsHost = ComposeView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            0,
            1f,
        )
    }

    init {
        videoPlayerHost.addView(playerView, ViewGroup.LayoutParams(MATCH_PARENT, 250.dp))
        rootView.addView(statusBarSpacer)
        rootView.addView(videoPlayerHost)
        rootView.addView(videoTabsHost)
    }

    val mainHostView: View
        get() = rootView

    fun setTabsHostContent(content: @Composable () -> Unit) {
        videoTabsHost.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        videoTabsHost.setContent(content)
    }

    fun setTabsVisible(visible: Boolean) {
        videoTabsHost.isVisible = visible
    }

    fun setPlayerHeight(height: Int) {
        val lp = playerView.layoutParams
        lp.height = height
        playerView.layoutParams = lp
        playerView.requestLayout()
    }

    fun clear() {
        videoTabsHost.disposeComposition()
    }
}
