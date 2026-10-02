package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.preview.getchupreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import io.github.daisukikaffuchino.han1meviewer.logic.model.GetchuPreview
import io.github.daisukikaffuchino.han1meviewer.ui.component.lazy.LazyColumn
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.preview.fakeGetchuPreview

@Composable
internal fun GetchuPreviewContent(
    preview: GetchuPreview,
    onOpenDetail: (String) -> Unit,
    imageLoader: ImageLoader
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        preview.groups.forEach { group ->
            item(key = "date-${group.releaseDate}") {
                Text(
                    text = group.releaseDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(group.items, key = { it.id }) { item ->
                GetchuPreviewItemCard(
                    item = item,
                    onClick = { onOpenDetail(item.id) },
                    imageLoader = imageLoader
                )
            }
        }
    }
}

@Preview
@Composable
private fun GetchuPreviewContentPreview() {
    ComponentPreview {
        val context = LocalContext.current
        GetchuPreviewContent(
            preview = fakeGetchuPreview,
            onOpenDetail = {},
            imageLoader = ImageLoader(context).newBuilder().build()
        )
    }
}
