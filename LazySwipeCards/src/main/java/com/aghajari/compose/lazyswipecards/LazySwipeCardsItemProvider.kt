package com.aghajari.compose.lazyswipecards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazySwipeCardsItemProvider(
    content: LazySwipeCardsScope.() -> Unit,
    frontIndex: MutableState<Int>,
    isEndless: Boolean,
): LazySwipeCardsItemProvider {
    val latestContent = rememberUpdatedState(content)

    return remember(isEndless) {
        val listScope = LazySwipeCardsScopeImpl()
            .apply(latestContent.value)
        val itemProviderState = derivedStateOf {
            LazySwipeCardsItemProviderImpl(
                intervals = listScope.intervals,
                onSwiped = listScope.onSwiped,
                onSwiping = listScope.onSwiping,
                frontIndex = frontIndex,
                isEndless = isEndless,
            )
        }
        delegatingLazySwipeCardsItemProvider(itemProviderState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal interface LazySwipeCardsItemProvider : LazyLayoutItemProvider {

    fun onSwiped(targetOffset: Float): Boolean
    fun onSwiping(offset: Float, ratio: Float)
}

@OptIn(ExperimentalFoundationApi::class)
private class LazySwipeCardsItemProviderImpl(
    val intervals: IntervalList<LazySwipeCardsIntervalContent>,
    val onSwiped: OnSwipedFunction?,
    val onSwiping: OnSwipingFunction?,
    val frontIndex: MutableState<Int>,
    val isEndless: Boolean,
) : LazySwipeCardsItemProvider,
    LazyLayoutItemProvider by LazyLayoutItemProvider(
        intervals = intervals,
        itemContent = { interval, index -> interval.item.invoke(index) }
    ) {

    override fun onSwiping(offset: Float, ratio: Float) {
        onSwiping?.invoke(offset, ratio, getDirection(offset))
    }

    override fun onSwiped(targetOffset: Float): Boolean {
        val realFrontIndex = if (isEndless) {
            frontIndex.value % itemCount
        } else {
            if (frontIndex.value >= itemCount) {
                return false
            }
            frontIndex.value
        }
        withLocalIntervalIndex(realFrontIndex, intervals) { localIndex, content ->
            val item = content.list[localIndex]
            onSwiped?.invoke(item, getDirection(targetOffset))
            frontIndex.value++
        }
        return true
    }

    private fun getDirection(offset: Float): SwipeDirection {
        return if (offset < 0) {
            SwipeDirection.LEFT
        } else {
            SwipeDirection.RIGHT
        }
    }
}

private fun delegatingLazySwipeCardsItemProvider(
    delegate: State<LazySwipeCardsItemProvider>
): LazySwipeCardsItemProvider =
    DefaultDelegatingLazySwipeCardsItemProvider(delegate)

private class DefaultDelegatingLazySwipeCardsItemProvider(
    private val delegate: State<LazySwipeCardsItemProvider>
) : LazySwipeCardsItemProvider {
    override val itemCount: Int get() = delegate.value.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        delegate.value.Item(index, key)
    }

    override fun getKey(index: Int): Any = delegate.value.getKey(index)

    override fun getContentType(index: Int): Any? = delegate.value.getContentType(index)

    override fun onSwiped(targetOffset: Float): Boolean = delegate.value.onSwiped(targetOffset)

    override fun onSwiping(offset: Float, ratio: Float) = delegate.value.onSwiping(offset, ratio)
}

@OptIn(ExperimentalFoundationApi::class)
private fun <T : LazyLayoutIntervalContent.Interval> LazyLayoutItemProvider(
    intervals: IntervalList<T>,
    itemContent: @Composable (interval: T, index: Int) -> Unit,
): LazyLayoutItemProvider =
    DefaultLazyLayoutItemsProvider(itemContent, intervals)

@OptIn(ExperimentalFoundationApi::class)
private class DefaultLazyLayoutItemsProvider<IntervalContent : LazyLayoutIntervalContent.Interval>(
    val itemContentProvider: @Composable IntervalContent.(index: Int) -> Unit,
    val intervals: IntervalList<IntervalContent>
) : LazyLayoutItemProvider {
    override val itemCount get() = intervals.size

    @Composable
    override fun Item(index: Int, key: Any) {
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.itemContentProvider(localIndex)
        }
    }

    override fun getKey(index: Int): Any =
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.key?.invoke(localIndex) ?: getDefaultLazyLayoutKey(index)
        }

    override fun getContentType(index: Int): Any? =
        withLocalIntervalIndex(index, intervals) { localIndex, content ->
            content.type.invoke(localIndex)
        }
}

@OptIn(ExperimentalFoundationApi::class)
internal inline fun <IntervalContent : LazyLayoutIntervalContent.Interval, T> withLocalIntervalIndex(
    index: Int,
    intervals: IntervalList<IntervalContent>,
    block: (localIndex: Int, content: IntervalContent) -> T
): T {
    val interval = intervals[index]
    val localIntervalIndex = index - interval.startIndex
    return block(localIntervalIndex, interval.value)
}