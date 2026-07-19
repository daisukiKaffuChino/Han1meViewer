package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.dao.DownloadDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.component.TripleButtonDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DownloadSettingsScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.DownloadSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager.KEY_TREE_URI
import io.github.daisukikaffuchino.han1meviewer.util.showToast
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadManager
import io.github.daisukikaffuchino.utils.showLongToast

private const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
private const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
private const val DOWNLOAD_USE_PRIVATE_STORAGE = "use_private_storage"

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun DownloadSettingsRouteScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showSpecifyPathDialog by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<ImportProgress?>(null) }
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
            if (!Preferences.isUsePrivateStorage &&
                !Preferences.safDownloadPath.isNullOrBlank() &&
                SafFileManager.checkSafPermissions(context)
            ) {
                showImportConfirm = true
            } else {
                showSpecifyPathDialog = true
            }
        },
        onDownloadCountLimitChange = { value ->
            Preferences.preferenceSp.edit { putInt(DOWNLOAD_COUNT_LIMIT, value) }
            HanimeDownloadManager.maxConcurrentDownloadCount = value
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

    ConfirmDialog(
        visible = showImportConfirm,
        title = stringResource(R.string.confirm_import),
        message = stringResource(R.string.import_warning),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showImportConfirm = false
            importProgress = ImportProgress()
            SafFileManager.migratePrivateToSaf(context, dao) { migrated, total ->
                when (total) {
                    0 -> {
                        importProgress = null
                        showLongToast(context.getString(R.string.no_exportable_files))
                    }

                    -1 -> {
                        importProgress = null
                        showLongToast(context.getString(R.string.permission_error))
                    }

                    else -> {
                        importProgress = ImportProgress(migrated, total)
                        if (migrated == total) {
                            importProgress = null
                            showLongToast(context.getString(R.string.import_complete, total))
                            refreshKey++
                        }
                    }
                }
            }
        },
        onDismiss = { showImportConfirm = false },
    )

    if (showSpecifyPathDialog) {
        AlertDialog(
            onDismissRequest = { showSpecifyPathDialog = false },
            title = { Text(stringResource(R.string.specify_path_first)) },
            text = { Text(stringResource(R.string.path_permission_message)) },
            confirmButton = {
                TextButton(onClick = { showSpecifyPathDialog = false }) {
                    Text(stringResource(R.string.understood))
                }
            },
        )
    }

    importProgress?.let { progress ->
        ImportProgressDialog(progress = progress)
    }
}

private data class ImportProgress(
    val migrated: Int = 0,
    val total: Int = 0,
)

@Composable
private fun ImportProgressDialog(progress: ImportProgress) {
    val percent = if (progress.total > 0) {
        progress.migrated * 100 / progress.total
    } else {
        0
    }
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.import_progress),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.importing))
                LinearProgressIndicator(
                    progress = {
                        if (progress.total > 0) {
                            progress.migrated.toFloat() / progress.total.toFloat()
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        R.string.import_progress_format,
                        progress.migrated,
                        progress.total,
                        percent,
                    )
                )
            }
        }
    }
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
