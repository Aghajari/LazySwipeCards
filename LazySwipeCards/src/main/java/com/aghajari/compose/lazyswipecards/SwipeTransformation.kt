package com.aghajari.compose.lazyswipecards

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.absoluteValue

internal fun Modifier.swipe(
    cardIndex: Int,
    state: LazySwipeCardsState,
    rotateDegree: Float,
    minRatioBound: Float,
    scaleFactor: ScaleFactor,
    translateSize: Dp,
    visibleItemCount: Int,
    swipeThreshold: Float,
    animationSpec: AnimationSpec<Float>,
    onSwipe: (Float) -> Unit,
): Modifier {
    return this then if (cardIndex == 0) {
        swipeListener(
            state = state,
            rotateDegree = rotateDegree,
            translateSize = translateSize,
            swipeThreshold = swipeThreshold,
            minRatioBound = minRatioBound,
            animationSpec = animationSpec,
            onSwipe = onSwipe,
        ).graphicsLayer(
            translationX = state.offsetX,
            rotationZ = rotateDegree * state.ratio,
        )
    } else {
        applyTransformation(
            cardIndex = cardIndex,
            ratio = state.ratio,
            scaleFactor = scaleFactor,
            translateSize = translateSize,
            visibleItemCount = visibleItemCount,
        )
    }
}

private fun Modifier.applyTransformation(
    cardIndex: Int,
    ratio: Float,
    scaleFactor: ScaleFactor,
    translateSize: Dp,
    visibleItemCount: Int,
) = this.composed {
    var itemHeight by remember { mutableIntStateOf(0) }
    val indexWithRatio = if (cardIndex == visibleItemCount) {
        visibleItemCount - 1f
    } else {
        cardIndex - ratio.absoluteValue
    }
    val scaleY = 1f - indexWithRatio * scaleFactor.scaleY
    val scaleX = 1f - indexWithRatio * scaleFactor.scaleX
    val defY = indexWithRatio * translateSize.value *
            LocalDensity.current.density

    onSizeChanged { itemHeight = it.height }
        .graphicsLayer(
            scaleX = scaleX,
            scaleY = scaleY,
            translationY = defY + (itemHeight * (1f - scaleY)) / 2f,
        )
}