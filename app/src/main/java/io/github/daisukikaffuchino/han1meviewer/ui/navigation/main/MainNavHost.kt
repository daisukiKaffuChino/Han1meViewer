package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.navigateSafely
import io.github.daisukikaffuchino.han1meviewer.ui.screen.account.AvatarCropScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.CreatorCenterScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframesRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HKeyframesTopBarActions
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HomeSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SettingsScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.account.AccountScreen
import io.github.daisukikaffuchino.han1meviewer.ui.theme.materialSharedAxisXIn
import io.github.daisukikaffuchino.han1meviewer.ui.theme.materialSharedAxisXOut
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.UserAccountViewModel
import kotlinx.serialization.json.Json

@Composable
fun MainNavHost(
    activity: MainActivity,
    navController: NavHostController,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationSpec = MainDestinationSpec.fromDestination(backStackEntry?.destination)
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { navController.popBackStack() }
    val onNavigateToVideo: (String) -> Unit = { code -> navController.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> navController.navigateSafely(VideoRoute(code, uri)) }

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { materialSharedAxisXIn(initialOffsetX = { it }) },
        exitTransition = { materialSharedAxisXOut(targetOffsetX = { -it }) },
        popEnterTransition = { materialSharedAxisXIn(initialOffsetX = { -it }) },
        popExitTransition = { materialSharedAxisXOut(targetOffsetX = { it }) },
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                isDrawerOpen = isDrawerOpen,
                onOpenDrawer = onOpenDrawer,
                onNavigateToPreview = { navController.navigateSafely(PreviewRoute) },
                onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { params ->
                    navController.navigateSafely(
                        SearchRoute(advancedSearchJson = Json.encodeToString(params))
                    )
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyFavVideoRoute> {
            FavVideoRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyWatchLaterRoute> {
            WatchLaterRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = onBack,
                onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<DownloadRoute> {
            DownloadRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        composable<CreatorCenterRoute> {
            val creatorViewModel: CreatorCenterViewModel = viewModel()
            CreatorCenterScreen(
                viewModel = creatorViewModel,
                onBack = onBack,
                onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
            )
        }
        composable<AccountRoute> {
            val accountViewModel: UserAccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onBack = onBack,
                onOpenAvatarCrop = { sourceUri ->
                    navController.navigateSafely(AvatarCropRoute(sourceUri))
                },
                pendingAvatarCropResult = pendingAvatarCropResult,
                onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                onRefreshHome = { activity.viewModel.getHomePage() },
                onLogout = { activity.showLogoutConfirmDialog(closeCurrentPageOnConfirm = true) },
            )
        }
        composable<AvatarCropRoute> {
            val route = it.toRoute<AvatarCropRoute>()
            AvatarCropScreen(
                sourceUri = route.sourceUri,
                onBack = onBack,
                onConfirm = { file ->
                    pendingAvatarCropResult = file.absolutePath
                    onBack()
                },
            )
        }
        composable<HomeSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeRoute,
            ) {
                HomeSettingsRouteScreen(
                    activity = activity,
                    onNavigateToPlayerSettings = { navController.navigateSafely(PlayerSettingsRoute) },
                    onNavigateToHKeyframeSettings = { navController.navigateSafely(HKeyframeSettingsRoute) },
                    onNavigateToDownloadSettings = { navController.navigateSafely(DownloadSettingsRoute) },
                    onNavigateToNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                )
            }
        }
        composable<PlayerSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                PlayerSettingsRouteScreen(
                    onNavigateToMpvSettings = { navController.navigateSafely(MpvPlayerSettingsRoute) },
                )
            }
        }
        composable<NetworkSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                NetworkSettingsRouteScreen()
            }
        }
        composable<DownloadSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                DownloadSettingsRouteScreen()
            }
        }
        composable<MpvPlayerSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = PlayerSettingsRoute,
            ) {
                MpvPlayerSettingsRouteScreen()
            }
        }
        composable<HKeyframesRoute> {
            var showImportDialog by remember { mutableStateOf(false) }
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HKeyframeSettingsRoute,
                actions = {
                    HKeyframesTopBarActions(onImportClick = { showImportDialog = true })
                },
            ) {
                HKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                    showImportDialog = showImportDialog,
                    onImportDialogDismiss = { showImportDialog = false },
                )
            }
        }
        composable<SharedHKeyframesRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HKeyframeSettingsRoute,
            ) {
                SharedHKeyframesRouteScreen(
                    onOpenVideo = onNavigateToVideo,
                )
            }
        }
        composable<HKeyframeSettingsRoute> {
            SettingsScaffold(
                navController = navController,
                fallbackDestination = HomeSettingsRoute,
            ) {
                HKeyframeSettingsRouteScreen(
                    onNavigateToHKeyframes = { navController.navigateSafely(HKeyframesRoute) },
                    onNavigateToSharedHKeyframes = { navController.navigateSafely(SharedHKeyframesRoute) },
                )
            }
        }
        composable<SearchRoute> {
            SearchRouteScreen(
                route = it.toRoute(),
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<PreviewRoute> {
            PreviewRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToGetchuPreview = {
                    navController.navigateSafely(GetchuPreviewRoute)
                },
                onNavigateToPreviewComment = { date, dateCode ->
                    navController.navigateSafely(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<GetchuPreviewRoute> {
            GetchuPreviewRouteScreen(
                onBack = onBack,
                onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
            )
        }
        composable<GetchuPreviewDetailRoute> {
            GetchuPreviewDetailRouteScreen(
                route = it.toRoute(),
                onBack = onBack,
                onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
                onNavigateToVideoUrl = { url -> navController.navigateSafely(VideoRoute("-1", url)) },
            )
        }
        composable<PreviewCommentRoute> {
            PreviewCommentRouteScreen(
                activity = activity,
                route = it.toRoute(),
                onBack = onBack,
            )
        }
        composable<VideoRoute> {
            VideoRouteScreen(
                activity = activity,
                route = it.toRoute(),
            )
        }
    }
}
