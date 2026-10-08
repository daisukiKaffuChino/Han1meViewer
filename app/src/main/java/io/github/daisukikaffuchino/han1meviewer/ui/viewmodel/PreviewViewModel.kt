package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimePreview
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.util.TagLocalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/23 023 16:47
 */
class PreviewViewModel : ViewModel() {

    private val previewCache = linkedMapOf<String, UiState<HanimePreview>>()

    private val _previewFlow =
        MutableStateFlow<UiState<HanimePreview>>(UiState.Loading)
    val previewFlow = _previewFlow.asStateFlow()

    fun getHanimePreview(date: String) {
        viewModelScope.launch {
            previewCache[date]?.let {
                _previewFlow.value = it
                return@launch
            }
            NetworkRepo.getHanimePreview(date).collect { preview ->
                val localizedPreview = preview.withLocalizedTags()
                _previewFlow.value = localizedPreview
                if (localizedPreview !is UiState.Loading) {
                    previewCache[date] = localizedPreview
                }
            }
        }
    }

    fun preloadPreview(date: String) {
        if (previewCache.containsKey(date)) return
        viewModelScope.launch {
            val preview = runCatching {
                withContext(Dispatchers.IO) {
                    NetworkRepo.getHanimePreview(date)
                        .catch { emit(UiState.Error(it)) }
                        .first { it !is UiState.Loading }
                }
            }.getOrElse { UiState.Error(it) }
            previewCache[date] = preview.withLocalizedTags()
        }
    }

    fun getCachedPreview(date: String): UiState<HanimePreview>? = previewCache[date]

    private fun UiState<HanimePreview>.withLocalizedTags(): UiState<HanimePreview> {
        return if (this is UiState.Success) {
            UiState.Success(data.withLocalizedTags())
        } else {
            this
        }
    }

    private fun HanimePreview.withLocalizedTags(): HanimePreview {
        return copy(previewInfo = previewInfo.map { info ->
            info.copy(tags = TagLocalizer.localizeTags(info.tags))
        })
    }
}
