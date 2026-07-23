package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.PageState
import io.github.daisukikaffuchino.han1meviewer.logic.state.dataOrNull
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeTopAppBar
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageEmpty
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageError
import io.github.daisukikaffuchino.han1meviewer.ui.component.isFirstPageLoading
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.util.toNetworkErrorMessageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetchuPreviewScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: GetchuPreviewViewModel,
) {
    var dateCode by rememberSaveable { mutableStateOf(currentGetchuDateCode()) }
    var monthMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val currentMonthCode = remember { currentGetchuDateCode() }
    val state = viewModel.previewFlow.collectAsStateWithLifecycle().value
    val dateLabel = remember(dateCode) { getchuDateLabel(dateCode) }
    val monthOptions = remember(currentMonthCode) { getchuMonthOptions(currentMonthCode) }
    val loadingHint = rememberRandomLoadingHint()
    val context = LocalContext.current
    val imageLoader = remember {
        createGetchuImageLoader(context)
    }
    LaunchedEffect(dateCode) { viewModel.getPreview(dateCode) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HanimeTopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { monthMenuExpanded = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.getchu_preview_title, dateLabel),
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp),
                    ) {
                        monthOptions.forEach { optionDateCode ->
                            DropdownMenuItem(
                                text = { Text(getchuDateLabel(optionDateCode)) },
                                onClick = {
                                    dateCode = optionDateCode
                                    monthMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            },
            onBack = onBack,
            actions = {
                IconButton(onClick = { dateCode = shiftGetchuMonthCode(dateCode, -1) }) {
                    Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, null)
                }
                IconButton(onClick = { dateCode = shiftGetchuMonthCode(dateCode, 1) }) {
                    Icon(Icons.AutoMirrored.Default.KeyboardArrowRight, null)
                }
            },
        )

        PageContent(
            isLoading = state.isFirstPageLoading,
            isError = state.isFirstPageError,
            isEmpty = state.isFirstPageEmpty || state.dataOrNull?.groups?.isEmpty() == true,
            errorMessage = (state as? PageState.Error)?.throwable?.toNetworkErrorMessageRes()?.let {
                stringResource(it)
            } ?: "",
            onRetry = { viewModel.getPreview(dateCode) },
            modifier = Modifier.fillMaxSize(),
            loadingMessage = loadingHint
        ) {
            state.dataOrNull?.let { preview ->
                GetchuPreviewContent(
                    preview = preview,
                    onOpenDetail = onNavigateToDetail,
                    imageLoader = imageLoader
                )
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
private fun GetchuPreviewScreenPreview() {
    ComponentPreview {
        GetchuPreviewScreen(
            onBack = {},
            onNavigateToDetail = { _ -> },
            viewModel = GetchuPreviewViewModel()
        )
    }
}
