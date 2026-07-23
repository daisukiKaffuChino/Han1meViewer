package io.github.daisukikaffuchino.han1meviewer.ui.screen.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.HCacheManager
import io.github.daisukikaffuchino.han1meviewer.logic.exception.CloudFlareBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageState
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.UsageNoticeDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.MainDestinationSpec
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.MainNavHost
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.handleMainIntent
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.navigateDrawerDestination
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.HomePageViewModel
import io.github.daisukikaffuchino.han1meviewer.videoUrlRegex
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun MainActivityContent(
    activity: MainActivity,
    viewModel: HomePageViewModel,
    pendingNavigationRequests: Flow<Intent>,
    showAuthGuard: Boolean,
    showSiteSwitchConfirm: Boolean,
    logoutDialogCloseCurrentPage: Boolean?,
    onOpenAccount: () -> Unit,
    onLogoutClick: () -> Unit,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onDismissSiteSwitch: () -> Unit,
    onConfirmSiteSwitch: () -> Unit,
    onDismissLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onOpenClipboardVideo: (String) -> Unit,
    onNavigateControllerReady: (NavHostController) -> Unit,
) {
        val composeNavController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboard.current
        val snackbarHostState = remember { SnackbarHostState() }
        var currentMainDestination by remember { mutableStateOf(MainDestinationSpec.Home) }
        var showUsageNotice by remember { mutableStateOf(!Preferences.usageNoticeAccepted) }
        val isDrawerOpen =
            drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open

        val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
        val showStorageSwitchNotice by HCacheManager.storageSwitchNotice.collectAsStateWithLifecycle()
        val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
        val checkInEnabled by Preferences.checkInEnabledFlow.collectAsStateWithLifecycle()
        val headerAvatarUrl = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.avatarUrl
        } else {
            null
        }
        val headerUsername = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.username
        } else {
            null
        }
        val headerIsLoading = isLoggedIn && homeState is PageState.Loading
        val selectedDrawerDestination = currentMainDestination.drawerDestination

        LaunchedEffect(composeNavController) {
            onNavigateControllerReady(composeNavController)
        }
        LaunchedEffect(Unit) {
            val clipboardText = clipboard.getClipEntry()
                ?.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(activity)
            val videoCode = clipboardText?.let { videoUrlRegex.find(it)?.groupValues?.get(1) }
            if (videoCode != null) {
                val result = snackbarHostState.showSnackbar(
                    message = activity.getString(R.string.detect_ha1_related_link_in_clipboard),
                    actionLabel = activity.getString(R.string.enter),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onOpenClipboardVideo(videoCode)
                }
            }
        }
        LaunchedEffect(Unit) {
            pendingNavigationRequests.collect { intent ->
                composeNavController.handleMainIntent(intent)
            }
        }
        LaunchedEffect(viewModel) {
            viewModel.sessionExpiredMessage.collect { event ->
                event.message?.let(SonnerToast::error) ?: SonnerToast.error(event.fallbackResId)
            }
        }
        LaunchedEffect(homeState) {
            if (homeState is PageState.Error) {
                val throwable = (homeState as PageState.Error).throwable
                if (throwable is CloudFlareBlockedException) {
                    Log.e("error", "被屏蔽时的处理")
                }
            }
        }
        MainActivityScaffold(
            drawerState = drawerState,
            drawerEnabled = currentMainDestination.drawerEnabled,
            selectedDestination = selectedDrawerDestination,
            avatarUrl = headerAvatarUrl,
            username = headerUsername,
            isLoggedIn = isLoggedIn,
            isLoading = headerIsLoading,
            currentSite = Preferences.baseUrl,
            checkInEnabled = checkInEnabled,
            onAvatarClick = {
                if (isLoggedIn) {
                    scope.launch { drawerState.close() }
                    onOpenAccount()
                } else {
                    onRequireLogin()
                }
            },
            onAvatarLongClick = {
                onLogoutClick()
            },
            onSwitchSiteClick = onSwitchSiteClick,
            onDrawerItemSelected = { destination ->
                val handled = composeNavController.navigateDrawerDestination(
                    destination = destination,
                    isLoggedIn = isLoggedIn,
                    onRequireLogin = { SonnerToast.warning(R.string.login_first) },
                )
                if (handled) {
                    scope.launch { drawerState.close() }
                }
                handled
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    activity = activity,
                    navController = composeNavController,
                    isDrawerOpen = isDrawerOpen,
                    onOpenDrawer = {
                        if (currentMainDestination.drawerEnabled) {
                            scope.launch { drawerState.open() }
                        }
                    },
                    onDestinationChanged = { destination ->
                        currentMainDestination = destination
                    },
                )
                if (showAuthGuard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }
                UsageNoticeDialog(
                    visible = showUsageNotice,
                    onAccepted = {
                        Preferences.usageNoticeAccepted = true
                        showUsageNotice = false
                    },
                    onDeclined = { activity.finish() },
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
        ConfirmDialog(
            visible = showSiteSwitchConfirm,
            title = stringResource(R.string.confirm_switch_site),
            message = "",
            confirmText = stringResource(R.string.sure),
            dismissText = stringResource(R.string.no),
            onConfirm = onConfirmSiteSwitch,
            onDismiss = onDismissSiteSwitch,
        )
        ConfirmDialog(
            visible = logoutDialogCloseCurrentPage != null,
            title = stringResource(R.string.sure_to_logout),
            message = "",
            confirmText = stringResource(R.string.sure),
            dismissText = stringResource(R.string.no),
            onConfirm = onConfirmLogout,
            onDismiss = onDismissLogout,
        )
        ConfirmDialog(
            visible = showStorageSwitchNotice,
            title = stringResource(R.string.save_failed_title),
            message = stringResource(R.string.save_failed_message),
            confirmText = stringResource(R.string.understood),
            dismissText = null,
            onConfirm = HCacheManager::dismissStorageSwitchNotice,
            onDismiss = HCacheManager::dismissStorageSwitchNotice,
        )
}
