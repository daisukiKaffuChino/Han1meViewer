package io.github.daisukikaffuchino.han1meviewer.logic.exception

/**
 * 登录态已过期。
 *
 * 只表达“登录态失效”这一语义，不带文案；提示语由 UI 侧根据
 * [io.github.daisukikaffuchino.han1meviewer.logic.state.HanimeErrorKind.SessionExpired] 决定。
 */
class LoginStateExpiredException : IllegalStateException()
