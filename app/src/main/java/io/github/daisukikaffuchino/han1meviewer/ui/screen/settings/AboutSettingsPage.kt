package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.daisukikaffuchino.han1meviewer.HA1_GITHUB_URL
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingInfoItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
internal fun AboutSettingsPage(
    state: HomeSettingsUiState,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onOpenUsageTerms: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        SettingsSection(stringResource(R.string.information)) {
        SettingInfoItem(
            title = stringResource(R.string.version),
            summary = state.versionSummary,
            iconRes = R.drawable.ic_info,
        )
        SettingNavigationItem(
            title = stringResource(R.string.developer),
            summary = "@daisukiKaffuChino",
            iconRes = R.drawable.ic_person,
            onClick = { uriHandler.openUri("https://github.com/daisukiKaffuChino") },
        )
        SettingNavigationItem(
            title = stringResource(R.string.user_terms),
            summary = stringResource(R.string.user_terms_summary),
            iconRes = R.drawable.ic_inbox_text,
            onClick = onOpenUsageTerms,
        )
    }
        SettingsSection("GitHub") {
        SettingNavigationItem(
            title = stringResource(R.string.project_repository),
            summary = "daisukiKaffuChino/Han1meViewer",
            iconRes = R.drawable.ic_ext_link,
            onClick = { uriHandler.openUri(HA1_GITHUB_URL) },
        )
        SettingNavigationItem(
            title = stringResource(R.string.submit_bug),
            summary = stringResource(R.string.submit_bug_summary),
            iconRes = R.drawable.ic_bug_report,
            onClick = onSubmitBug,
        )
        SettingNavigationItem(
            title = stringResource(R.string.forum),
            summary = stringResource(R.string.forum_summary),
            iconRes = R.drawable.ic_forum,
            onClick = onOpenForum,
        )
        SettingNavigationItem(
            title = stringResource(R.string.open_source_license),
            summary = stringResource(R.string.open_source_license_summary),
            iconRes = R.drawable.ic_gavel,
            onClick = onOpenOpenSourceLicense,
        )
        }
    }
}
