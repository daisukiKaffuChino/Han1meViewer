package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.websiteIOFlow

/**
 * 首页。
 */
object HomeRepository {

    fun getHomePage() = websiteIOFlow(
        request = { HanimeApi.getHomePage() },
        action = Parser::homePageVer2
    )
}
