package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import org.jsoup.Jsoup

/**
 * 登录页解析。
 */
internal object HanimeLoginParser {

    fun extractTokenFromLoginPage(body: String): String {
        val parseBody = Jsoup.parse(body).body()
        return parseBody.selectFirst("input[name=_token]")?.attr("value")
            ?: throw ParseException("Can't find csrf token from login page.")
    }
}
