package io.github.daisukikaffuchino.han1meviewer.worker

import android.util.Log
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.dao.DownloadDatabase
import io.github.daisukikaffuchino.han1meviewer.logic.entity.download.HanimeDownloadEntity
import io.github.daisukikaffuchino.han1meviewer.logic.state.DownloadState
import io.github.daisukikaffuchino.han1meviewer.util.runSuspendCatching
import io.github.daisukikaffuchino.utils.applicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.resume

/**
 * 优化后的下载管理器，利用 Channel �?Semaphore 限制并发下载数，
 * 同时通过监听 WorkManager 的任务状态实现“等待任务完成后释放许可”的逻辑�?
 */
object HanimeDownloadManager {

    private const val TAG = "HanimeDownloadManager"

    const val MAX_CONCURRENT_DOWNLOAD_DEF = 2
    var maxConcurrentDownloadCount = 0
        set(value) {
            field = if (value > 0) value else Int.MAX_VALUE
            // 如果更新并发数，重新创建 semaphore
            semaphore = Semaphore(field)
        }

    private val workManager = WorkManager.getInstance(applicationContext)

    // 信号量限制同时下载的任务数量
    private var semaphore: Semaphore = Semaphore(1)

    init {
        // �?Preferences 里的值初始化，保�?0 会被转换�?Int.MAX_VALUE
        maxConcurrentDownloadCount = Preferences.downloadCountLimit
    }

    // Channel 内部状态：保存正在下载任务与等待队�?
    private val activeDownloads = linkedMapOf<String, HanimeDownloadWorker.Args>()
    private val waitingQueue = ArrayDeque<HanimeDownloadWorker.Args>()

    // 协程 Scope，用于管�?channel 与任务协�?
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Channel 消息类型
    private sealed class DownloadMsg {
        /**
         * 添加下载任务
         */
        data class Add(
            val args: HanimeDownloadWorker.Args,
            val redownload: Boolean = false,
            val waiting: Boolean = false,
            val state: DownloadState = DownloadState.Unknown
        ) : DownloadMsg()

