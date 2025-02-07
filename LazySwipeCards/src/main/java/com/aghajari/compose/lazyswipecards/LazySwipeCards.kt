package com.aghajari.compose.lazyswipecards

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

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
        scaleY = 0.1f,
    ),
    translateSize: Dp = 24.dp,
    rotateDegree: Float = 14f,
    visibleItemCount: Int = 3,
    contentPadding: PaddingValues = PaddingValues(
        vertical = translateSize * visibleItemCount,
        horizontal = translateSize,
    ),
    swipeThreshold: Float = 0.5f,
    minRatioBound: Float = MAX_RATIO,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    isEndless: Boolean = false,
    state: LazySwipeCardsState = rememberLazySwipeCardsState(),
    content: LazySwipeCardsScope.() -> Unit,
) {
    require(swipeThreshold > 0f && swipeThreshold <= 1f) {
        "swipeThreshold must be > 0 and <= 1"
    }
    require(visibleItemCount >= 1) {
        "visibleItemCount must be >= 1"
    }

    state.updateRatio(swipeThreshold = swipeThreshold)
    val itemProvider = rememberLazySwipeCardsItemProvider(
        content = content,
        state = state,
        isEndless = isEndless,
    )
    LaunchedEffect(state.ratio) {
        itemProvider.onSwiping(state.offsetX, state.ratio)
    }

    Box(
        modifier = modifier
            .onSizeChanged(state::onSizeChanged)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        state.bind(
            isEndless = isEndless,
            visibleItemCount = visibleItemCount,
            itemProvider = itemProvider,
        ) { cardIndex, content ->
            Surface(
                modifier = Modifier
                    .swipe(
                        cardIndex = cardIndex,
                        state = state,
                        rotateDegree = rotateDegree,
                        minRatioBound = minRatioBound,
                        swipeThreshold = swipeThreshold,
                        scaleFactor = scaleFactor,
                        translateSize = translateSize,
                        visibleItemCount = visibleItemCount,
                        animationSpec = animationSpec,
                        onSwipe = itemProvider::onSwiped,
                    ) then cardModifier,
                shape = cardShape,
                color = cardColor,
                contentColor = cardContentColor,
                tonalElevation = cardTonalElevation,
                shadowElevation = cardShadowElevation,
                border = cardBorder,
                content = content,
            )
        }
    }
}

internal fun calculateRatio(
    offsetX: Float,
    width: Int,
    swipeThreshold: Float,
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