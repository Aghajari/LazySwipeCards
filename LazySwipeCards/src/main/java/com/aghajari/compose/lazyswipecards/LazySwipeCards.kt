package com.aghajari.compose.lazyswipecards

import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.max

@Composable
fun LazySwipeCards(
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    cardShape: Shape = RectangleShape,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    cardContentColor: Color = contentColorFor(cardColor),
    cardTonalElevation: Dp = 0.dp,
    cardShadowElevation: Dp = 2.dp,
    cardBorder: BorderStroke? = null,
    scaleFactor: ScaleFactor = ScaleFactor(
        scaleX = 0.1f,
        scaleY = 0.1f
    ),
    translateSize: Dp = 24.dp,
    rotateDegree: Float = 14f,
    visibleItemCount: Int = 3,
    contentPadding: PaddingValues = PaddingValues(
        vertical = translateSize * visibleItemCount,
        horizontal = translateSize
    ),
    swipeThreshold: Float = 0.5f,
    minRatioBound: Float = MAX_RATIO,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    isEndless: Boolean = false,
    frontItemIndex: MutableState<Int> = remember { mutableIntStateOf(0) },
    content: LazySwipeCardsScope.() -> Unit,
) {
    require(swipeThreshold > 0f && swipeThreshold <= 1f) {
        "swipeThreshold must be > 0 and <= 1"
    }
    require(visibleItemCount >= 1) {
        "visibleItemCount must be >= 1"
    }

    @Composable
    fun Card(
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        Surface(
            modifier = modifier.then(cardModifier),
            shape = cardShape,
            color = cardColor,
            contentColor = cardContentColor,
            tonalElevation = cardTonalElevation,
            shadowElevation = cardShadowElevation,
            border = cardBorder,
            content = content
        )
    }

    val offsetX = remember { Animatable(0f) }
    val width = remember { mutableIntStateOf(1) }
    val ratio = calculateRatio(offsetX.value, width.intValue, swipeThreshold)

    val itemProvider = rememberLazySwipeCardsItemProvider(
        content = content,
        frontIndex = frontItemIndex,
        isEndless = isEndless,
    )
    itemProvider.onSwiping(offsetX.value, ratio)

    fun Modifier.applyTransformation(cardIndex: Int) = composed {
        if (cardIndex == 0) {
            swipeListener(
                offsetX = offsetX,
                width = width,
                rotateDegree = rotateDegree,
                translateSize = translateSize,
                swipeThreshold = swipeThreshold,
                minRatioBound = minRatioBound,
                animationSpec = animationSpec
            ) { direction -> itemProvider.onSwiped(direction) }
                .graphicsLayer(
                    translationX = offsetX.value,
                    rotationZ = rotateDegree * ratio
                )
        } else {
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

            onSizeChanged {
                itemHeight = it.height
            }.graphicsLayer(
                scaleX = scaleX,
                scaleY = scaleY,
                translationY = defY + (itemHeight * (1f - scaleY)) / 2f,
            )
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                width.intValue = max(1, it.width)
            }
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        val visible = if (isEndless) {
            visibleItemCount
        } else {
            min(visibleItemCount, itemProvider.itemCount - 1 - frontItemIndex.value)
        }
        for (index in visible downTo 0) {
            Card(Modifier.applyTransformation(index)) {
                val itemIndex = if (isEndless) {
                    (frontItemIndex.value + index) % itemProvider.itemCount
                } else {
                    frontItemIndex.value + index
                }
                itemProvider.Item(itemIndex, itemProvider.getKey(itemIndex))
            }
        }
    }
}

private fun Modifier.swipeListener(
    offsetX: Animatable<Float, AnimationVector1D>,
    width: State<Int>,
    rotateDegree: Float,
    translateSize: Dp,
    swipeThreshold: Float,
    minRatioBound: Float,
    animationSpec: AnimationSpec<Float>,
    onSwipe: (Float) -> Unit
) = composed {
    val density = LocalDensity.current.density
    var padding by remember {
        mutableFloatStateOf(0f)
    }

    onSizeChanged {
        val transform = Matrix()
        transform.setRotate(rotateDegree, it.width / 2f, it.height / 2f)
        val points = FloatArray(2) { 0f }
        transform.mapPoints(points)
        padding = points[0].absoluteValue +
                translateSize.value * density
    }.pointerInput(Unit) {
        val decay = splineBasedDecay<Float>(this)
        coroutineScope {
            while (true) {
                offsetX.stop()
                val velocityTracker = VelocityTracker()
                awaitPointerEventScope {
                    val pointerId = awaitFirstDown().id

                    horizontalDrag(pointerId) { change ->
                        launch {
                            offsetX.snapTo(
                                targetValue = offsetX.value +
                                        change.positionChange().x
                            )
                        }
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )
                    }
                }

                val velocity = velocityTracker.calculateVelocity().x
                val targetOffsetX = decay.calculateTargetValue(
                    initialValue = offsetX.value,
                    initialVelocity = velocity
                )

                val bound = max(
                    width.value.toFloat(),
                    size.width.toFloat() + padding
                )
                offsetX.updateBounds(
                    lowerBound = -bound,
                    upperBound = bound
                )
                val ratio = calculateRatio(offsetX.value, width.value, swipeThreshold)
                    .absoluteValue

                launch {
                    if (targetOffsetX.absoluteValue < bound) {
                        if (ratio >= minRatioBound) {
                            offsetX.animateTo(
                                targetValue = if (offsetX.value > 0) {
                                    offsetX.upperBound!!
                                } else {
                                    offsetX.lowerBound!!
                                },
                                animationSpec = animationSpec,
                                initialVelocity = velocity
                            )
                        } else {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = animationSpec,
                                initialVelocity = velocity
                            )
                        }
                    } else {
                        offsetX.animateDecay(
                            initialVelocity = velocity,
                            animationSpec = decay
                        )
                    }
                }.invokeOnCompletion { error ->
                    launch {
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
        }
    }
}

private fun calculateRatio(
    offsetX: Float,
    width: Int,
    swipeThreshold: Float
): Float {
    return min(
        MAX_RATIO, max(
            -MAX_RATIO,
            offsetX / (width * swipeThreshold)
        )
    )
}

private const val MAX_RATIO = 1f

@Preview(showBackground = true)
@Composable
private fun PreviewLazySwipeCards() {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Cyan,
    )

    LazySwipeCards(Modifier.fillMaxSize()) {
        items(colors) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = it),
            )
        }
    }
}