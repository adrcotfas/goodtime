package com.apps.adrcotfas.goodtime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
fun SystemDialogScrim(enabled: Boolean) {
    AnimatedVisibility(
        visible = enabled,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing,
            ),
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing,
            ),
        ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clearAndSetSemantics { }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume taps */ },
                    ),
        )
    }
}