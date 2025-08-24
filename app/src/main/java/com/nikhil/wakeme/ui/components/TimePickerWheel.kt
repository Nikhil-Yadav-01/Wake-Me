package com.nikhil.wakeme.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

// --- Enhanced TimePickerWheel ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimePickerWheel(
    hour: Int, minute: Int, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Companion.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            NumberWheel(
                range = 0..23,
                value = hour,
                onValueChange = onHourChange,
                modifier = Modifier.Companion.width(100.dp)
            )
            Text(
                ":",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.Companion.padding(horizontal = 8.dp)
            )
            NumberWheel(
                range = 0..59,
                value = minute,
                onValueChange = onMinuteChange,
                modifier = Modifier.Companion.width(100.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    range: IntRange, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(range, value) {
        if (listState.firstVisibleItemIndex != value) listState.scrollToItem(value)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.filter { !it }.collect {
            val centerItemInfo =
                listState.layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset) }
            if (centerItemInfo != null) {
                val newValue = centerItemInfo.index
                if (newValue != value) onValueChange(newValue)
            }
        }
    }

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 48.dp)
        ) {
            items(range.last - range.first + 1) { index ->
                val number = range.start + index
                val isSelected = number == value
                val textAlpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "alpha")
                val textSize by animateDpAsState(if (isSelected) 32.dp else 20.dp, label = "size")

                Text(
                    text = "%02d".format(number),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = with(LocalDensity.current) { textSize.toSp() }),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(40.dp)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                )
        )
    }
}