package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.websiteIOFlow
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import java.io.File

/**
 * 用户中心（资料 / 密码 / 头像）。
 */
object AccountRepository {

    fun getUserAccountPage(userId: String) = websiteIOFlow(
        request = { HanimeApi.getUserAccountPage(userId) },
        action = Parser::userAccountPage,
    )

    fun updateUserAccountProfile(
        userId: String,
        csrfToken: String?,
        name: String,
        email: String,
    ) = websiteIOFlow(
        request = {
            HanimeApi.updateUserAccountProfile(
                userId = userId,
                csrfToken = csrfToken,
                name = name,
                email = email,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            UiState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is UiState.Error -> UiState.Error(result.throwable)
                else -> UiState.Success(Unit)
            }
        }
    }

    fun updateUserAccountPassword(
        userId: String,
        csrfToken: String?,
        oldPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
    ) = websiteIOFlow(
        request = {
            HanimeApi.updateUserAccountPassword(
                userId = userId,
                csrfToken = csrfToken,
                oldPassword = oldPassword,
                newPassword = newPassword,
                newPasswordConfirm = newPasswordConfirm,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            UiState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is UiState.Error -> UiState.Error(result.throwable)
                else -> UiState.Success(Unit)
            }
        }
    }

    fun updateUserAccountAvatar(
        userId: String,
        csrfToken: String?,
        avatarFile: File,
    ) = websiteIOFlow(
        request = {
            HanimeApi.updateUserAccountAvatar(
                userId = userId,
                csrfToken = csrfToken,
                avatarFile = avatarFile,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            UiState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is UiState.Error -> UiState.Error(result.throwable)
                else -> UiState.Success(Unit)
            }
        }
    }
}
