package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.dao.DownloadDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import io.github.daisukikaffuchino.han1meviewer.ui.activity.MainActivity
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.TripleButtonDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DownloadSettingsScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DownloadSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager.KEY_TREE_URI
import io.github.daisukikaffuchino.han1meviewer.util.showToast
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadManagerV2

private const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
private const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
private const val DOWNLOAD_USE_PRIVATE_STORAGE = "use_private_storage"

@Composable
fun DownloadSettingsRouteScreen(
    activity: MainActivity,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    val uiState = remember(refreshKey, context) { buildDownloadSettingsUiState(context) }

    val openDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SafFileManager.persistUriPermission(context, result.data)
            Preferences.preferenceSp.edit { putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, false) }
            context.showToast(R.string.directory_saved, result.data.toString())
            refreshKey++
        } else {
            context.showToast(R.string.no_directory_selected)
        }
    }

    DownloadSettingsScreen(
        state = uiState,
        maxDownloadCountLimit = 10,
        maxDownloadSpeedLimitIndex = SpeedLimitInterceptor.SPEED_BYTES.lastIndex,
        onOpenDownloadPath = { showDownloadPathDialog = true },
        onRestoreDefaultPath = { },
        onImportDownloadedFiles = {
            importDownloadedFiles(context, activity, dao, onCompleted = { refreshKey++ })
        },
        onDownloadCountLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_COUNT_LIMIT, value) }
            HanimeDownloadManagerV2.maxConcurrentDownloadCount = value
            refreshKey++
        },
        onDownloadSpeedLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_SPEED_LIMIT, value) }
            refreshKey++
        },
    )

    if (!Preferences.isUsePrivateStorage) {
        TripleButtonDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            negativeText = stringResource(R.string.cancel),
            neutralText = stringResource(R.string.restore_default_path),
            positiveText = stringResource(R.string.ok),
            onNegative = { showDownloadPathDialog = false },
            onNeutral = {
                showDownloadPathDialog = false
                showRestoreDefaultConfirm = true
            },
            onPositive = {
                showDownloadPathDialog = false
                openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    } else {
        ConfirmDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showDownloadPathDialog = false
                openDirectoryPicker.launch(SafFileManager.buildOpenDirectoryIntent())
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    }

    ConfirmDialog(
        visible = showRestoreDefaultConfirm,
        title = stringResource(R.string.restore_default_path),
        message = stringResource(R.string.restore_default_message),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            Preferences.preferenceSp.edit {
                putBoolean(DOWNLOAD_USE_PRIVATE_STORAGE, true)
                remove(KEY_TREE_URI)
            }
            refreshKey++
            showRestoreDefaultConfirm = false
            context.showToast(R.string.default_path_restored)
        },
        onDismiss = { showRestoreDefaultConfirm = false },
    )
}

private fun buildDownloadSettingsUiState(context: Context): DownloadSettingsUiState {
    val uri = SafFileManager.getSavedUri()
    val pathSummary = if (Preferences.isUsePrivateStorage) {
        context.getExternalFilesDir(null)?.absolutePath.orEmpty()
    } else {
        DocumentFile.fromTreeUri(
            context,
            uri ?: return DownloadSettingsUiState(
                downloadPathSummary = context.getString(R.string.unknown_error),
                downloadCountLimit = Preferences.downloadCountLimit,
                downloadCountLimitSummary = toDownloadCountLimitPrettyString(
                    context,
                    Preferences.downloadCountLimit
                ),
                downloadSpeedLimitIndex = Preferences.preferenceSp.getInt(
                    DOWNLOAD_SPEED_LIMIT,
                    SpeedLimitInterceptor.NO_LIMIT_INDEX,
                ),
                downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[
                    Preferences.preferenceSp.getInt(
                        DOWNLOAD_SPEED_LIMIT,
                        SpeedLimitInterceptor.NO_LIMIT_INDEX,
                    )
                ].toDownloadSpeedPrettyString(context),
            )
        )?.name ?: uri.toString()
    }
    val speedIndex = Preferences.preferenceSp.getInt(
        DOWNLOAD_SPEED_LIMIT,
        SpeedLimitInterceptor.NO_LIMIT_INDEX,
    )
    return DownloadSettingsUiState(
        downloadPathSummary = pathSummary,
        downloadCountLimit = Preferences.downloadCountLimit,
        downloadCountLimitSummary = toDownloadCountLimitPrettyString(
            context,
            Preferences.downloadCountLimit
        ),
        downloadSpeedLimitIndex = speedIndex,
        downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[speedIndex]
            .toDownloadSpeedPrettyString(context),
    )
}
