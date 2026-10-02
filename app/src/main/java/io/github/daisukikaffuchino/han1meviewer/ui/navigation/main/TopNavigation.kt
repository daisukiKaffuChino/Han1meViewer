package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.IconButton
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.AboutSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.AppearanceSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DataPrivacySettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DeveloperOptionsSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.EchTestRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.EchTestSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframesRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HomeSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.InterfaceInteractionSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.NetworkDownloadSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.OpenSourceLicensesRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SettingsScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.VideoPlaybackSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.screen.account.AccountScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.account.AvatarCropScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.HomeSettingsPage
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.OpenSourceLicensesScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.SettingsMainScreen
import io.github.daisukikaffuchino.han1meviewer.ui.theme.fadeScale
import io.github.daisukikaffuchino.han1meviewer.ui.theme.materialSharedAxisX
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.UserAccountViewModel
import io.github.daisukikaffuchino.utils.VibrationUtil
import kotlinx.serialization.json.Json

private const val PageTransitionOffsetFactor = 0.10f

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopNavigation(
    activity: MainActivity,
    backStack: TopLevelBackStack<HanimeScreen>,
    isDrawerOpen: Boolean,
    showHomeNavigationIcon: Boolean,
    homeContentStartPadding: Dp,
    onOpenDrawer: () -> Unit,
) {
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { backStack.removeLast() }
    val onNavigateToVideo: (String) -> Unit = { code -> backStack.add(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> backStack.add(VideoRoute(code, uri)) }

    fun pageTransition() = NavDisplay.transitionSpec {
        materialSharedAxisX(
            initialOffsetX = { (it * PageTransitionOffsetFactor).toInt() },
            targetOffsetX = { -(it * PageTransitionOffsetFactor).toInt() },
        )
    } + NavDisplay.popTransitionSpec {
        materialSharedAxisX(
            initialOffsetX = { -(it * PageTransitionOffsetFactor).toInt() },
            targetOffsetX = { (it * PageTransitionOffsetFactor).toInt() },
        )
    } + NavDisplay.predictivePopTransitionSpec {
        materialSharedAxisX(
            initialOffsetX = { -(it * PageTransitionOffsetFactor).toInt() },
            targetOffsetX = { (it * PageTransitionOffsetFactor).toInt() },
        )
    }

    fun videoTransition() = NavDisplay.transitionSpec {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    } + NavDisplay.popTransitionSpec {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    } + NavDisplay.predictivePopTransitionSpec {
        ContentTransform(EnterTransition.None, ExitTransition.None)
    }

    val defaultTransition = fadeScale(
        effectSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    )

    SharedTransitionLayout {
    NavDisplay(
        backStack = backStack.backStack,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { defaultTransition },
        popTransitionSpec = { defaultTransition },
        predictivePopTransitionSpec = { defaultTransition },
        entryProvider = entryProvider {
        entry<HomeRoute> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = homeContentStartPadding),
            ) {
                HomeRouteScreen(
                    activity = activity,
                    isDrawerOpen = isDrawerOpen,
                    showNavigationIcon = showHomeNavigationIcon,
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToPreview = { backStack.add(PreviewRoute) },
                    onNavigateToSearch = { query -> backStack.add(SearchRoute(query = query)) },
                    onNavigateToSearchAdvanced = { params ->
                        backStack.add(
                            SearchRoute(advancedSearchJson = Json.encodeToString(params))
                        )
                    },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
        }
        entry<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<MyFavVideoRoute> {
            FavVideoRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<MyWatchLaterRoute> {
            WatchLaterRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = onBack,
                onNavigateToSearch = { query -> backStack.add(SearchRoute(query = query)) },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<DailyCheckInRoute> {
            DailyCheckInRouteScreen(
                activity = activity,
                onBack = onBack,
            )
        }
        entry<DownloadRoute> {
            DownloadRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        entry<AccountRoute>(metadata = pageTransition()) {
            val accountViewModel: UserAccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onBack = onBack,
                onOpenAvatarCrop = { sourceUri ->
                    backStack.add(AvatarCropRoute(sourceUri))
                },
                pendingAvatarCropResult = pendingAvatarCropResult,
                onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                onRefreshHome = { activity.viewModel.getHomePage() },
                onLogout = { activity.showLogoutConfirmDialog(closeCurrentPageOnConfirm = true) },
            )
        }
        entry<LoginRoute>(metadata = pageTransition()) {
            LoginRouteScreen(
                activity = activity,
                onBack = onBack,
                onOpenManualCookies = { backStack.add(ManualCookiesRoute) },
                onLoginSucceeded = {
                    backStack.popTo(LoginRoute, inclusive = true)
                    activity.viewModel.getHomePage()
                },
            )
        }
        entry<ManualCookiesRoute>(metadata = pageTransition()) {
            ManualCookiesRouteScreen(
                onBack = onBack,
                onLoginSucceeded = {
                    backStack.popTo(LoginRoute, inclusive = true)
                    activity.viewModel.getHomePage()
                },
            )
        }
        entry<CloudflareRoute>(metadata = pageTransition()) { route ->
            CloudflareRouteScreen(
                activity = activity,
                route = route,
                onBack = onBack,
            )
        }
        entry<AvatarCropRoute>(metadata = pageTransition()) { route ->
            AvatarCropScreen(
                sourceUri = route.sourceUri,
                onBack = onBack,
                onConfirm = { file ->
                    pendingAvatarCropResult = file.absolutePath
                    onBack()
                },
            )
        }
        entry<HomeSettingsRoute> {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Home,
                fallbackDestination = HomeRoute,
            ) {
                SettingsMainScreen(
                    onOpenVideoPlayback = { backStack.add(VideoPlaybackSettingsRoute) },
                    onOpenPlayerSettings = { backStack.add(PlayerSettingsRoute) },
                    onOpenNetworkDownload = { backStack.add(NetworkDownloadSettingsRoute) },
                    onOpenAppearance = { backStack.add(AppearanceSettingsRoute) },
                    onOpenInterfaceInteraction = {
                        backStack.add(InterfaceInteractionSettingsRoute)
                    },
                    onOpenDataPrivacy = { backStack.add(DataPrivacySettingsRoute) },
                    onOpenDeveloperOptions = { backStack.add(DeveloperOptionsSettingsRoute) },
                    onOpenAbout = { backStack.add(AboutSettingsRoute) },
                )
            }
        }
        entry<VideoPlaybackSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.VideoPlayback,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.VideoPlayback,
                    onNavigateToHKeyframes = { backStack.add(HKeyframesRoute) },
                    onNavigateToSharedHKeyframes = { backStack.add(SharedHKeyframesRoute) },
                )
            }
        }
        entry<NetworkDownloadSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.NetworkDownload,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.NetworkDownload,
                    onNavigateToEchTest = { backStack.add(EchTestSettingsRoute) },
                )
            }
        }
        entry<AppearanceSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Appearance,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.Appearance,
                )
            }
        }
        entry<InterfaceInteractionSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.InterfaceInteraction,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.InterfaceInteraction,
                )
            }
        }
        entry<DataPrivacySettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.DataPrivacy,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.DataPrivacy,
                )
            }
        }
        entry<DeveloperOptionsSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.DeveloperOptions,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.DeveloperOptions,
                )
            }
        }
        entry<AboutSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.About,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    page = HomeSettingsPage.About,
                    onNavigateToOpenSourceLicenses = {
                        backStack.add(OpenSourceLicensesRoute)
                    },
                )
            }
        }
        entry<OpenSourceLicensesRoute>(metadata = pageTransition()) {
            var searchMode by remember { mutableStateOf(false) }
            BackHandler(enabled = searchMode) {
                searchMode = false
            }
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.OpenSourceLicenses,
                fallbackDestination = AboutSettingsRoute,
                onNavigateBack = {
                    if (searchMode) {
                        searchMode = false
                        true
                    } else {
                        false
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = !searchMode,
                        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                            scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                            scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                    ) {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = { searchMode = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                    }
                },
            ) {
                OpenSourceLicensesScreen(
                    searchMode = searchMode,
                )
            }
        }
        entry<PlayerSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Player,
                fallbackDestination = HomeSettingsRoute,
            ) {
                PlayerSettingsRouteScreen(
                    onNavigateToMpvSettings = { backStack.add(MpvPlayerSettingsRoute) },
                )
            }
        }
        entry<NetworkSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Network,
                fallbackDestination = NetworkDownloadSettingsRoute,
            ) {
                NetworkSettingsRouteScreen(
                    onOpenEchTest = { backStack.add(EchTestSettingsRoute) },
                )
            }
        }
        entry<DownloadSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Download,
                fallbackDestination = NetworkDownloadSettingsRoute,
            ) {
                DownloadSettingsRouteScreen()
            }
        }
        entry<EchTestSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.EchTest,
                fallbackDestination = NetworkDownloadSettingsRoute,
            ) {
                EchTestRouteScreen()
            }
        }
        entry<MpvPlayerSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.Mpv,
                fallbackDestination = PlayerSettingsRoute,
            ) {
                MpvPlayerSettingsRouteScreen()
            }
        }
        entry<HKeyframesRoute>(metadata = pageTransition()) {
            var showImportDialog by remember { mutableStateOf(false) }
            val view = LocalView.current
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.HKeyframes,
                fallbackDestination = VideoPlaybackSettingsRoute,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            VibrationUtil.performHapticFeedback(view)
                            showImportDialog = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.h_keyframes_import_shared),
                        )
                    }
                },
            ) {
                HKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                    showImportDialog = showImportDialog,
                    onImportDialogDismiss = { showImportDialog = false },
                )
            }
        }
        entry<SharedHKeyframesRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.SharedHKeyframes,
                fallbackDestination = VideoPlaybackSettingsRoute,
            ) {
                SharedHKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                )
            }
        }
        entry<HKeyframeSettingsRoute>(metadata = pageTransition()) {
            SettingsScaffold(
                backStack = backStack,
                destination = SettingsDestinationSpec.HKeyframeSettings,
                fallbackDestination = VideoPlaybackSettingsRoute,
            ) {
                HKeyframeSettingsRouteScreen(
                    onNavigateToHKeyframes = { backStack.add(HKeyframesRoute) },
                    onNavigateToSharedHKeyframes = { backStack.add(SharedHKeyframesRoute) },
                )
            }
        }
        entry<SearchRoute>(metadata = pageTransition()) { route ->
            SearchRouteScreen(
                route = route,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<PreviewRoute>(metadata = pageTransition()) {
            PreviewRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToGetchuPreview = {
                    backStack.add(GetchuPreviewRoute)
                },
                onNavigateToPreviewComment = { date, dateCode ->
                    backStack.add(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        entry<GetchuPreviewRoute>(metadata = pageTransition()) {
            GetchuPreviewRouteScreen(
                onBack = onBack,
                onNavigateToDetail = { id -> backStack.add(GetchuPreviewDetailRoute(id)) },
            )
        }
        entry<GetchuPreviewDetailRoute>(metadata = pageTransition()) { route ->
            GetchuPreviewDetailRouteScreen(
                route = route,
                onBack = onBack,
                onNavigateToDetail = { id -> backStack.add(GetchuPreviewDetailRoute(id)) },
                onNavigateToVideoUrl = { url -> backStack.add(VideoRoute("-1", url)) },
            )
        }
        entry<PreviewCommentRoute>(metadata = pageTransition()) { route ->
            PreviewCommentRouteScreen(
                activity = activity,
                route = route,
                onBack = onBack,
            )
        }
        entry<VideoRoute>(metadata = videoTransition()) { route ->
            VideoRouteScreen(
                activity = activity,
                route = route,
            )
        }
        },
    )
    }
}
