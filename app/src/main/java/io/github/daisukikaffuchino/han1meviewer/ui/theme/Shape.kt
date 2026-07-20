package io.github.daisukikaffuchino.han1meviewer.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun shapeByInteraction(
    shapes: ButtonShapes,
    pressed: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val normal = shapes.shape
    val pressedShape = shapes.pressedShape
    if (normal !is CornerBasedShape || pressedShape !is CornerBasedShape) {
        return if (pressed) pressedShape else normal
    }

    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = animationSpec,
        label = "interactive-shape-progress",
    )
    return remember(normal, pressedShape, progress) {
        InterpolatedCornerShape(normal, pressedShape, progress)
    }
}

private class InterpolatedCornerShape(
    private val start: CornerBasedShape,
    private val end: CornerBasedShape,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        fun radius(startPx: Float, endPx: Float): CornerRadius {
            val value = startPx + (endPx - startPx) * progress
            return CornerRadius(value.coerceIn(0f, size.minDimension / 2f))
        }

        val topStart = radius(
            start.topStart.toPx(size, density),
            end.topStart.toPx(size, density),
        )
        val topEnd = radius(
            start.topEnd.toPx(size, density),
            end.topEnd.toPx(size, density),
        )
        val bottomStart = radius(
            start.bottomStart.toPx(size, density),
            end.bottomStart.toPx(size, density),
        )
        val bottomEnd = radius(
            start.bottomEnd.toPx(size, density),
            end.bottomEnd.toPx(size, density),
        )
        val leftToRight = layoutDirection == LayoutDirection.Ltr
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = if (leftToRight) topStart else topEnd,
                topRightCornerRadius = if (leftToRight) topEnd else topStart,
                bottomRightCornerRadius = if (leftToRight) bottomEnd else bottomStart,
                bottomLeftCornerRadius = if (leftToRight) bottomStart else bottomEnd,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun animatedShape(
    shapes: ButtonShapes,
    interactionSource: MutableInteractionSource? = null,
): Shape {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return shapeByInteraction(
        shapes = shapes,
        pressed = pressed,
        animationSpec = HanimeDefaults.shapesDefaultAnimationSpec,
    )
}
