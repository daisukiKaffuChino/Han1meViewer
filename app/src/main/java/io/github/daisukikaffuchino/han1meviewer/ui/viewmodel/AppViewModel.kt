package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import io.github.daisukikaffuchino.utils.LogUtil
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadManager
import io.github.daisukikaffuchino.han1meviewer.worker.HanimeDownloadWorker
import io.github.daisukikaffuchino.utils.ApplicationViewModel
import io.github.daisukikaffuchino.utils.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/29 029 18:00
 */
object AppViewModel : ApplicationViewModel(application), IHCsrfToken {

    /**
     * csrfToken 全局唯一，只需要在首页拉起或点击视频页时更新一下就可以了
     */
    override var csrfToken: String? = null

    val runningWorkInfoCountFlow = MutableStateFlow(0)

    init {
        // 取消，防止每次启动都有残留的更新任务
        WorkManager.getInstance(application).pruneWork()

        viewModelScope.launch(Dispatchers.IO) {
            // HanimeDownloadManager.init()
            HanimeDownloadManager.init()
        }

        viewModelScope.launch(Dispatchers.IO) {
            HanimeDownloadWorker.getRunningWorkInfoCount(application).collect { count ->
                LogUtil.d(HanimeDownloadWorker.TAG, "getRunningWorkInfoCount: $count")
                runningWorkInfoCountFlow.value = count
            }
        }
    }
}
