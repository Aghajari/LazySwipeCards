package com.aghajari.compose.lazyswipecards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberLazySwipeCardsItemProvider(
    content: LazySwipeCardsScope.() -> Unit,
    isEndless: Boolean,
    state: LazySwipeCardsState,
): LazySwipeCardsItemProvider {
    val latestContent = rememberUpdatedState(content)

    return remember(isEndless, state) {
        val listScope = LazySwipeCardsScopeImpl()
            .apply(latestContent.value)
        val itemProviderState = derivedStateOf {
            LazySwipeCardsItemProviderImpl(
                intervals = listScope.intervals,
                onSwiped = listScope.onSwiped,
                onSwiping = listScope.onSwiping,
                isEndless = isEndless,
                state = state,
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
    val isEndless: Boolean,
    val state: LazySwipeCardsState,
) : LazySwipeCardsItemProvider,
    LazyLayoutItemProvider by LazyLayoutItemProvider(
        intervals = intervals,
        itemContent = { interval, index -> interval.item.invoke(index) },
    ) {

    override fun onSwiping(offset: Float, ratio: Float) {
        onSwiping?.invoke(offset, ratio, getSwipeDirection(offset))
    }

    override fun onSwiped(targetOffset: Float): Boolean {
        val selectedIndex = state.selectedItemIndex
        val realFrontIndex = if (isEndless) {
            selectedIndex % itemCount
        } else {
            if (selectedIndex >= itemCount) {
                return false
            }
            selectedIndex
        }
        withLocalIntervalIndex(realFrontIndex, intervals) { localIndex, content ->
            val item = content.list[localIndex]
            onSwiped?.invoke(item, getSwipeDirection(targetOffset))
            state.onSwiped()
        }
        return true
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