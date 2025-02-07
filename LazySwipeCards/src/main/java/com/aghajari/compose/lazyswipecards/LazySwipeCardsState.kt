package com.aghajari.compose.lazyswipecards

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

@Stable
class LazySwipeCardsState(
    initialSelectedItemIndex: Int = 0,
) {

    private val _selectedIndex = mutableIntStateOf(initialSelectedItemIndex)
    val selectedItemIndex: Int get() = _selectedIndex.value

    internal val offsetXAnimatable = Animatable(0f)
    val offsetX: Float get() = offsetXAnimatable.value
    val isAnimationRunning: Boolean get() = offsetXAnimatable.isRunning

    private val _width = mutableIntStateOf(1)
    val viewportWidth: Int get() = _width.intValue

    var ratio: Float = 0f
        private set

    val swipingDirection: SwipeDirection
        get() = getSwipeDirection(offsetX)

    internal fun updateRatio(swipeThreshold: Float) {
        ratio = calculateRatio(offsetX, viewportWidth, swipeThreshold)
    }

    internal fun onSwiped() {
        _selectedIndex.value++
    }

    internal fun onSizeChanged(newSize: IntSize) {
        _width.value = max(1, newSize.width)
    }

    companion object {

        val Saver: Saver<LazySwipeCardsState, *> = Saver(
            save = { it.selectedItemIndex },
            restore = { LazySwipeCardsState(it) },
        )
    }
}

@Composable
internal fun LazySwipeCardsState.bind(
    isEndless: Boolean,
    visibleItemCount: Int,
    itemProvider: LazySwipeCardsItemProvider,
    cardComposable: @Composable (cardIndex: Int, content: @Composable () -> Unit) -> Unit,
) {
    val selectedIndex = selectedItemIndex
    val visible = if (isEndless) {
        visibleItemCount
    } else {
        min(visibleItemCount, itemProvider.itemCount - 1 - selectedIndex)
    }
    for (localIndex in visible downTo 0) {
        cardComposable.invoke(localIndex) {
            val itemIndex = if (isEndless) {
                (selectedIndex + localIndex) % itemProvider.itemCount
            } else {
                selectedIndex + localIndex
            }
            itemProvider.Item(itemIndex, itemProvider.getKey(itemIndex))
        }
    }
}

@Composable
fun rememberLazySwipeCardsState(
    initialSelectedItemIndex: Int = 0,
): LazySwipeCardsState {
    return rememberSaveable(saver = LazySwipeCardsState.Saver) {
        LazySwipeCardsState(
            initialSelectedItemIndex = initialSelectedItemIndex,
        )
    }
}