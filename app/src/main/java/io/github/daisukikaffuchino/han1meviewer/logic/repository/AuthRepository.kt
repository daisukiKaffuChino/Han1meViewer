package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.exception.InvalidCredentialsException
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.NetworkErrorMapper
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 登录。
 */
object AuthRepository {

    fun login(email: String, password: String) = flow {
        emit(UiState.Loading)
        // 首先获取token
        val loginPage = HanimeApi.getLoginPage()
        val token = loginPage.body()?.string()?.let(Parser::extractTokenFromLoginPage)
        val req = HanimeApi.login(token, email, password)
        if (req.isSuccessful) {
            // 再次获取登录页面，如果失败则返回 cookie
            // 因为登录成功再次访问 login 返回 404，这是判断是否登录成功的方法
            val loginPageAgain = HanimeApi.getLoginPage()
            if (loginPageAgain.code() == 404) {
                // Cookie 會返回 XSRF-TOKEN 和 hanime1_session，我們只需要後者
                // 错误的，还需要 remember_web 字段！但我没找到！
                LogUtil.d("login_headers", req.headers().toMultimap().toString())
                emit(UiState.Success(req.headers().values("Set-Cookie")))
            } else {
                emit(UiState.Error(InvalidCredentialsException("Account or password may be incorrect")))
            }
        } else {
            // 雙重保險
            emit(UiState.Error(InvalidCredentialsException("Account or password may be incorrect")))
        }
    }.catch { e ->
        emit(UiState.Error(NetworkErrorMapper.handleException(e)))
    }.flowOn(Dispatchers.IO)
}
