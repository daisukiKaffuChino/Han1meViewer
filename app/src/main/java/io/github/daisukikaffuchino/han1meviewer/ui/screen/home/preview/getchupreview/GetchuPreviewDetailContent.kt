package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.ui.component.CardContainerSurface
import io.github.daisukikaffuchino.han1meviewer.ui.component.OutlinedButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyRow
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeGetchuPreviewDetail
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticButton as Button
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton

@Composable
internal fun GetchuPreviewDetailContent(
    detail: GetchuPreviewDetail,
    onOpenImage: (Int, List<String>) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
    imageLoader: ImageLoader,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CardContainerSurface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        AsyncImage(
                            model = getchuImageRequest(detail.coverUrl),
                            imageLoader = imageLoader,
                            contentDescription = detail.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            placeholder = painterResource(R.drawable.h_chan_loading),
                            error = painterResource(R.drawable.h_chan_load_failed)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )

                        SelectionContainer(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = detail.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            detail.brand?.let {
                                Text(
                                    text = "${stringResource(R.string.brand)}: $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            detail.releaseDate?.let {
                                Text(
                                    text = "${stringResource(R.string.release_date)}: $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            detail.price?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary // 价格高亮
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (detail.videoUrls.isNotEmpty()) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToVideoUrl(detail.videoUrls.first()) }
                                ) {
                                    Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.play_trailer))
                                }
                            }

                            OutlinedButton(
                                 modifier = Modifier.weight(1f),
                                 onClick = { uriHandler.openUri(detail.productUrl) }
                             ) {
                                 Icon(painterResource(R.drawable.ic_ext_link), contentDescription = null)
                                 Spacer(Modifier.width(4.dp))
                                 Text(stringResource(R.string.jump_to_webpage))
                             }
                        }

                        if (detail.videoUrls.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail.videoUrls.drop(1).forEachIndexed { index, url ->
                                    TextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onNavigateToVideoUrl(url) },
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Icon(painterResource(R.drawable.ic_play_circle), contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${stringResource(R.string.play_trailer)} ${index + 2}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val textSections = detail.sections.ifEmpty {
            detail.description
                ?.takeIf { it.isNotBlank() }
                ?.let { description ->
                    listOf(
                        GetchuPreviewDetail.TextSection(
                            title = "商品紹介",
                            body = description,
                        )
                    )
                }
                .orEmpty()
        }
        items(textSections, key = { it.title }) { section ->
            GetchuTextSection(section = section)
        }

        if (detail.sampleImages.isNotEmpty()) {
            item {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            detail.sampleImages,
                            key = { index, url -> "$index-$url" }) { index, url ->
                            CardContainerSurface(
                                onClick = { onOpenImage(index, detail.sampleImages) },
                            ) {
                                AsyncImage(
                                    model = getchuImageRequest(url),
                                    imageLoader = imageLoader,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(96.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }

        val relatedItems = detail.relatedItems.ifEmpty { detail.seriesItems }
        if (relatedItems.isNotEmpty()) {
            item {
                GetchuRelatedRow(
                    title = stringResource(R.string.getchu_series),
                    items = relatedItems,
                    onNavigateToDetail = onNavigateToDetail,
                    imageLoader = imageLoader
                )
            }
        }
    }
}


@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
)
@Composable
private fun GetchuPreviewDetailContentPreview() {
    ComponentPreview {
        Box(Modifier.fillMaxSize()) {
            val context = LocalContext.current
            GetchuPreviewDetailContent(
                detail = fakeGetchuPreviewDetail,
                onOpenImage = { _, _ -> },
                onNavigateToDetail = {},
                onNavigateToVideoUrl = {},
                imageLoader = ImageLoader(context).newBuilder().build()
            )
        }
    }
}
