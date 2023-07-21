package com.aghajari.compose.lazyswipecards.test

val models = arrayOf(
    Model(
        text = "Mount Bromo, Indonesia",
        image = "file:///android_asset/img1.jpg"
    ),
    Model(
        text = "Le Phare du Petit Minou, France",
        image = "file:///android_asset/img2.jpg"
    ),
    Model(
        text = "Rickenbacker Causeway, United States",
        image = "file:///android_asset/img3.jpg"
    ),
    Model(
        text = "Wanaka, New Zealand",
        image = "file:///android_asset/img4.jpg"
    ),
    Model(
        text = "Black Forest, Germany",
        image = "file:///android_asset/img5.jpg"
    )
)

data class Model(
    val text: String,
    val image: String
)