@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Scm
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.theme.animatedShape
import io.github.daisukikaffuchino.han1meviewer.ui.theme.fadeScale

private data class DisplayLicense(
    val name: String,
    val url: String? = null,
    val content: String? = null,
)

private data class SelectedLicenseDialog(
    val libraryName: String,
    val licenseName: String,
    val content: String,
)

private data class LicenseItem(
    val library: Library,
    val displayLicenses: List<DisplayLicense>,
)

private val MOMO_QR_LICENSE = License(
    name = "GNU General Public License v3.0",
    url = "https://www.gnu.org/licenses/gpl-3.0.html",
    spdxId = "GPL-3.0-only",
    licenseContent = "MomoQR is licensed under the GNU General Public License v3.0.\n\n" +
            "https://www.gnu.org/licenses/gpl-3.0.html",
    hash = "GPL-3.0-only",
)

private val MOMO_QR_LIBRARY = Library(
    uniqueId = "github.daisukikaffuchino:momoqr",
    artifactVersion = null,
    name = "MomoQR",
    description = "A modern QR code and barcode scanner built with Jetpack Compose.",
    website = "https://github.com/daisukiKaffuChino/MomoQR",
    developers = listOf(
        Developer(
            name = "daisukiKaffuChino",
            organisationUrl = "https://github.com/daisukiKaffuChino",
        ),
    ),
    organization = null,
    scm = Scm(
        connection = null,
        developerConnection = null,
        url = "https://github.com/daisukiKaffuChino/MomoQR",
    ),
    licenses = setOf(MOMO_QR_LICENSE),
)

@Composable
fun OpenSourceLicensesScreen(
    searchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val uriHandler = LocalUriHandler.current
    val searchFieldState = rememberTextFieldState()
    val transitionSpec = fadeScale()
    var selectedLicenseDialog by rememberSaveable(
        stateSaver = listSaver(
            save = {
                it?.let { dialog ->
                    listOf(dialog.libraryName, dialog.licenseName, dialog.content)
                } ?: emptyList()
            },
            restore = {
                it.takeIf { saved -> saved.size == 3 }?.let { saved ->
                    SelectedLicenseDialog(
                        libraryName = saved[0],
                        licenseName = saved[1],
                        content = saved[2],
                    )
                }
            },
        )
    ) { mutableStateOf<SelectedLicenseDialog?>(null) }

    val licenseItems = remember(libraries) {
        (libraries?.libraries.orEmpty() + MOMO_QR_LIBRARY)
            .distinctBy { it.uniqueId }
            .map { library ->
                LicenseItem(
                    library = library,
                    displayLicenses = library.licenses.map { license ->
                        DisplayLicense(
                            name = license.name.ifBlank { "Unknown License" },
                            url = license.url,
                            content = license.htmlReadyLicenseContent?.takeIf { it.isNotBlank() }
                                ?: license.licenseContent?.takeIf { it.isNotBlank() },
                        )
                    },
                )
            }
    }

    val filteredLicenseItems = remember(licenseItems, searchMode, searchFieldState.text) {
        if (!searchMode) {
            licenseItems
        } else {
            val keyword = searchFieldState.text.toString()
            licenseItems.filter { item ->
                item.library.name.contains(keyword, ignoreCase = true) ||
                        item.library.author.contains(keyword, ignoreCase = true) ||
                        item.library.artifactId.contains(keyword, ignoreCase = true) ||
                        item.displayLicenses.any { license ->
                            license.name.contains(keyword, ignoreCase = true) ||
                                    license.url.orEmpty().contains(keyword, ignoreCase = true) ||
                                    license.content.orEmpty().contains(keyword, ignoreCase = true)
                        }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = searchMode,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + expandVertically(
                MaterialTheme.motionScheme.fastSpatialSpec(),
            ),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + shrinkVertically(
                MaterialTheme.motionScheme.fastSpatialSpec(),
            ),
        ) {
            LicenseSearchTextField(
                searchMode = searchMode,
                onSearchModeChange = onSearchModeChange,
                textFieldState = searchFieldState,
            )
        }

        AnimatedContent(
            targetState = filteredLicenseItems.isEmpty(),
            transitionSpec = { transitionSpec },
        ) { isEmpty ->
            if (isEmpty) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LicenseEmptyTip(
                        searchMode = searchMode,
                        size = 96.dp,
                    )
                    Text(
                        text = stringResource(
                            if (searchMode) {
                                R.string.no_licenses_found
                            } else {
                                R.string.no_license_items
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(HanimeDefaults.settingsItemPadding),
                ) {
                    item { Spacer(Modifier.size(4.dp)) }
                    items(
                        items = filteredLicenseItems,
                        key = { it.library.uniqueId },
                    ) { item ->
                        LicenseLibraryItem(
                            item = item,
                            onClick = {
                                val license = item.displayLicenses.firstOrNull()
                                    ?: return@LicenseLibraryItem
                                when {
                                    !license.content.isNullOrBlank() -> {
                                        selectedLicenseDialog = SelectedLicenseDialog(
                                            libraryName = item.library.name,
                                            licenseName = license.name,
                                            content = license.content,
                                        )
                                    }

                                    !license.url.isNullOrBlank() -> {
                                        try {
                                            uriHandler.openUri(license.url)
                                        } catch (_: Throwable) {
                                        }
                                    }
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.size(4.dp)) }
                }
            }
        }
    }

    selectedLicenseDialog?.let { dialog ->
        LicenseContentDialog(
            dialog = dialog,
            onDismiss = { selectedLicenseDialog = null },
        )
    }
}

@Composable
private fun LicenseSearchTextField(
    searchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
) {
    val view = LocalView.current
    TextField(
        modifier = Modifier.fillMaxWidth(),
        state = textFieldState,
        shape = CircleShape,
        placeholder = { Text(stringResource(R.string.search)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = textFieldState.text.isNotBlank(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear),
                    )
                }
            }
        },
    )
}

@Composable
private fun LicenseLibraryItem(
    item: LicenseItem,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = animatedShape(HanimeDefaults.largerShapes(), interactionSource)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                },
            ),
        shape = shape,
        color = HanimeDefaults.Colors.Container,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    horizontal = HanimeDefaults.settingsItemHorizontalPadding,
                    vertical = HanimeDefaults.settingsItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = item.library.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val author = item.library.author
                    if (author.isNotBlank()) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                        )
                    }
                }
                if (item.displayLicenses.isNotEmpty()) {
                    FlowRow {
                        item.displayLicenses.forEach { license ->
                            Text(
                                text = license.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmallEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseEmptyTip(
    searchMode: Boolean,
    size: androidx.compose.ui.unit.Dp,
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(HanimeDefaults.screenHorizontalPadding)
            .size(size)
            .clip(MaterialShapes.Cookie7Sided.toShape())
            .background(containerColor),
    ) {
        Icon(
            painter = painterResource(
                if (searchMode) {
                    R.drawable.ic_license_search_not_found
                } else {
                    R.drawable.ic_license_list_no_item
                },
            ),
            contentDescription = null,
            tint = contentColorFor(containerColor),
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
private fun LicenseContentDialog(
    dialog: SelectedLicenseDialog,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(dialog.licenseName.ifBlank { dialog.libraryName })
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(text = AnnotatedString.fromHtml(dialog.content))
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
    )
}
