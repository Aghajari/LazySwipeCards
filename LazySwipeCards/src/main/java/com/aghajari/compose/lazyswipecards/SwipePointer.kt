package com.aghajari.compose.lazyswipecards

import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

internal fun Modifier.swipeListener(
    state: LazySwipeCardsState,
    rotateDegree: Float,
    translateSize: Dp,
    swipeThreshold: Float,
    minRatioBound: Float,
    animationSpec: AnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
) = this.composed {
    val density = LocalDensity.current.density
    var padding by remember { mutableFloatStateOf(0f) }

    onSizeChanged {
        val transform = Matrix()
        transform.setRotate(rotateDegree, it.width / 2f, it.height / 2f)
        val points = FloatArray(2) { 0f }
        transform.mapPoints(points)
        padding = points[0].absoluteValue + translateSize.value * density
        state.bound = it.width + padding
    }.pointerInput(Unit) {
        val decay = splineBasedDecay<Float>(this)
        coroutineScope {
            while (true) {
                val velocity = awaitSwipe(
                    coroutineScope = this,
                    offsetX = state.offsetXAnimatable,
                )
                val ratio = calculateRatio(
                    offsetX = state.offsetX,
                    width = state.viewportWidth,
                    swipeThreshold = swipeThreshold,
                ).absoluteValue
                state.bound = size.width.toFloat() + padding

                afterSwipe(
                    coroutineScope = this,
                    offsetX = state.offsetXAnimatable,
                    bound = state.bound,
                    velocity = velocity,
                    ratio = ratio,
                    minRatioBound = minRatioBound,
                    animationSpec = animationSpec,
                    decay = decay,
                    onSwipe = onSwipe,
                )
            }
        }
    }
}

private suspend fun PointerInputScope.awaitSwipe(
    coroutineScope: CoroutineScope,
    offsetX: Animatable<Float, AnimationVector1D>,
): Float {
    offsetX.stop()
    val velocityTracker = VelocityTracker()
    awaitPointerEventScope {
        val pointerId = awaitFirstDown().id

        horizontalDrag(pointerId) { change ->
            coroutineScope.launch {
                offsetX.snapTo(
                    targetValue = offsetX.value + change.positionChange().x,
                )
            }
            velocityTracker.addPosition(
                change.uptimeMillis,
                change.position,
            )
        }
    }
    return velocityTracker.calculateVelocity().x
}

private fun afterSwipe(
    coroutineScope: CoroutineScope,
    offsetX: Animatable<Float, AnimationVector1D>,
    velocity: Float,
    ratio: Float,
    bound: Float,
    minRatioBound: Float,
    animationSpec: AnimationSpec<Float>,
    decay: DecayAnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
) {
    val targetOffsetX = decay.calculateTargetValue(
        initialValue = offsetX.value,
        initialVelocity = velocity,
    )

    coroutineScope.launch {
        if (targetOffsetX.absoluteValue < bound) {
            if (ratio >= minRatioBound) {
                offsetX.animateTo(
                    targetValue = if (offsetX.value > 0) {
                        offsetX.upperBound!!
                    } else {
                        offsetX.lowerBound!!
                    },
                    animationSpec = animationSpec,
                    initialVelocity = velocity,
                )
            } else {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = animationSpec,
                    initialVelocity = velocity,
                )
            }
        } else {
            offsetX.animateDecay(
                initialVelocity = velocity,
                animationSpec = decay,
            )
        }
    }.invokeOnCompletion { error ->
        coroutineScope.launch {
            if (error == null &&
                (targetOffsetX.absoluteValue >= bound ||
                        ratio >= minRatioBound)
            ) {
                onSwipe.invoke(targetOffsetX)
                offsetX.snapTo(0f)
            }
        }
    }
}