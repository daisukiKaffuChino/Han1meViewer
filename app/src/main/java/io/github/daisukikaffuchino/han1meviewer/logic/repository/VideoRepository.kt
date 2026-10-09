package io.github.daisukikaffuchino.han1meviewer.logic.repository

import io.github.daisukikaffuchino.han1meviewer.logic.Parser
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeApi
import io.github.daisukikaffuchino.han1meviewer.logic.network.videoIOFlow

/**
 * 影片详情。
 */
object VideoRepository {

    fun getHanimeVideo(videoCode: String) = videoIOFlow(
        request = { HanimeApi.getHanimeVideo(videoCode) },
        action = Parser::hanimeVideo
    )
}
