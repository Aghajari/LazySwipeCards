package com.aghajari.compose.lazyswipecards.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aghajari.compose.lazyswipecards.LazySwipeCards
import com.aghajari.compose.lazyswipecards.test.theme.ApplicationTheme

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
    val list = remember { listOf(*models) }

    LazySwipeCards(
        modifier = modifier.fillMaxSize(),
        cardShape = RoundedCornerShape(16.dp),
        cardShadowElevation = 4.dp,
        visibleItemCount = 3,
        isEndless = false,
    ) {
        onSwiped { model, dir ->
            println("OnSwiped: ${(model as Model).text} to ${dir.name}")
        }
        onSwiping { x, ratio, dir ->
            println("$x : $ratio : ${dir.name}")
        }

        items(list) {
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

@Preview(showBackground = true)
@Composable
fun PreviewLazySwipeCards() {
    ApplicationTheme {
        MyLazySwipeCards()
    }
}