package io.github.daisukikaffuchino.han1meviewer.logic.parser

import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccount
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 用户中心页解析。
 */
internal object HanimeUserParser {

    fun userAccountPage(body: String): UiState<UserAccount> {
        val parseBody = Jsoup.parse(body).body()
        extractFormError(parseBody)?.let { errorMessage ->
            return UiState.Error(IllegalStateException(errorMessage))
        }

        val csrfToken = parseBody.selectFirst("meta[name=csrf-token]")?.attr("content")
            ?: parseBody.selectFirst("input[name=_token]")?.attr("value")
        val avatarElement = parseBody.selectFirst("img#playlist-avatar")
        val avatarUrl = avatarElement?.absUrl("src")?.ifBlank { avatarElement.attr("src") }
            .throwIfParseNull("userAccountPage", "avatarUrl")
        val username = parseBody.selectFirst("input[name=name]")?.attr("value")?.trim()
            .throwIfParseNull("userAccountPage", "username")
        val email = parseBody.selectFirst("input[name=email]")?.attr("value")?.trim()
            .throwIfParseNull("userAccountPage", "email")
        val userIdText = parseBody.selectFirst("div.profile-sub-stats-id")?.text()
            .throwIfParseNull("userAccountPage", "userId")
        val userId = Regex("""\d+""").find(userIdText)?.value
            .throwIfParseNull("userAccountPage", "userId")
        val joinedLabel = parseBody.selectFirst("#user-modal-created")?.text()?.trim()
        val statsText = parseBody.selectFirst("div.profile-sub-stats-new-line")?.text().orEmpty()
        val statNumbers = Regex("""\d+""").findAll(statsText)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()

        return UiState.Success(
            UserAccount(
                csrfToken = csrfToken,
                avatarUrl = avatarUrl,
                username = username,
                email = email,
                userId = userId,
                joinedLabel = joinedLabel,
                subscriberCount = statNumbers.getOrElse(0) { 0 },
                videoCount = statNumbers.getOrElse(1) { 0 },
            )
        )
    }

    private fun extractFormError(parseBody: Element): String? {
        val selectors = listOf(
            ".alert-danger",
            ".invalid-feedback",
            ".text-danger",
            ".help-block",
            ".error-message",
        )
        selectors.forEach { selector ->
            parseBody.select(selector)
                .map { it.text().trim() }
                .firstOrNull { it.isNotBlank() }
                ?.let { return it }
        }
        return null
    }
}
