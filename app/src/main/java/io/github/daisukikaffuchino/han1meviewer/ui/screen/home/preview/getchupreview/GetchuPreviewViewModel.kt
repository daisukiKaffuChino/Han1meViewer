package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import io.github.daisukikaffuchino.utils.LogUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.GetchuNetworkRepo.getGetchuPreview
import io.github.daisukikaffuchino.han1meviewer.logic.GetchuNetworkRepo.getGetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreview
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageState
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GetchuPreviewViewModel : ViewModel() {

    private val previewCache = linkedMapOf<String, PageState<GetchuPreview>>()
    private val detailCache = linkedMapOf<String, PageState<GetchuPreviewDetail>>()

    private val _previewFlow = MutableStateFlow<PageState<GetchuPreview>>(PageState.Loading)
    val previewFlow = _previewFlow.asStateFlow()

    private val _detailStates =
        MutableStateFlow<Map<String, PageState<GetchuPreviewDetail>>>(emptyMap())

    fun detailState(id: String) = _detailStates
        .map { states -> states[id] ?: PageState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), detailCache[id] ?: PageState.Loading)

    fun getPreview(date: String) {
        viewModelScope.launch {
            LogUtil.d("GetchuPreviewVM", "getPreview date=$date cacheHit=${previewCache.containsKey(date)}")
            previewCache[date]?.let {
                _previewFlow.value = it
                LogUtil.d("GetchuPreviewVM", "emit cached list date=$date state=${it.logSummary()}")
                return@launch
            }
            _previewFlow.value = PageState.Loading
            getGetchuPreview(date).collect { state ->
                val pageState = state.toPageState()
                LogUtil.d("GetchuPreviewVM", "emit list date=$date state=${pageState.logSummary()}")
                _previewFlow.value = pageState
                if (pageState is PageState.Success || pageState is PageState.NoMoreData) {
                    previewCache[date] = pageState
                }
            }
        }
    }

    fun getDetail(id: String) {
        viewModelScope.launch {
            LogUtil.d("GetchuPreviewVM", "getDetail id=$id cacheHit=${detailCache.containsKey(id)}")
            detailCache[id]?.let { cachedState ->
                setDetailState(id, cachedState)
                LogUtil.d("GetchuPreviewVM", "emit cached detail id=$id state=${cachedState.logSummary()}")
                return@launch
            }
            setDetailState(id, PageState.Loading)
            getGetchuPreviewDetail(id).collect { state ->
                val pageState = state.toPageState()
                LogUtil.d("GetchuPreviewVM", "emit detail id=$id state=${pageState.logSummary()}")
                setDetailState(id, pageState)
                if (pageState is PageState.Success || pageState is PageState.NoMoreData) {
                    detailCache[id] = pageState
                }
            }
        }
    }

    private fun setDetailState(id: String, state: PageState<GetchuPreviewDetail>) {
        _detailStates.value += (id to state)
    }

    private fun <T> WebsiteState<T>.toPageState(): PageState<T> {
        return when (this) {
            is WebsiteState.Loading -> PageState.Loading
            is WebsiteState.Error -> PageState.Error(throwable)
            is WebsiteState.Success -> PageState.Success(info)
        }
    }

    private fun PageState<*>.logSummary(): String {
        return when (this) {
            is PageState.Loading -> "Loading"
            is PageState.Empty -> "Empty"
            is PageState.Error -> "Error(${throwable::class.simpleName}: ${throwable.message})"
            is PageState.NoMoreData<*> -> "NoMoreData"
            is PageState.Success<*> -> when (val value = info) {
                is GetchuPreview -> "Success(GetchuPreview groups=${value.groups.size} totalItems=${value.groups.sumOf { it.items.size }})"
                is GetchuPreviewDetail -> "Success(GetchuPreviewDetail title=${value.title.take(60)} samples=${value.sampleImages.size})"
                else -> "Success(${value?.let { it::class.simpleName }})"
            }
        }
    }
}
