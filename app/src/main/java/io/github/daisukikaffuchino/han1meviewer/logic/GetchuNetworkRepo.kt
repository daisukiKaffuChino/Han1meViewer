package io.github.daisukikaffuchino.han1meviewer.logic

import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo.handleException
import io.github.daisukikaffuchino.han1meviewer.logic.NetworkRepo.throwRequestException
import io.github.daisukikaffuchino.han1meviewer.logic.network.HanimeNetwork
import io.github.daisukikaffuchino.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response
import java.nio.charset.Charset
import kotlin.runCatching

object GetchuNetworkRepo {
    val GETCHU_CHARSET: Charset = Charset.forName("EUC-JP")
    fun getGetchuPreview(date: String) = websiteIOFlow(
        request = {
            HanimeNetwork.getchuService.getPreviewList(
                year = date.take(4),
                month = date.takeLast(2),
            )
        },
        bodyToString = { it.getchuString() },
        action = { GetchuParser.getchuPreview(it, date) }
    )
    fun getGetchuPreviewDetail(id: String) = websiteIOFlow(
        request = { HanimeNetwork.getchuService.getPreviewDetail(id) },
        bodyToString = { it.getchuString() },
    ) { body ->
        val detailState = GetchuParser.getchuPreviewDetail(body, id)
        if (detailState !is WebsiteState.Success) return@websiteIOFlow detailState

        val parentId = body.extractGetchuSeriesParentId() ?: return@websiteIOFlow detailState
        runCatching {
            val response = HanimeNetwork.getchuService.getSeriesItems(parentIdArray = parentId)
            if (!response.isSuccessful) return@runCatching emptyList()
            response.body()?.getchuString()?.let(GetchuParser::getchuSeriesItems).orEmpty()
        }.getOrDefault(emptyList()).let { seriesItems ->
            LogUtil.d(
                "GetchuPreviewParser",
                "series ajax id=$id parentId=$parentId items=${seriesItems.size}"
            )
            if (seriesItems.isEmpty()) {
                detailState
            } else {
                val detail = detailState.info
                val mergedSeriesItems = (detail.seriesItems + seriesItems)
                    .distinctBy { it.id }
                    .filterNot { it.id == id }
                WebsiteState.Success(
                    detail.copy(
                        seriesItems = mergedSeriesItems,
                        relatedItems = mergedSeriesItems,
                    )
                )
            }
        }
    }
    private fun <T> websiteIOFlow(
        request: suspend () -> Response<ResponseBody>,
        permittedSuccessCode: IntArray? = null,
        bodyToString: (ResponseBody) -> String = ResponseBody::string,
        action: suspend (String) -> WebsiteState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = requestResult.body()?.let(bodyToString)
        val permitted = permittedSuccessCode?.contains(requestResult.code()) == true
        if ((permitted || requestResult.isSuccessful)) {
            emit(action.invoke(resultBody ?: EMPTY_STRING))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    private fun ResponseBody.getchuString(): String {
        return bytes().toString(GETCHU_CHARSET)
    }

    private fun String.extractGetchuSeriesParentId(): String? {
        return Regex("[\"']parent_id_array[\"']\\s*:\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }
}
