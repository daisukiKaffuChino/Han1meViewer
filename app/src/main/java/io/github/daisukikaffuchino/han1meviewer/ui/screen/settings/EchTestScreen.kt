package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.ConscryptEch
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchDiagnosticReport
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchDiagnostics
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchLog
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchLogEntry
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchLogLevel
import io.github.daisukikaffuchino.han1meviewer.logic.network.ech.HyEchH3
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingInfoItem
import io.github.daisukikaffuchino.han1meviewer.ui.component.SettingsSectionTitle
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOG_HEADER_ITEM_COUNT = 1

enum class EchTestPanel {
    Debug,
    Logs,
}

@Composable
fun EchTestScreen(
    initialHost: String,
) {
    var host by rememberSaveable(initialHost) { mutableStateOf(initialHost) }
    var isRunning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<EchDiagnosticReport?>(null) }
    var selectedPanel by rememberSaveable { mutableStateOf(EchTestPanel.Debug) }
    val logs by EchLog.entries.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    LaunchedEffect(selectedPanel, logs.size) {
        if (selectedPanel == EchTestPanel.Logs && logs.isNotEmpty()) {
            listState.animateScrollToItem(LOG_HEADER_ITEM_COUNT + logs.lastIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = HanimeDefaults.Corners.large,
        color = HanimeDefaults.Colors.pageSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedPanel.ordinal) {
                Tab(
                    selected = selectedPanel == EchTestPanel.Debug,
                    onClick = { selectedPanel = EchTestPanel.Debug },
                    text = { Text(stringResource(R.string.debug)) },
                )
                Tab(
                    selected = selectedPanel == EchTestPanel.Logs,
                    onClick = { selectedPanel = EchTestPanel.Logs },
                    text = { Text(stringResource(R.string.ech_logs)) },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedPanel) {
                    EchTestPanel.Debug -> EchDebugPanel(
                        host = host,
                        onHostChange = { host = it.trim() },
                        report = report,
                        isRunning = isRunning,
                        onRunTest = {
                            if (!isRunning) {
                                isRunning = true
                                coroutineScope.launch {
                                    report = runCatching { EchDiagnostics.run(host) }
                                        .getOrElse { throwable ->
                                            EchLog.e(
                                                "HY-ECH-TEST",
                                                "Unexpected failure: ${throwable.message}",
                                            )
                                            EchDiagnosticReport(
                                                host = host,
                                                resolvedAddresses = emptyList(),
                                                echConfigBytes = 0,
                                                httpStatus = 0,
                                                protocol = "",
                                                elapsedMillis = 0,
                                                h3Result = "Failed",
                                                error = throwable.message
                                                    ?: throwable.javaClass.simpleName,
                                            )
                                        }
                                    isRunning = false
                                }
                            }
                        },
                    )

                    EchTestPanel.Logs -> EchLogPanel(
                        logs = logs,
                        listState = listState,
                        timeFormat = timeFormat,
                        onClearLogs = EchLog::clear,
                    )
                }
            }
        }
    }
}

@Composable
private fun EchDebugPanel(
    host: String,
    onHostChange: (String) -> Unit,
    report: EchDiagnosticReport?,
    isRunning: Boolean,
    onRunTest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.small),
    ) {
        item {
            SettingsSection(stringResource(R.string.ech_test_configuration)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HanimeDefaults.Spacing.itemHorizontal),
                    label = { Text(stringResource(R.string.ech_test_host)) },
                    singleLine = true,
                )
            }
        }

        item {
            SettingsSection(stringResource(R.string.ech_test_status)) {
                SettingInfoItem(
                    title = stringResource(R.string.ech_conscrypt_status),
                    valueText = stringResource(
                        if (ConscryptEch.ready) R.string.ech_ready else R.string.ech_not_ready,
                    ),
                    iconRes = R.drawable.ic_security_update,
                )
                SettingInfoItem(
                    title = stringResource(R.string.ech_h3_status),
                    valueText = stringResource(
                        if (HyEchH3.isNativeLoaded()) R.string.ech_ready else R.string.ech_not_ready,
                    ),
                    iconRes = R.drawable.ic_router,
                )
                report?.let { result ->
                    SettingInfoItem(
                        title = stringResource(R.string.ech_doh_result),
                        valueText = result.resolvedAddresses.joinToString().ifBlank {
                            stringResource(R.string.ech_empty_result)
                        },
                        iconRes = R.drawable.ic_dns,
                    )
                    SettingInfoItem(
                        title = stringResource(R.string.ech_config_result),
                        valueText = if (result.echConfigBytes > 0) {
                            stringResource(R.string.ech_config_bytes, result.echConfigBytes)
                        } else {
                            stringResource(R.string.ech_empty_result)
                        },
                        iconRes = R.drawable.ic_code,
                    )
                    SettingInfoItem(
                        title = stringResource(R.string.ech_http_result),
                        valueText = if (result.httpStatus > 0) {
                            "HTTP ${result.httpStatus} · ${result.protocol} · ${result.elapsedMillis} ms"
                        } else {
                            result.error ?: stringResource(R.string.ech_empty_result)
                        },
                        iconRes = R.drawable.ic_speed,
                    )
                    SettingInfoItem(
                        title = stringResource(R.string.ech_h3_test),
                        valueText = result.h3Result,
                        iconRes = R.drawable.ic_router,
                    )
                }
                if (isRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            HapticButton(
                onClick = onRunTest,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HanimeDefaults.Spacing.contentHorizontal),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (isRunning) R.string.ech_test_running else R.string.run_ech_test,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EchLogPanel(
    logs: List<EchLogEntry>,
    listState: LazyListState,
    timeFormat: SimpleDateFormat,
    onClearLogs: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.extraSmall),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HanimeDefaults.Spacing.contentHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsSectionTitle(
                        title = stringResource(R.string.ech_logs) + "  ${logs.size}",
                    )
                }
                HapticTextButton(
                    onClick = onClearLogs,
                    enabled = logs.isNotEmpty(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clear_all),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(R.string.clear_logs),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.ech_logs_empty),
                    modifier = Modifier.padding(
                        horizontal = HanimeDefaults.Spacing.contentHorizontal,
                        vertical = 16.dp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(
                items = logs,
                key = { index, entry -> "${entry.timestampMillis}-$index" },
            ) { _, entry ->
                EchLogRow(entry = entry, timeFormat = timeFormat)
            }
        }
    }
}

@Composable
private fun EchLogRow(
    entry: EchLogEntry,
    timeFormat: SimpleDateFormat,
) {
    val levelColor = when (entry.level) {
        EchLogLevel.Debug -> MaterialTheme.colorScheme.onSurfaceVariant
        EchLogLevel.Info -> MaterialTheme.colorScheme.primary
        EchLogLevel.Warning -> MaterialTheme.colorScheme.tertiary
        EchLogLevel.Error -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = HanimeDefaults.Spacing.contentHorizontal,
                vertical = 6.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeFormat.format(Date(entry.timestampMillis)),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.level.name.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = levelColor,
            )
            Text(
                text = entry.tag,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectionContainer {
            Text(
                text = entry.message,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (entry.level == EchLogLevel.Error) levelColor else Color.Unspecified,
            )
        }
    }
}
