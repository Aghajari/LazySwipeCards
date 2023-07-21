@file:OptIn(ExperimentalFoundationApi::class)

package com.aghajari.compose.lazyswipecards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

internal typealias OnSwipingFunction = (offset: Float, ratio: Float, direction: SwipeDirection) -> Unit
internal typealias OnSwipedFunction = (item: Any?, direction: SwipeDirection) -> Unit

interface LazySwipeCardsScope {

    fun onSwiping(function: OnSwipingFunction)

    fun onSwiped(function: OnSwipedFunction)

    fun <T> itemsIndexed(
        items: MutableList<T>,
        itemContent: @Composable (index: Int, item: T) -> Unit
    )

    fun <T> items(
        items: MutableList<T>,
        itemContent: @Composable (item: T) -> Unit
    )
}

internal class LazySwipeCardsScopeImpl : LazySwipeCardsScope {

    private val _intervals = MutableIntervalList<LazySwipeCardsIntervalContent>()
    val intervals: IntervalList<LazySwipeCardsIntervalContent> = _intervals

    var onSwiped: OnSwipedFunction? = null
    var onSwiping: OnSwipingFunction? = null

    override fun <T> items(
        items: MutableList<T>,
        itemContent: @Composable (item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            LazySwipeCardsIntervalContent(items) {
                itemContent(items[it])
            }
        )
    }

    override fun <T> itemsIndexed(
        items: MutableList<T>,
        itemContent: @Composable (index: Int, item: T) -> Unit
    ) {
        _intervals.addInterval(
            size = items.size,
            LazySwipeCardsIntervalContent(items) {
                itemContent(it, items[it])
            }
        )
    }

    override fun onSwiped(function: OnSwipedFunction) {
        onSwiped = function
    }

    override fun onSwiping(function: OnSwipingFunction) {
        onSwiping = function
    }
}

internal class LazySwipeCardsIntervalContent(
    val list: MutableList<*>,
    val item: @Composable (index: Int) -> Unit
) : LazyLayoutIntervalContent

