package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import io.github.daisukikaffuchino.utils.LogUtil
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.dao.DownloadDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.download.HanimeDownloadEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.download.VideoWithCategories
import io.github.daisukikaffuchino.han1meviewer.ui.component.ConfirmDialog
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.DownloadScreen
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.download.DownloadEvent
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.DownloadViewModel
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager.checkSafPermissions
import io.github.daisukikaffuchino.han1meviewer.util.SafFileManager.scanAndImportHanimeDownloads
import io.github.daisukikaffuchino.han1meviewer.util.openDownloadedHanimeVideoLocally
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadManager
import io.github.daisukikaffuchino.utils.application
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DownloadViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val dao = remember { DownloadDatabase.instance.hanimeDownloadDao }
    var showVideoNotExistConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showDeleteVideoConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showImportDownloadedConfirm by remember { mutableStateOf(false) }
    var isImportingDownloaded by remember { mutableStateOf(false) }

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {
            is DownloadEvent.OnPauseAll -> event.items.forEach { entity ->
                if (entity.isDownloading) HanimeDownloadManager.stopTask(entity)
            }
            is DownloadEvent.OnResumeAll -> event.items.forEach { entity ->
                if (!entity.isDownloading) HanimeDownloadManager.resumeTask(entity)
            }
            is DownloadEvent.OnPauseItem -> HanimeDownloadManager.stopTask(event.item)
            is DownloadEvent.OnResumeItem -> HanimeDownloadManager.resumeTask(event.item)
            is DownloadEvent.OnDeleteDownloadingItem -> HanimeDownloadManager.deleteTask(event.item)

            is DownloadEvent.OnImportDownloaded -> {
                if (!Preferences.safDownloadPath.isNullOrBlank() &&
                    !Preferences.isUsePrivateStorage && !isImportingDownloaded
                ) {
                    showImportDownloadedConfirm = true
                } else {
                    SonnerToast.warning(application.getString(R.string.select_custom_directory))
                }
            }

            is DownloadEvent.OnOpenDownloadedVideo -> onNavigateToVideo(event.video.video.videoCode)
            is DownloadEvent.OnLocalPlayback -> onNavigateToLocalVideo(
                event.video.video.videoCode, event.video.video.videoUri
            )

            is DownloadEvent.OnExternalPlayback -> {
                context.openDownloadedHanimeVideoLocally(event.video.video.videoUri) {
                    showVideoNotExistConfirm = event.video
                }
            }

            is DownloadEvent.OnDeleteDownloadedVideo -> showDeleteVideoConfirm = event.video

            is DownloadEvent.OnMoveVideoGroup -> viewModel.updateVideoGroup(
                event.video.video.videoCode, event.groupId
            )

            is DownloadEvent.OnRenameGroup -> {
                viewModel.updateGroupName(event.groupId, event.newName)
                SonnerToast.success(application.getString(R.string.group_renamed, event.newName))
            }

            is DownloadEvent.OnCreateGroup -> {
                if (event.name.isBlank()) {
                SonnerToast.warning(application.getString(R.string.group_name_empty))
                } else {
                    viewModel.createNewGroup(event.name)
                SonnerToast.success(application.getString(R.string.create_group_success, event.name))
                }
            }

            is DownloadEvent.OnDeleteGroup -> {
                viewModel.deleteGroup(event.group)
                SonnerToast.success(application.getString(R.string.delete_success))
            }

            is DownloadEvent.OnBatchDelete -> event.videos.forEach { video ->
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                SafFileManager.deleteDownloadVideoFolder(context, video.video.videoCode)
            }

            is DownloadEvent.OnBatchMoveGroup -> event.videos.forEach { video ->
                viewModel.updateVideoGroup(video.video.videoCode, event.groupId)
            }

            // 以下事件由 Screen 层自行处理，Route 不关心
            is DownloadEvent.OnToggleGroup,
            is DownloadEvent.OnCreateGroupDialogChange,
            is DownloadEvent.OnPageChange,
            is DownloadEvent.OnToggleMultiSelect,
            is DownloadEvent.OnToggleVideoSelection,
            is DownloadEvent.OnSelectAllCurrentGroup,
            is DownloadEvent.OnBatchMoveRequest -> Unit
        }
    }

    DownloadScreen(
        downloadingFlow = viewModel.loadAllDownloadingHanime(),
        downloadedFlow = viewModel.downloaded,
        downloadedGroupsFlow = viewModel.downloadedGroups,
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        onBack = onBack,
        onLoadDownloaded = {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = false,
            )
        },
        onEvent = handleEvent,
    )

    ConfirmDialog(
        visible = showImportDownloadedConfirm,
        title = application.getString(R.string.read_download_dir_title),
        message = application.getString(R.string.read_download_dir_message),
        confirmText = application.getString(R.string.ok),
        dismissText = application.getString(R.string.cancel),
        onConfirm = {
            showImportDownloadedConfirm = false
            isImportingDownloaded = true
            scope.launch {
                val importSucceeded = withContext(Dispatchers.IO) {
                    try {
                        if (!checkSafPermissions(context)) return@withContext false
                        scanAndImportHanimeDownloads(context, dao)
                        true
                    } catch (e: Exception) {
                        LogUtil.e("ImportHanime", "Failed to import downloaded videos", e)
                        false
                    }
                }
                isImportingDownloaded = false
                if (importSucceeded) {
                    viewModel.loadAllDownloadedHanime(
                        sortedBy = HanimeDownloadEntity.SortedBy.ID,
                        ascending = false,
                    )
                    SonnerToast.success(application.getString(R.string.read_success))
                } else {
                    SonnerToast.error(application.getString(R.string.permission_error))
                }
            }
        },
        onDismiss = { showImportDownloadedConfirm = false },
    )

    showVideoNotExistConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = application.getString(R.string.video_not_exist),
            message = application.getString(R.string.video_deleted_sure_to_delete_item),
            confirmText = application.getString(R.string.delete),
            dismissText = application.getString(R.string.cancel),
            onConfirm = {
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showVideoNotExistConfirm = null
            },
            onDismiss = { showVideoNotExistConfirm = null },
        )
    }

    showDeleteVideoConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = application.getString(R.string.sure_to_delete),
            message = application.getString(R.string.prepare_to_delete_s, video.video.title),
            confirmText = application.getString(R.string.confirm),
            dismissText = application.getString(R.string.cancel),
            onConfirm = {
                SafFileManager.deleteDownloadVideoFolder(context, video.video.videoCode)
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showDeleteVideoConfirm = null
            },
            onDismiss = { showDeleteVideoConfirm = null },
        )
    }
}
