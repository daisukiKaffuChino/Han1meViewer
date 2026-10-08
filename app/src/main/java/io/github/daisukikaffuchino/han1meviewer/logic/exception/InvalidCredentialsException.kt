package io.github.daisukikaffuchino.han1meviewer.logic.exception

/**
 * 账号或密码错误。
 *
 * 与网络故障区分开，便于调用方按错误类型展示不同提示。
 */
class InvalidCredentialsException(reason: String) : IllegalStateException(reason)
