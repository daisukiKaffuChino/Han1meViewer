package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage

import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.Preferences
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.SAVED_USER_ID
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.daisukikaffuchino.han1meviewer.logic.exception.LoginStateExpiredException
import io.github.daisukikaffuchino.han1meviewer.logic.model.Announcement
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import io.github.daisukikaffuchino.han1meviewer.logout
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.AppViewModel
import io.github.daisukikaffuchino.utils.getSpValue
import io.github.daisukikaffuchino.utils.putSpValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class HomePageViewModel: ViewModel() {
    data class SessionExpiredMessage(
        val message: String?,
        @param:StringRes val fallbackResId: Int,
    )

    private val _homePageFlow = MutableStateFlow<PageState<HomeData>>(PageState.Loading)
    val homePageFlow = _homePageFlow.asStateFlow()

    private val _sessionExpiredMessage = MutableSharedFlow<SessionExpiredMessage>()
    val sessionExpiredMessage = _sessionExpiredMessage

    private var homePageJob: Job? = null

    init {
        viewModelScope.launch {
            // 初始化默认已下载分组，防止[FOREIGN KEY constraint failed]
            DatabaseRepo.HanimeDownload.insertDefaultGroup()
        }
    }

    fun getHomePage(isRefresh: Boolean = false){
        homePageJob?.cancel()
        homePageJob = viewModelScope.launch {
            val current = _homePageFlow.value
            if (isRefresh && current is PageState.Success) {
                _homePageFlow.value = current.copy(isRefreshing = true)
            } else if (isRefresh && current is PageState.Error && current.cachedInfo != null) {
                _homePageFlow.value = PageState.Success(info = current.cachedInfo, isRefreshing = true)
            } else if (!isRefresh && current !is PageState.Success){
                _homePageFlow.value = PageState.Loading
            }
            NetworkRepo.getHomePage().collect { networkState ->
                when (networkState){
                    is WebsiteState.Error -> {
                        if (networkState.throwable is LoginStateExpiredException) {
                            logout()
                            _sessionExpiredMessage.emit(
                                SessionExpiredMessage(
                                    message = networkState.throwable.message,
                                    fallbackResId = R.string.login_state_expired,
                                )
                            )
                        }
                        val previousData = (_homePageFlow.value as? PageState.Success)?.info
                        _homePageFlow.value = PageState.Error(networkState.throwable, cachedInfo = previousData)
                    }
                    is WebsiteState.Success -> {
                        AppViewModel.csrfToken = networkState.info.csrfToken
                        networkState.info.userId.takeIf { it.isNotEmpty() }?.let { userId ->
                            Preferences.preferenceSp.edit { putString(SAVED_USER_ID, userId) }
                        }
                        val homeData = HomeData(page = networkState.info)
                        _homePageFlow.value = PageState.Success(info = homeData, isRefreshing = false)
                    }
                    is WebsiteState.Loading -> { }
                }
            }
        }
    }

    fun dismissAnnouncements(){
        putSpValue("last_dismiss_time", System.currentTimeMillis(), "setting_pref")
        val current = _homePageFlow.value
        if (current is PageState.Success) {
            _homePageFlow.value = current.copy(info = current.info.copy(announcements = emptyList()))
        }
    }

    fun deleteWatchHistory(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.delete(history)
            Log.d("delete_watch_hty", "$history DONE!")
        }
    }

    fun deleteAllWatchHistories() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.deleteAll()
            Log.d("del_all_watch_hty", "DONE!")
        }
    }

    fun loadAllWatchHistories() =
        DatabaseRepo.WatchHistory.loadAll()
            .catch { e -> e.printStackTrace() }
            .flowOn(Dispatchers.IO)
    private val _modifyHKeyframeFlow = MutableSharedFlow<Boolean>()
    fun removeHKeyframe(videoCode: String, hKeyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.removeKeyframe(videoCode, hKeyframe)
            Log.d("HKeyframe", "removeHKeyframe:$hKeyframe DONE!")
            _modifyHKeyframeFlow.emit(true)
        }
    }
    fun modifyHKeyframe(
        videoCode: String,
        oldKeyframe: HKeyframeEntity.Keyframe, keyframe: HKeyframeEntity.Keyframe,
    ) {
        viewModelScope.launch {
            DatabaseRepo.HKeyframe.modifyKeyframe(videoCode, oldKeyframe, keyframe)
            Log.d("HKeyframe", "modifyHKeyframe:$keyframe DONE!")
            _modifyHKeyframeFlow.emit(true)
        }
    }
    fun deleteHKeyframes(entity: HKeyframeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.delete(entity)
        }
    }

    fun updateHKeyframes(entity: HKeyframeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.update(entity)
        }
    }
}
