package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.glance.appwidget.updateAll
import io.github.daisukikaffuchino.han1meviewer.BuildConfig
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants
import io.github.daisukikaffuchino.han1meviewer.HA1_GITHUB_FORUM_URL
import io.github.daisukikaffuchino.han1meviewer.HA1_GITHUB_ISSUE_URL
import io.github.daisukikaffuchino.han1meviewer.HanimeApplication
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.BackupManager
import io.github.daisukikaffuchino.han1meviewer.logic.LocalListRepository
import io.github.daisukikaffuchino.han1meviewer.logic.OnlineListsBackup
import io.github.daisukikaffuchino.han1meviewer.logic.model.AppLanguage
import io.github.daisukikaffuchino.han1meviewer.logic.model.DisplayDensity
import io.github.daisukikaffuchino.han1meviewer.logic.model.PaletteStyle
import io.github.daisukikaffuchino.han1meviewer.logic.model.ThemeAccent
import io.github.daisukikaffuchino.han1meviewer.logic.model.ThemeMode
import io.github.daisukikaffuchino.han1meviewer.logic.model.VideoLandscapeLayoutStyle
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.HomeSettingsPage
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.HomeSettingsScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.defaultHomeCategoryPreferenceItems
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.hiddenHomeCategoryKeys
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.homeCategoryOrder
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage.saveHomeCategoryPreferences
import io.github.daisukikaffuchino.han1meviewer.ui.widget.CheckInWidget
import io.github.daisukikaffuchino.han1meviewer.util.AppLanguageManager
import io.github.daisukikaffuchino.utils.ActivityManager
import io.github.daisukikaffuchino.utils.folderSize
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ResourceType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsRouteScreen(
    activity: MainActivity,
    page: HomeSettingsPage,
    onNavigateToHKeyframes: () -> Unit = {},
    onNavigateToSharedHKeyframes: () -> Unit = {},
    onNavigateToOpenSourceLicenses: () -> Unit = {},
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val isLoggedIn by SettingsRepository.loginStateFlow.collectAsStateWithLifecycle()
    var cacheKey by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showLauncherPicker by remember { mutableStateOf(false) }
    var showApplyDeepLinksDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching { BackupManager.exportTo(context, uri) }
                .onSuccess { withContext(Dispatchers.Main) { SonnerToast.success(R.string.backup_export_success) } }
                .onFailure { withContext(Dispatchers.Main) { SonnerToast.error(R.string.backup_export_failed) } }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }
    val localListsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val jsonText = LocalListRepository.exportLocalListsJson()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(jsonText)
                } ?: error("Unable to open output file")
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    SonnerToast.success(R.string.local_data_export_success)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    SonnerToast.error(
                        it.message ?: context.getString(R.string.local_data_export_failed)
                    )
                }
            }
        }
    }
    val localListsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: error("Unable to open input file")
                LocalListRepository.importLocalListsJson(jsonText, merge = true)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    SonnerToast.success(R.string.local_data_import_success)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    SonnerToast.error(
                        it.message ?: context.getString(R.string.local_data_import_failed)
                    )
                }
            }
        }
    }
    val onlineListsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val jsonText = OnlineListsBackup.exportOnlineListsJson()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(jsonText)
                } ?: error("Unable to open output file")
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    SonnerToast.success(R.string.online_data_export_success)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    SonnerToast.error(
                        it.message ?: context.getString(R.string.online_data_export_failed)
                    )
                }
            }
        }
    }
    val onlineListsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (!SettingsRepository.isAlreadyLogin) {
            SonnerToast.warning(R.string.login_first)
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: error("Unable to open input file")
                OnlineListsBackup.importOnlineListsJson(jsonText)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    SonnerToast.success(R.string.online_data_import_success)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    SonnerToast.error(
                        it.message ?: context.getString(R.string.online_data_import_failed)
                    )
                }
            }
        }
    }
    val hanimeAppName = stringResource(R.string.hanime_app_name)
    val fakeNameCalc = stringResource(R.string.app_name_fake_calc)
    val fakeNameCornhub = stringResource(R.string.app_name_fake_cornhub)
    val fakeNameXXT = stringResource(R.string.app_name_fake_xxt)

    val launcherItems = remember(context) {
        listOf(
            LauncherItem(
                name = hanimeAppName,
                iconRes = R.drawable.ic_launcher_new,
                alias = "io.github.daisukikaffuchino.han1meviewer.LauncherAliasDefault",
            ),
            LauncherItem(
                name = fakeNameCalc,
                iconRes = R.drawable.ic_launcher_calc,
                alias = "io.github.daisukikaffuchino.han1meviewer.LauncherFakeCalc",
            ),
            LauncherItem(
                name = fakeNameCornhub,
                iconRes = R.drawable.ic_launcher_cornhub,
                alias = "io.github.daisukikaffuchino.han1meviewer.LauncherFakeCornhub",
            ),
            LauncherItem(
                name = fakeNameXXT,
                iconRes = R.drawable.ic_launcher_xxt,
                alias = "io.github.daisukikaffuchino.han1meviewer.LauncherFakeXxt",
            ),
        )
    }

    var cacheSummary by remember { mutableStateOf("") }

    LaunchedEffect(cacheKey) {
        cacheSummary = withContext(Dispatchers.IO) {
            generateClearCacheSummary(context, context.cacheDir?.folderSize ?: 0L).toString()
        }
    }
    val uiState = remember(settings, cacheSummary, launcherItems, context) {
        buildHomeSettingsUiState(
            context = context,
            launcherItems = launcherItems,
            cacheSummary = cacheSummary,
        )
    }

    HomeSettingsScreen(
        page = page,
        state = uiState,
        isLoggedIn = isLoggedIn,
        onVideoLanguageChange = { value ->
            if (value != SettingsRepository.videoLanguage) {
                coroutineScope.launch {
                    SettingsRepository.update { it.copy(videoLanguage = value) }
                    showRestartConfirmDialog = true
                }
            }
        },
        onVideoQualityChange = { value ->
            coroutineScope.launch {
                SettingsRepository.update { it.copy(videoQuality = value) }
                SonnerToast.success(R.string.success_value, value)
            }
        },
        onDarkModeChange = { value ->
            if (value != SettingsRepository.useDarkMode) {
                coroutineScope.launch { SettingsRepository.setThemeMode(ThemeMode.fromValue(value)) }
            }
        },
        onUseDynamicColorChange = { enabled ->
            coroutineScope.launch { SettingsRepository.setDynamicColor(enabled) }
        },
        onHapticFeedbackChange = { enabled ->
            coroutineScope.launch { SettingsRepository.setHapticFeedback(enabled) }
        },
        onFunLoadingHintsChange = { enabled ->
            coroutineScope.launch { SettingsRepository.update { it.copy(funLoadingHints = enabled) } }
        },
        onThemeAccentColorChange = { id ->
            coroutineScope.launch { SettingsRepository.setThemeAccent(ThemeAccent.fromId(id)) }
        },
        onAppPaletteStyleChange = { id ->
            coroutineScope.launch { SettingsRepository.setPaletteStyle(PaletteStyle.fromId(id)) }
        },
        onAllowPipModeChange = { enabled ->
            if (enabled && !isPipPermissionGranted(context)) {
                SonnerToast.warning(R.string.request_pip_alert)
                openPipPermissionSettings(context)
                coroutineScope.launch { SettingsRepository.update { it.copy(allowPipMode = false) } }
                return@HomeSettingsScreen
            }
            coroutineScope.launch { SettingsRepository.update { it.copy(allowPipMode = enabled) } }
        },
        onAllowResumePlaybackChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(allowResumePlayback = it) } }
        },
        onShowPlayedIndicatorChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(showPlayedIndicator = it) } }
        },
        onSearchArtistIgnoreVideoTypeChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(searchArtistIgnoreVideoType = it) } }
        },
        onDisableMobileDataWarningChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(disableMobileDataWarning = it) } }
        },
        onDisablePredictiveBackChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(disablePredictiveBack = it) } }
        },
        onTabletModeChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(tabletMode = it) } }
        },
        onVideoLandscapeLayoutStyleChange = { value ->
            coroutineScope.launch {
                SettingsRepository.setVideoLandscapeLayoutStyle(
                    VideoLandscapeLayoutStyle.fromValue(value)
                )
            }
        },
        onCheckInEnabledChange = {
            coroutineScope.launch {
                SettingsRepository.setCheckInEnabled(it)
                CheckInWidget().updateAll(context)
            }
        },
        onDisableCommentsChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(disableComments = it) } }
        },
        onCollapseDownloadedGroupChange = {
            coroutineScope.launch { SettingsRepository.update { settings -> settings.copy(collapseDownloadedGroup = it) } }
        },
        onSearchGridColumnsConfigChange = { config ->
            coroutineScope.launch { SettingsRepository.update { it.copy(searchGridColumnsCompact = config.compactColumns, searchGridColumnsMedium = config.mediumColumns, searchGridColumnsExpanded = config.expandedColumns, searchGridColumnsLarge = config.largeColumns) } }
        },
        onHorizontalCardCountConfigChange = { config ->
            coroutineScope.launch { SettingsRepository.update { it.copy(horizontalCardCountNarrow = config.narrowCount, horizontalCardCountCompact = config.compactCount, horizontalCardCountMedium = config.mediumCount, horizontalCardCountExpanded = config.expandedCount) } }
        },
        onHomeCategoryPreferencesChange = { order, hiddenKeys ->
            coroutineScope.launch { saveHomeCategoryPreferences(order, hiddenKeys) }
        },
        onUseLockScreenChange = { value ->
            if (value) {
                if (!isDeviceSecureCompat(context)) {
                    SonnerToast.warning(R.string.not_set_sys_lock)
                    return@HomeSettingsScreen
                }
            }
            coroutineScope.launch { SettingsRepository.update { it.copy(useLockScreen = value) } }
        },
        onSecureModeChange = { enabled ->
            coroutineScope.launch {
                SettingsRepository.update { it.copy(secureMode = enabled) }
                activity.setSecureMode(enabled)
            }
        },
        onAlwaysShowUpdateCardChange = { enabled ->
            coroutineScope.launch { SettingsRepository.setAlwaysShowUpdateCard(enabled) }
        },
        onDisplayDensityChange = { percent ->
            coroutineScope.launch {
                SettingsRepository.setDisplayDensity(DisplayDensity.fromPercent(percent))
            }
        },
        onTriggerCrash = {
            throw RuntimeException("Crash triggered from developer options")
        },
        hKeyframeSettingsContent = {
            HKeyframeSettingsRouteScreen(
                onNavigateToHKeyframes = onNavigateToHKeyframes,
                onNavigateToSharedHKeyframes = onNavigateToSharedHKeyframes,
                embedded = true,
            )
        },
        networkSettingsContent = { NetworkSettingsRouteScreen(embedded = true) },
        downloadSettingsContent = { DownloadSettingsRouteScreen(embedded = true) },
        onOpenAppLanguageSettings = { value ->
            val language = AppLanguage.fromPreference(value)
            if (AppLanguageManager.current(context) != language) {
                coroutineScope.launch { AppLanguageManager.select(context, language) }
            }
        },
        onOpenApplyDeepLinks = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                SonnerToast.warning(R.string.action_app_open_by_default_settings_not_support)
            } else {
                showApplyDeepLinksDialog = true
            }
        },
        onOpenFakeLauncherIcon = { showLauncherPicker = true },
        onOpenOpenSourceLicense = onNavigateToOpenSourceLicenses,
        onClearCache = {
            val cacheDir = context.cacheDir
            val folderSize = cacheDir?.folderSize ?: 0L
            if (folderSize == 0L) {
                SonnerToast.info(R.string.cache_empty)
                return@HomeSettingsScreen
            }
            showClearCacheConfirm = true
        },
        onExportBackup = {
            exportLauncher.launch("Han1meViewer-backup-${System.currentTimeMillis()}.json")
        },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onExportLocalLists = {
            localListsExportLauncher.launch(
                "Han1meViewer-local-lists-${System.currentTimeMillis()}.json"
            )
        },
        onImportLocalLists = {
            localListsImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onExportOnlineLists = {
            if (!SettingsRepository.isAlreadyLogin) {
                SonnerToast.warning(R.string.login_first)
                return@HomeSettingsScreen
            }
            onlineListsExportLauncher.launch(
                "Han1meViewer-online-lists-${System.currentTimeMillis()}.json"
            )
        },
        onImportOnlineLists = {
            if (!SettingsRepository.isAlreadyLogin) {
                SonnerToast.warning(R.string.login_first)
                return@HomeSettingsScreen
            }
            onlineListsImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onSubmitBug = { uriHandler.openUri(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { uriHandler.openUri(HA1_GITHUB_FORUM_URL) },
    )

    ConfirmDialog(
        visible = pendingImportUri != null,
        title = stringResource(R.string.backup_import_title),
        message = stringResource(R.string.backup_import_confirm_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            val uri = pendingImportUri ?: return@ConfirmDialog
            pendingImportUri = null
            coroutineScope.launch(Dispatchers.IO) {
                runCatching { BackupManager.importFrom(context, uri) }
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            SonnerToast.success(R.string.backup_import_success)
                            activity.recreate()
                        }
                    }
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            SonnerToast.error(R.string.backup_import_failed)
                        }
                    }
            }
        },
        onDismiss = { pendingImportUri = null },
    )

    ConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(R.string.sure_to_clear),
        message = stringResource(R.string.sure_to_clear_cache),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showClearCacheConfirm = false
            coroutineScope.launch(Dispatchers.IO) {
                val cacheDir = context.cacheDir
                val success = cacheDir?.deleteRecursively() == true
                withContext(Dispatchers.Main) {
                    cacheKey++
                    if (success) SonnerToast.success(R.string.clear_success) else SonnerToast.error(R.string.clear_failed)
                }
            }
        },
        onDismiss = { showClearCacheConfirm = false },
    )

    if (showApplyDeepLinksDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDeepLinksDialog = false },
            title = { Text(stringResource(R.string.apply_deep_links)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.apply_deep_links_summary))
                    Text(stringResource(R.string.apply_deep_links_tips))
                    Image(
                        painter = painterResource(R.raw.apply_deep_links),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyDeepLinksDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            openApplyDeepLinksSettings(context, activity)
                        }
                    },
                ) {
                    Text(stringResource(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDeepLinksDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showRestartConfirmDialog,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_needed),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            ActivityManager.restart(killProcess = true)
        },
        onDismiss = { showRestartConfirmDialog = false },
    )

    if (showLauncherPicker) {
        Dialog(
            onDismissRequest = { showLauncherPicker = false },
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.fake_app_icon),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    launcherItems.forEach { item ->
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    SettingsRepository.setLauncherIcon(item.alias)
                                    (context.applicationContext as? HanimeApplication)?.switchLauncher(item.alias)
                                    SonnerToast.info(R.string.fake_icon_hint)
                                    showLauncherPicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(30.dp),
                                )
                                Text(item.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LauncherItem(
    val name: String,
    @param:DrawableRes val iconRes: Int,
    val alias: String,
)

private fun buildHomeSettingsUiState(
    context: Context,
    launcherItems: List<LauncherItem>,
    cacheSummary: String,
): HomeSettingsUiState {
    val currentAlias = SettingsRepository.fakeLauncherIcon
    val currentItem = launcherItems.find { it.alias == currentAlias } ?: launcherItems.first()
    val videoLanguageLabel = when (SettingsRepository.videoLanguage) {
        "zht" -> context.getString(R.string.traditional_chinese)
        "zhs" -> context.getString(R.string.simplified_chinese)
        else -> SettingsRepository.videoLanguage
    }
    val appLanguage = AppLanguageManager.current(context)
    val appLanguageLabel = when (appLanguage) {
        AppLanguage.SYSTEM -> context.getString(R.string.follow_system)
        AppLanguage.ENGLISH -> "English"
        AppLanguage.CHINESE_SIMPLIFIED -> "简体中文"
        AppLanguage.CHINESE_TRADITIONAL -> "繁體中文"
    }
    val searchGridColumnsConfig = SettingsRepository.searchGridColumnsConfig
    val horizontalCardCountConfig = SettingsRepository.horizontalCardCountConfig
    return HomeSettingsUiState(
        videoLanguage = SettingsRepository.videoLanguage,
        videoLanguageLabel = videoLanguageLabel,
        defaultVideoQuality = SettingsRepository.videoQuality,
        darkMode = SettingsRepository.useDarkMode,
        appLanguage = appLanguage.preferenceValue,
        appLanguageLabel = appLanguageLabel,
        allowPipMode = SettingsRepository.current.allowPipMode,
        allowResumePlayback = SettingsRepository.allowResumePlayback,
        showPlayedIndicator = SettingsRepository.showPlayedIndicator,
        searchArtistIgnoreVideoType = SettingsRepository.searchArtistIgnoreVideoType,
        disableMobileDataWarning = SettingsRepository.disableMobileDataWarning,
        disablePredictiveBack = SettingsRepository.disablePredictiveBack,
        tabletMode = SettingsRepository.tabletMode,
        videoLandscapeLayoutStyle = SettingsRepository.videoLandscapeLayoutStyle.value,
        disableComments = SettingsRepository.current.disableComments,
        collapseDownloadedGroup = SettingsRepository.collapseDownloadedGroup,
        useDynamicColor = SettingsRepository.useDynamicColor,
        hapticFeedbackEnabled = SettingsRepository.hapticFeedbackEnabled,
        funLoadingHints = SettingsRepository.funLoadingHints,
        useLockScreen = SettingsRepository.current.useLockScreen,
        secureMode = SettingsRepository.secureMode,
        fakeLauncherIconName = currentItem.name,
        cacheSummary = cacheSummary,
        versionSummary = context.getString(
            R.string.current_version,
            "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        ),
        dynamicColorEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        themeAccentColorId = SettingsRepository.current.themeAccent.id,
        appPaletteStyleId = SettingsRepository.current.paletteStyle.id,
        searchGridColumnsSummary = listOf(
            searchGridColumnsConfig.compactColumns,
            searchGridColumnsConfig.mediumColumns,
            searchGridColumnsConfig.expandedColumns,
            searchGridColumnsConfig.largeColumns,
        ).joinToString(" / "),
        searchGridColumnsConfig = searchGridColumnsConfig,
        horizontalCardCountSummary = "${horizontalCardCountConfig.narrowCount}~${horizontalCardCountConfig.expandedCount}",
        horizontalCardCountConfig = horizontalCardCountConfig,
        checkInEnabled = SettingsRepository.isCheckInEnabled,
        homeCategoryItems = defaultHomeCategoryPreferenceItems,
        homeCategoryOrder = homeCategoryOrder,
        hiddenHomeCategoryKeys = hiddenHomeCategoryKeys,
        useAvHomeCategoryTitles = SettingsRepository.baseUrl == HanimeConstants.HANIME_URL[3],
        alwaysShowUpdateCard = SettingsRepository.alwaysShowUpdateCard,
        displayDensityPercent = SettingsRepository.displayDensity.percent,
    )
}
