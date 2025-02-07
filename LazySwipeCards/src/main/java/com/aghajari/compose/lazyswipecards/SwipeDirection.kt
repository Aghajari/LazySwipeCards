package com.aghajari.compose.lazyswipecards

enum class SwipeDirection {
    LEFT,
    RIGHT
}

internal fun getSwipeDirection(offset: Float): SwipeDirection {
    return if (offset < 0) {
        SwipeDirection.LEFT
    } else {
        SwipeDirection.RIGHT
    }
}