package com.aghajari.compose.lazyswipecards.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aghajari.compose.lazyswipecards.LazySwipeCards
import com.aghajari.compose.lazyswipecards.SwipeDirection
import com.aghajari.compose.lazyswipecards.rememberLazySwipeCardsState
import com.aghajari.compose.lazyswipecards.test.theme.ApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyLazySwipeCards()
                }
            }
        }
    }
}

@Composable
fun MyLazySwipeCards(modifier: Modifier = Modifier) {
    val state = rememberLazySwipeCardsState()
    val scope = rememberCoroutineScope()
    val swipedDirection = remember { mutableStateOf(SwipeDirection.LEFT) }

    Box(contentAlignment = Alignment.TopCenter) {
        Row {
            Button(onClick = {
                scope.launch {
                    state.animateSwipe(
                        direction = swipedDirection.value,
                    )
                }
            }) { Text(text = "Next") }
            
            Button(onClick = {
                scope.launch {
                    state.animateBackSwipe(
                        direction = swipedDirection.value,
                    )
                }
            }) { Text(text = "Back") }
        }

        LazySwipeCards(
            modifier = modifier.fillMaxSize(),
            cardShape = RoundedCornerShape(16.dp),
            cardShadowElevation = 4.dp,
            visibleItemCount = 3,
            isEndless = false,
            state = state,
        ) {
            onSwiped { model, dir ->
                swipedDirection.value = dir
                println("OnSwiped: ${(model as Model).text} to ${dir.name}")
            }
            onSwiping { x, ratio, dir ->
                println("$x : $ratio : ${dir.name}")
            }

            items(models) {
                Box {
                    AsyncImage(
                        model = it.image,
                        contentDescription = it.text,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = it.text,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 28.dp,
                            )
                            .align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLazySwipeCards() {
    ApplicationTheme {
        MyLazySwipeCards()
    }
}