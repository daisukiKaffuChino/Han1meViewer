package io.github.daisukikaffuchino.han1meviewer.ui.screen.main

import android.content.Intent
import android.content.res.Configuration
import io.github.daisukikaffuchino.utils.LogUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.HCacheManager
import io.github.daisukikaffuchino.han1meviewer.logic.state.HanimeErrorKind
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.UsageNoticeDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.HomeRoute
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.MainDrawerDestination
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.TopNavigation
import io.github.daisukikaffuchino.han1meviewer.ui.navigation.main.VideoRoute
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
) {
    val backStack = viewModel.mainBackStack
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showUsageNotice by remember { mutableStateOf(!SettingsRepository.usageNoticeAccepted) }
    var showSourceDialog by remember {
        mutableStateOf(
            SettingsRepository.usageNoticeAccepted &&
                    !SettingsRepository.usageSourceVerified &&
                    !SettingsRepository.usageSourcePending,
        )
    }
    var showSourceWarning by rememberSaveable {
        mutableStateOf(
            SettingsRepository.usageNoticeAccepted &&
                    !SettingsRepository.usageSourceVerified &&
                    SettingsRepository.usageSourcePending,
        )
    }
    var sourceLink by rememberSaveable { mutableStateOf("") }
    var appAccessGranted by remember {
        mutableStateOf(SettingsRepository.usageNoticeAccepted && SettingsRepository.usageSourceVerified)
    }
    val isDrawerOpen =
        drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open

    val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
    val showStorageSwitchNotice by HCacheManager.storageSwitchNotice.collectAsStateWithLifecycle()
    val isLoggedIn by SettingsRepository.loginStateFlow.collectAsStateWithLifecycle()
    val checkInEnabled by SettingsRepository.checkInEnabledFlow.collectAsStateWithLifecycle()
    val headerAvatarUrl = if (isLoggedIn) {
        (homeState as? PagedUiState.Success)?.data?.page?.avatarUrl
    } else {
        null
    }
    val headerUsername = if (isLoggedIn) {
        (homeState as? PagedUiState.Success)?.data?.page?.username
    } else {
        null
    }
    val headerIsLoading = isLoggedIn && homeState is PagedUiState.Loading
    val currentRoute = backStack.currentKey
    val previousRoute = backStack.backStack.getOrNull(backStack.backStack.lastIndex - 1)
    val selectedDrawerDestination = MainDrawerDestination.fromRoute(backStack.topLevelKey)
    val drawerEnabled = currentRoute == HomeRoute
    val permanentDrawer = (drawerEnabled || previousRoute == HomeRoute) &&
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(permanentDrawer) {
        if (permanentDrawer) drawerState.close()
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
            backStack.handleMainIntent(intent)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.sessionExpiredMessage.collect { event ->
            event.message?.let(SonnerToast::error) ?: SonnerToast.error(event.fallbackResId)
        }
    }
    LaunchedEffect(homeState) {
        (homeState as? PagedUiState.Error)?.let { errorState ->
            if (errorState.error.kind == HanimeErrorKind.CloudflareChallenge) {
                LogUtil.e("error", "被屏蔽时的处理")
            }
        }
    }
    MainActivityScaffold(
        drawerState = drawerState,
        drawerEnabled = drawerEnabled,
        permanentDrawer = permanentDrawer,
        applyHorizontalSafeInsets = currentRoute !is VideoRoute,
        selectedDestination = selectedDrawerDestination,
        avatarUrl = headerAvatarUrl,
        username = headerUsername,
        isLoggedIn = isLoggedIn,
        isLoading = headerIsLoading,
        currentSite = SettingsRepository.baseUrl,
        checkInEnabled = checkInEnabled,
        onAvatarClick = {
            if (isLoggedIn) {
                scope.launch { drawerState.close() }
                onOpenAccount()
            } else {
                scope.launch {
                    drawerState.close()
                    onRequireLogin()
                }
            }
        },
        onAvatarLongClick = {
            onLogoutClick()
        },
        onSwitchSiteClick = onSwitchSiteClick,
        onDrawerItemSelected = { destination ->
            val handled = backStack.navigateDrawerDestination(
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
            if (appAccessGranted) {
                TopNavigation(
                    activity = activity,
                    backStack = backStack,
                    isDrawerOpen = isDrawerOpen && !permanentDrawer,
                    showHomeNavigationIcon = !permanentDrawer,
                    homeContentStartPadding = if (permanentDrawer) 280.dp else 0.dp,
                    onOpenDrawer = {
                        if (drawerEnabled) {
                            scope.launch { drawerState.open() }
                        }
                    },
                )
            }
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
                    scope.launch {
                        SettingsRepository.setUsageNoticeAccepted(true)
                        showUsageNotice = false
                        if (SettingsRepository.usageSourceVerified) {
                            appAccessGranted = true
                            viewModel.initializeHomePage()
                        } else if (SettingsRepository.usageSourcePending) {
                            showSourceWarning = true
                        } else {
                            showSourceDialog = true
                        }
                    }
                },
                onDeclined = { activity.finish() },
            )
            AppSourceDialog(
                visible = showSourceDialog,
                onSelect = { source ->
                    if (source.equals("github", ignoreCase = true)) {
                        scope.launch {
                            SettingsRepository.update {
                                it.copy(
                                    usageSourceVerified = true,
                                    usageSourcePending = false
                                )
                            }
                            showSourceDialog = false
                            appAccessGranted = true
                            viewModel.initializeHomePage()
                        }
                    } else {
                        scope.launch {
                            SettingsRepository.setUsageSourcePending(true)
                            showSourceDialog = false
                            sourceLink = ""
                            showSourceWarning = true
                        }
                    }
                },
            )
            if (showSourceWarning) {
                val expectedRepository = "https://github.com/daisukiKaffuChino/Han1meViewer"
                val linkValid = sourceLink.trim().equals(expectedRepository, ignoreCase = true)
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.app_source_illegal_title)) },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text(stringResource(R.string.app_source_illegal_message))
                            OutlinedTextField(
                                value = sourceLink,
                                onValueChange = { sourceLink = it },
                                label = { Text(stringResource(R.string.app_source_repository_link)) },
                                singleLine = true,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = linkValid,
                            onClick = {
                                scope.launch {
                                    SettingsRepository.update {
                                        it.copy(
                                            usageSourceVerified = true,
                                            usageSourcePending = false
                                        )
                                    }
                                    showSourceWarning = false
                                    appAccessGranted = true
                                    viewModel.initializeHomePage()
                                }
                            },
                        ) { Text(stringResource(R.string.app_source_verify)) }
                    },
                )
            }
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

@Composable
private fun AppSourceDialog(
    visible: Boolean,
    onSelect: (String) -> Unit,
) {
    if (!visible) return

    var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
    val options = listOf(
        stringResource(R.string.app_source_forum) to "forum",
        stringResource(R.string.app_source_telegram) to "telegram",
        stringResource(R.string.app_source_github) to "github",
        stringResource(R.string.app_source_qq_group) to "qq_group",
        stringResource(R.string.app_source_wechat) to "wechat",
        stringResource(R.string.app_source_douyin_tiktok) to "douyin_tiktok",
    )
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.app_source_title)) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSource = value }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedSource == value,
                            onClick = { selectedSource = value },
                        )
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedSource != null,
                onClick = { selectedSource?.let(onSelect) },
            ) { Text(stringResource(R.string.app_source_confirm)) }
        },
    )
}
