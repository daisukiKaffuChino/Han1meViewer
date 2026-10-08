package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import io.github.daisukikaffuchino.utils.LogUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.GetchuNetworkRepo.getGetchuPreview
import io.github.daisukikaffuchino.han1meviewer.logic.GetchuNetworkRepo.getGetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreview
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GetchuPreviewViewModel : ViewModel() {

    private val previewCache = linkedMapOf<String, PagedUiState<GetchuPreview>>()
    private val detailCache = linkedMapOf<String, PagedUiState<GetchuPreviewDetail>>()

    private val _previewFlow = MutableStateFlow<PagedUiState<GetchuPreview>>(PagedUiState.Loading)
    val previewFlow = _previewFlow.asStateFlow()

    private val _detailStates =
        MutableStateFlow<Map<String, PagedUiState<GetchuPreviewDetail>>>(emptyMap())

    fun detailState(id: String) = _detailStates
        .map { states -> states[id] ?: PagedUiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), detailCache[id] ?: PagedUiState.Loading)

    fun getPreview(date: String) {
        viewModelScope.launch {
            LogUtil.d("GetchuPreviewVM", "getPreview date=$date cacheHit=${previewCache.containsKey(date)}")
            previewCache[date]?.let {
                _previewFlow.value = it
                LogUtil.d("GetchuPreviewVM", "emit cached list date=$date state=${it.logSummary()}")
                return@launch
            }
            _previewFlow.value = PagedUiState.Loading
            getGetchuPreview(date).collect { state ->
                val pageState = state.toPageState()
                LogUtil.d("GetchuPreviewVM", "emit list date=$date state=${pageState.logSummary()}")
                _previewFlow.value = pageState
                if (pageState is PagedUiState.Success || pageState is PagedUiState.NoMoreData) {
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
            setDetailState(id, PagedUiState.Loading)
            getGetchuPreviewDetail(id).collect { state ->
                val pageState = state.toPageState()
                LogUtil.d("GetchuPreviewVM", "emit detail id=$id state=${pageState.logSummary()}")
                setDetailState(id, pageState)
                if (pageState is PagedUiState.Success || pageState is PagedUiState.NoMoreData) {
                    detailCache[id] = pageState
                }
            }
        }
    }

    private fun setDetailState(id: String, state: PagedUiState<GetchuPreviewDetail>) {
        _detailStates.value += (id to state)
    }

    private fun <T> UiState<T>.toPageState(): PagedUiState<T> {
        return when (this) {
            is UiState.Loading -> PagedUiState.Loading
            is UiState.Empty -> PagedUiState.Empty
            is UiState.Error -> PagedUiState.Error(throwable)
            is UiState.Success -> PagedUiState.Success(data)
        }
    }

    private fun PagedUiState<*>.logSummary(): String {
        return when (this) {
            is PagedUiState.Loading -> "Loading"
            is PagedUiState.Empty -> "Empty"
            is PagedUiState.Error -> "Error(${throwable::class.simpleName}: ${throwable.message})"
            is PagedUiState.NoMoreData -> "NoMoreData"
            is PagedUiState.Success<*> -> when (val value = data) {
                is GetchuPreview -> "Success(GetchuPreview groups=${value.groups.size} totalItems=${value.groups.sumOf { it.items.size }})"
                is GetchuPreviewDetail -> "Success(GetchuPreviewDetail title=${value.title.take(60)} samples=${value.sampleImages.size})"
                else -> "Success(${value?.let { it::class.simpleName }})"
            }
        }
    }
}
