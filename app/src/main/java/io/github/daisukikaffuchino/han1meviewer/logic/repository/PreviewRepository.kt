package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.websiteIOFlow

/**
 * 预告 / 预览页。
 */
object PreviewRepository {

    fun getHanimePreview(date: String) = websiteIOFlow(
        request = { HanimeApi.getHanimePreview(date) },
        action = Parser::hanimePreview
    )
}
