package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.DatabaseRepo
import io.github.daisukikaffuchino.han1meviewer.logic.entity.HKeyframeEntity
import io.github.daisukikaffuchino.utils.ApplicationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/01 001 13:40
 */
class SettingsViewModel(application: Application) : ApplicationViewModel(application) {

    fun loadAllHKeyframes(keyword: String? = null) =
        DatabaseRepo.HKeyframe.loadAll(keyword).flowOn(Dispatchers.IO)

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

    fun removeHKeyframe(videoCode: String, keyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.removeKeyframe(videoCode, keyframe)
        }
    }


    fun modifyHKeyframe(
        videoCode: String,
        oldKeyframe: HKeyframeEntity.Keyframe, keyframe: HKeyframeEntity.Keyframe,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.modifyKeyframe(videoCode, oldKeyframe, keyframe)
        }
    }

    fun insertHKeyframes(entity: HKeyframeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.insert(entity)
        }
    }

    fun loadAllSharedHKeyframes() =
        DatabaseRepo.HKeyframe.loadAllShared().flowOn(Dispatchers.IO)
}
