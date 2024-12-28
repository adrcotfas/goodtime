package com.apps.adrcotfas.goodtime.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.apps.adrcotfas.goodtime.R

@OptIn(ExperimentalTextApi::class)
fun timerFontWith(resId: Int, weight: Int): FontFamily {
    return FontFamily(
        Font(
            resId = resId,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings((FontVariation.weight(weight)))
        )
    )
}

val timerFontWeights = listOf(100, 200, 300)
val timerFontAzeretMap =
    timerFontWeights.associateWith { weight -> timerFontWith(R.font.azeret_mono, weight) }

val timerTextAzeretStyle = TextStyle(
    fontFamily = timerFontAzeretMap[100],
    fontSize = 60.em
)
