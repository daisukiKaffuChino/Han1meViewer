package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

/** Shows the press indication immediately while keeping clickable semantics and scroll cancellation. */
@Composable
fun Modifier.immediateClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier {
    val clickInteractionSource = remember { MutableInteractionSource() }
    return this
        .indication(interactionSource, ripple())
        .pointerInput(enabled, interactionSource) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                var completed = false
                try {
                    val up = waitForUpOrCancellation()
                    interactionSource.tryEmit(
                        if (up == null) PressInteraction.Cancel(press)
                        else PressInteraction.Release(press),
                    )
                    completed = true
                } finally {
                    if (!completed) {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                    }
                }
            }
        }
        .clickable(
            enabled = enabled,
            role = role,
            interactionSource = clickInteractionSource,
            indication = null,
            onClick = onClick,
        )
}
