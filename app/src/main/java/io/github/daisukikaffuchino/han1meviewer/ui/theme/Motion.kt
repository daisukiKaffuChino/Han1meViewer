package io.github.daisukikaffuchino.han1meviewer.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private const val ProgressThreshold = 0.35f
const val DefaultMotionDuration = 300

private val Int.forOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

private val Int.forIncoming: Int
    get() = this - forOutgoing

fun materialSharedAxisX(
    initialOffsetX: (fullWidth: Int) -> Int,
    targetOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = DefaultMotionDuration,
): ContentTransform = ContentTransform(
    targetContentEnter = materialSharedAxisXIn(initialOffsetX, durationMillis),
    initialContentExit = materialSharedAxisXOut(targetOffsetX, durationMillis),
)

fun materialSharedAxisXIn(
    initialOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = DefaultMotionDuration,
): EnterTransition = slideInHorizontally(
    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
    initialOffsetX = initialOffsetX,
) + fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.forIncoming,
        delayMillis = durationMillis.forOutgoing,
        easing = LinearOutSlowInEasing,
    ),
)

fun materialSharedAxisXOut(
    targetOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = DefaultMotionDuration,
): ExitTransition = slideOutHorizontally(
    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
    targetOffsetX = targetOffsetX,
) + fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.forOutgoing,
        easing = FastOutLinearInEasing,
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun fadeScaleIn(
    effectSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastEffectsSpec(),
    spatialSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastSpatialSpec(),
): EnterTransition = fadeIn(effectSpec) + scaleIn(
    animationSpec = spatialSpec,
    initialScale = 0.92f,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun fadeScaleOut(
    effectSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastEffectsSpec(),
): ExitTransition = fadeOut(effectSpec)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun fadeScale(
    effectSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastEffectsSpec(),
    spatialSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.fastSpatialSpec(),
): ContentTransform = ContentTransform(
    targetContentEnter = fadeScaleIn(effectSpec, spatialSpec),
    initialContentExit = fadeScaleOut(effectSpec),
)
