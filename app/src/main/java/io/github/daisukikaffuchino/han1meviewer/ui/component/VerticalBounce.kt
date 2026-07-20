package io.github.daisukikaffuchino.han1meviewer.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("RememberInComposition")
@Stable
fun Modifier.verticalBounce(
    enabled: Boolean = true,
    maxOffset: Float = 100f,
    dragMultiplier: Float = 0.15f,
    settleBackMultiplier: Float = 0.3f,
): Modifier = composed {
    if (!enabled) return@composed this

    val scope = rememberCoroutineScope()
    val offsetY = Animatable(0f)
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || offsetY.value == 0f) return Offset.Zero
            val draggingBack =
                (offsetY.value > 0f && available.y < 0f) ||
                    (offsetY.value < 0f && available.y > 0f)
            if (!draggingBack) return Offset.Zero
            val target = (offsetY.value + available.y * settleBackMultiplier)
                .coerceIn(-maxOffset, maxOffset)
            scope.launch { offsetY.snapTo(target) }
            return Offset(0f, available.y)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
            val target = (offsetY.value + available.y * dragMultiplier)
                .coerceIn(-maxOffset, maxOffset)
            scope.launch { offsetY.snapTo(target) }
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (abs(offsetY.value) <= 0.5f) return Velocity.Zero
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            return available
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (abs(offsetY.value) > 0.5f) {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
            return Velocity.Zero
        }
    }

    nestedScroll(connection).graphicsLayer { translationY = offsetY.value }
}
