package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults

/** Shared plain container surface used by cards and settings items. */
@Composable
fun CardContainerSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    color: Color? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color ?: HanimeDefaults.Colors.Container,
        content = content,
    )
}