        /**
         * 恢复下载任务（暂�?=> 下载�?
         */
        data class Resume(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 停止下载任务
         */
        data class Stop(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 删除下载任务
         */
        data class Delete(val args: HanimeDownloadWorker.Args) : DownloadMsg()

        /**
         * 处理下一个任�?
         */
        data object ProcessNext : DownloadMsg()
    }

    private val downloadChannel = Channel<DownloadMsg>(capacity = Channel.UNLIMITED)

    init {
        scope.launch {
            for (msg in downloadChannel) {
                when (msg) {
                    is DownloadMsg.Add -> {
                        if (msg.args.videoCode in activeDownloads) {
                            Log.d(TAG, "任务已存在：${msg.args.videoCode}")
                        } else if (waitingQueue.any { it.videoCode == msg.args.videoCode }) {
                            Log.d(TAG, "任务已在等待队列�?{msg.args.videoCode}")
                        } else {
                            // Unknown 代表任务刚添加，未开始状态流�?
                            if (activeDownloads.size < maxConcurrentDownloadCount &&
                                (msg.state == DownloadState.Downloading || msg.state == DownloadState.Unknown)
                            ) {
                                Log.d(TAG, "添加任务�?{msg.args.videoCode}")
                                activeDownloads[msg.args.videoCode] = msg.args
                                launchDownload(msg.args, msg.redownload, msg.waiting)
                            } else {
                                Log.d(TAG, "任务已满，加入等待队列：${msg.args.videoCode}")
                                when (msg.state) {
                                    DownloadState.Downloading -> {
                                        // 之前�?Downloading 的优先级更高
                                        waitingQueue.addFirst(msg.args)
                                        enqueueWaitingWork(msg.args, msg.redownload)
                                    }

                                    DownloadState.Queued, DownloadState.Unknown -> {
                                        waitingQueue.addLast(msg.args)
                                        enqueueWaitingWork(msg.args, msg.redownload)
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    }

                    is DownloadMsg.Resume -> {
                        if (msg.args.videoCode in activeDownloads) {
                            Log.d(TAG, "任务已在下载中，无需恢复�?{msg.args.videoCode}")
                        } else {
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            Log.d(TAG, "恢复任务�?{msg.args.videoCode}")
                            // 如果 active 已满，则暂停一个任务，加入等待队列
                            while (activeDownloads.size >= maxConcurrentDownloadCount && activeDownloads.isNotEmpty()) {
                                val (videoCode, task) = activeDownloads.entries.first()
                                activeDownloads.remove(videoCode)
                                stopWork(task)
                                waitingQueue.addLast(task)
                                markQueued(task)
                                Log.d(TAG, "任务已满，暂停任务：$videoCode")
                            }
                            activeDownloads[msg.args.videoCode] = msg.args
                            launchDownload(msg.args, redownload = false, waiting = false)
                        }
                    }

                    is DownloadMsg.Stop -> {
                        if (activeDownloads.remove(msg.args.videoCode) != null) {
                            Log.d(TAG, "停止任务�?{msg.args.videoCode}")
                            stopWork(msg.args)
                            processNext()
                        } else {
                            Log.e(TAG, "停止任务，不应该走到这里�?{msg.args.videoCode}")
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            markPaused(msg.args)
                        }
                    }

                    is DownloadMsg.Delete -> {
                        if (activeDownloads.remove(msg.args.videoCode) != null) {
                            Log.d(TAG, "从正在下载列表中删除任务�?{msg.args.videoCode}")
                        } else {
                            waitingQueue.removeIf { it.videoCode == msg.args.videoCode }
                            Log.d(TAG, "从等待队列中删除任务�?{msg.args.videoCode}")
                        }
                        deleteWork(msg.args)
                        processNext()
                    }

                    DownloadMsg.ProcessNext -> processNext()
                }
            }
        }
    }

    /**
     * 初始化，加载所有正在下载的任务
     */
    suspend fun init() {
        Log.d(TAG, "init")
        val allDownloading =
            DownloadDatabase.instance.hanimeDownloadDao.loadAllDownloadingHanimeOnce()
        allDownloading.forEach { entity ->
            val args = HanimeDownloadWorker.Args.fromEntity(entity)
            // addTask
            downloadChannel.send(DownloadMsg.Add(args, state = entity.state))
        }
    }

    /**
     * 添加下载任务
     */
    fun addTask(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false, waiting: Boolean = false
    ) {
        scope.launch { downloadChannel.send(DownloadMsg.Add(args, redownload, waiting)) }
    }

    /**
     * 恢复下载任务
     */
    fun resumeTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Resume(args)) }
    }

    /**
     * 停止下载任务
     */
    fun stopTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Stop(args)) }
    }

    /**
     * 删除下载任务
     */
    fun deleteTask(entity: HanimeDownloadEntity) {
        val args = HanimeDownloadWorker.Args.fromEntity(entity)
        scope.launch { downloadChannel.send(DownloadMsg.Delete(args)) }
    }

    /**
     * 处理等待队列中的下一个任�?
     */
    private fun processNext() {
        Log.d(TAG, "processNext")
        while (activeDownloads.size < maxConcurrentDownloadCount && waitingQueue.isNotEmpty()) {
            val next = waitingQueue.removeFirst()
            activeDownloads[next.videoCode] = next
            launchDownload(next, redownload = false, waiting = false)
        }
    }

    /**
     * 启动下载任务，采�?semaphore 限制并发数，并等待任务完成后自动释放许可
     */
    private fun launchDownload(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean,
        waiting: Boolean
    ) {
        scope.launch {
            // 如果当前处于等待状态，则直接启动任务。目的就是为了添加到列表，但不下�?
            if (waiting) {
                Log.d(TAG, "launchDownload (waiting): ${args.videoCode}")
                markQueued(args)
            } else {
                // 使用 semaphore.withPermit 来确保同时只有规定数量的任务在执�?
                semaphore.withPermit {
                    Log.d(TAG, "launchDownload (start): ${args.videoCode}")
                    // 启动 WorkManager 任务
                    val workId = startWork(args, redownload)
                    // 阻塞等待 WorkManager 任务完成
                    awaitWorkCompletion(args.videoCode, workId.toString())
                }
                // 下载完成或取消后，从 active 中移除，并尝试启动下一个任�?
                activeDownloads.remove(args.videoCode)
                Log.d(TAG, "launchDownload (end): ${args.videoCode}")
                downloadChannel.send(DownloadMsg.ProcessNext)
            }
        }
    }

