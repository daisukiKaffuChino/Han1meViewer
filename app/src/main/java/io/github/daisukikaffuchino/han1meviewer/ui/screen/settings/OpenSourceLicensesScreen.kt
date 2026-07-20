@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.github.daisukikaffuchino.han1meviewer.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Scm
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

@Composable
fun OpenSourceLicensesScreen(
    searchMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val generatedLibraries by produceLibraries(R.raw.aboutlibraries)
    val libraries = remember(generatedLibraries) {
        generatedLibraries?.let { generated ->
            generated.copy(
                libraries = (generated.libraries + MOMO_QR_LIBRARY)
                    .sortedBy { it.name.lowercase() },
                licenses = generated.licenses + MOMO_QR_LICENSE,
            )
        }
    }
    OpenSourceLicensesContent(
        libraries = libraries,
        searchMode = searchMode,
        modifier = modifier,
    )
}

@Composable
private fun OpenSourceLicensesContent(
    libraries: Libs?,
    searchMode: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val displayedLibraries = remember(libraries, searchMode, query) {
        if (!searchMode || query.isBlank()) {
            libraries
        } else {
            libraries?.let { source ->
                source.copy(
                    libraries = source.libraries.filter { it.matches(query.trim()) },
                )
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = searchMode,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + expandVertically(
                MaterialTheme.motionScheme.fastSpatialSpec(),
            ),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + shrinkVertically(
                MaterialTheme.motionScheme.fastSpatialSpec(),
            ),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HanimeDefaults.screenVerticalPadding, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_licenses)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
            )
        }

        if (displayedLibraries != null && displayedLibraries.libraries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_licenses_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LibrariesContainer(
                libraries = displayedLibraries,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun Library.matches(keyword: String): Boolean =
    name.contains(keyword, ignoreCase = true) ||
        artifactId.contains(keyword, ignoreCase = true) ||
        description.orEmpty().contains(keyword, ignoreCase = true) ||
        website.orEmpty().contains(keyword, ignoreCase = true) ||
        developers.any { developer ->
            developer.name.orEmpty().contains(keyword, ignoreCase = true) ||
                developer.organisationUrl.orEmpty().contains(keyword, ignoreCase = true)
        } ||
        licenses.any { license ->
            license.name.contains(keyword, ignoreCase = true) ||
                license.url.orEmpty().contains(keyword, ignoreCase = true) ||
                license.licenseContent.orEmpty().contains(keyword, ignoreCase = true)
        }

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

@Preview(showBackground = true, widthDp = 420, heightDp = 800)
@Composable
private fun OpenSourceLicensesPreview() {
    ComponentPreview {
        OpenSourceLicensesContent(
            libraries = Libs(
                libraries = listOf(MOMO_QR_LIBRARY),
                licenses = setOf(MOMO_QR_LICENSE),
            ),
            searchMode = true,
        )
    }
}
