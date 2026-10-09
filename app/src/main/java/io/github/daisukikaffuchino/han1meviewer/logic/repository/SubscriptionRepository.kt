package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.websiteIOFlow
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.utils.LogUtil

/**
 * 订阅（我的订阅列表 + 订阅 / 取消订阅作者）。
 */
object SubscriptionRepository {

    fun getMySubscriptions(page: Int) = websiteIOFlow(
        request = { HanimeApi.getMySubscriptions(page) },
        action = Parser::getMySubscriptions
    )

    fun subscribeArtist(
        csrfToken: String?,
        userId: String,
        artistId: String,
        // 这里表示目标状态
        status: Boolean,
    ) = websiteIOFlow(
        request = {
            HanimeApi.subscribeArtist(csrfToken, userId, artistId, status)
        }
    ) {
        LogUtil.d("subscribe_artist_body", it)
        return@websiteIOFlow UiState.Success(status)
    }
}