    /**
     * 开启下载任�?
     */
    private suspend fun startWork(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false,
        waiting: Boolean = false,
        delete: Boolean = false
    ) = HanimeDownloadWorker.build(constraintsRequired = !delete) {
            setInputData(
                workDataOf(
                    HanimeDownloadWorker.QUALITY to args.quality,
                    HanimeDownloadWorker.DOWNLOAD_URL to args.downloadUrl,
                    HanimeDownloadWorker.VIDEO_TYPE to args.videoType,
                    HanimeDownloadWorker.HANIME_NAME to args.hanimeName,
                    HanimeDownloadWorker.VIDEO_CODE to args.videoCode,
                    HanimeDownloadWorker.COVER_URL to args.coverUrl,
                    HanimeDownloadWorker.REDOWNLOAD to redownload,
                    HanimeDownloadWorker.IN_WAITING_QUEUE to waiting,
                    HanimeDownloadWorker.DELETE to delete
                )
            )
        }.apply {
            workManager.beginUniqueWork(
                args.videoCode, ExistingWorkPolicy.REPLACE, this
            ).enqueue().await()
        }.id

    /**
     * 取消正在执行�?WorkManager 任务
     */
    private suspend fun stopWork(args: HanimeDownloadWorker.Args) {
        runSuspendCatching {
            workManager.cancelUniqueWork(args.videoCode).await()
            markPaused(args)
            Log.d(TAG, "stopWork (cancelUniqueWork): ${args.videoCode}")
        }.onFailure { t -> // 上述方法可能无法取消任务
            t.printStackTrace()
            markPaused(args)
        }
    }

    private suspend fun markQueued(args: HanimeDownloadWorker.Args) {
        DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())?.let { entity ->
            DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Queued))
        }
    }

    private suspend fun enqueueWaitingWork(
        args: HanimeDownloadWorker.Args,
        redownload: Boolean = false
    ) {
        val entity = DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())
        if (entity == null) {
            startWork(args, redownload = redownload, waiting = true)
        } else {
            DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Queued))
        }
    }

    private suspend fun markPaused(args: HanimeDownloadWorker.Args) {
        DatabaseRepo.HanimeDownload.find(args.videoCode, args.quality.orEmpty())?.let { entity ->
            if (entity.state != DownloadState.Finished) {
                DatabaseRepo.HanimeDownload.update(entity.copy(state = DownloadState.Paused))
            }
        }
    }

    /**
     * 删除下载任务
     *
     * 删除操作交给 WorkManager 处理
     */
    private suspend fun deleteWork(args: HanimeDownloadWorker.Args) = startWork(args, delete = true)

    /**
     * 通过观察 WorkManager �?LiveData 来阻塞等待任务完�?
     */
    private suspend fun awaitWorkCompletion(videoCode: String, workId: String) =
        suspendCancellableCoroutine { cont ->
            val liveData = workManager.getWorkInfosForUniqueWorkLiveData(videoCode)
            Log.d(TAG, "获取 LiveData�?videoCode")
            val observer = object : Observer<List<WorkInfo>> {
                override fun onChanged(value: List<WorkInfo>) {
                    val info = value.firstOrNull { it.id.toString() == workId } ?: return
                    if (info.state.isFinished) {
                        Log.d(TAG, "任务完成，移�?observer�?videoCode")
                        liveData.removeObserver(this)
                        cont.resume(Unit)
                    }
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                liveData.observeForever(observer)
                Log.d(TAG, "添加 observer�?videoCode")
            }
            cont.invokeOnCancellation { liveData.removeObserver(observer) }
        }
}
