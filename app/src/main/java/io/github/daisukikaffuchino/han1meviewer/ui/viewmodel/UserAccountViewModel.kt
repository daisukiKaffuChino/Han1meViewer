package io.github.daisukikaffuchino.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.repository.AccountRepository
import io.github.daisukikaffuchino.han1meviewer.logic.exception.NotLoggedInException
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccount
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccountAction
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccountActionEvent
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccountSubmittingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UserAccountViewModel : ViewModel() {

    private val _accountState = MutableStateFlow<UiState<UserAccount>>(UiState.Loading)
    val accountState = _accountState.asStateFlow()

    private val _actionFlow = MutableSharedFlow<UserAccountActionEvent>()
    val actionFlow = _actionFlow.asSharedFlow()

    private val _submittingState = MutableStateFlow(UserAccountSubmittingState.Idle)
    val submittingState = _submittingState.asStateFlow()

    fun loadAccount(forceReload: Boolean = false) {
        if (!forceReload && _accountState.value is UiState.Success) return
        val userId = SettingsRepository.savedUserId
        if (userId.isBlank()) {
            _accountState.value = UiState.Error(
                NotLoggedInException()
            )
            return
        }
        viewModelScope.launch {
            _accountState.value = UiState.Loading
            AccountRepository.getUserAccountPage(userId).collect { state ->
                _accountState.value = state
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        val account = (_accountState.value as? UiState.Success)?.data ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingProfile
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.ProfileUpdated, UiState.Loading))
            AccountRepository.updateUserAccountProfile(
                userId = account.userId,
                csrfToken = account.csrfToken,
                name = name,
                email = email,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.ProfileUpdated, state))
                if (state is UiState.Success) {
                    loadAccount(forceReload = true)
                }
                if (state !is UiState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }

    fun updatePassword(oldPassword: String, newPassword: String, newPasswordConfirm: String) {
        val account = (_accountState.value as? UiState.Success)?.data ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingPassword
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.PasswordUpdated, UiState.Loading))
            AccountRepository.updateUserAccountPassword(
                userId = account.userId,
                csrfToken = account.csrfToken,
                oldPassword = oldPassword,
                newPassword = newPassword,
                newPasswordConfirm = newPasswordConfirm,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.PasswordUpdated, state))
                if (state !is UiState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }

    fun updateAvatar(avatarFile: File) {
        val account = (_accountState.value as? UiState.Success)?.data ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingAvatar
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.AvatarUpdated, UiState.Loading))
            AccountRepository.updateUserAccountAvatar(
                userId = account.userId,
                csrfToken = account.csrfToken,
                avatarFile = avatarFile,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.AvatarUpdated, state))
                if (state is UiState.Success) {
                    loadAccount(forceReload = true)
                }
                if (state !is UiState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }
}
