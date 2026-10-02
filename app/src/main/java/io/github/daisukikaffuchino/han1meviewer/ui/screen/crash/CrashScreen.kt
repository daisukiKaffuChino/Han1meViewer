package io.github.daisukikaffuchino.han1meviewer.ui.screen.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingNavigationItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSectionTitle
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSegmentedGroup
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
fun CrashScreen(
    crashReport: String,
    packageName: String,
    onCopyLog: () -> Unit,
    onRestartApp: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val highlightedReport = remember(
        crashReport,
        packageName,
        primaryContainer,
        onPrimaryContainer,
    ) {
        val lines = crashReport.lines()
        buildAnnotatedString {
            lines.forEachIndexed { index, line ->
                if (packageName.isNotBlank() && line.contains(packageName)) {
                    withStyle(
                        SpanStyle(
                            color = onPrimaryContainer,
                            background = primaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(line)
                    }
                } else {
                    append(line)
                }
                if (index < lines.lastIndex) append('\n')
            }
        }
    }

    HanimeScaffold(
        title = stringResource(R.string.crash_page_title),
        onBack = null,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        vertical = HanimeDefaults.Spacing.small,
                    ),
            ) {
                Surface(
                    color = lerp(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.surface,
                        0.6f
                    ),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_error_outline),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.crash_unexpected_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.crash_unexpected_message),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                SettingsSectionTitle(titleRes = R.string.crash_actions)
                SettingsSegmentedGroup {
                    SettingNavigationItem(
                        title = stringResource(R.string.crash_copy_log),
                        summary = stringResource(R.string.crash_copy_log_summary),
                        iconRes = R.drawable.ic_bug_report,
                        onClick = onCopyLog,
                    )
                    SettingNavigationItem(
                        title = stringResource(R.string.crash_restart_app),
                        summary = stringResource(R.string.crash_restart_app_summary),
                        iconRes = R.drawable.ic_refresh,
                        onClick = onRestartApp,
                    )
                    SettingNavigationItem(
                        title = stringResource(R.string.crash_exit_app),
                        summary = stringResource(R.string.crash_exit_app_summary),
                        iconRes = R.drawable.ic_exit_to_app,
                        onClick = onExitApp,
                    )
                }

                SettingsSectionTitle(titleRes = R.string.crash_log_title)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = highlightedReport,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun CrashScreenPreview() {
    ComponentPreview {
        CrashScreen(
            crashReport = """
                App: Han1meViewer 26.3.0 (260802)
                Package: io.github.daisukikaffuchino.han1meviewer.debug
                Device: Google Pixel
                Android: 16 (API 36)
                Thread: main

                ====== beginning of crash ======
                java.lang.RuntimeException: Crash triggered from developer options
                    at io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings.HomeSettingsRoute
            """.trimIndent(),
            packageName = "io.github.daisukikaffuchino.han1meviewer",
            onCopyLog = {},
            onRestartApp = {},
            onExitApp = {},
        )
    }
}
