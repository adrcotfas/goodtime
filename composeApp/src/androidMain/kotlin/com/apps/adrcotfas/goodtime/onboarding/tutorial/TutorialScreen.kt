/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.onboarding.tutorial

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.onboarding.PageIndicator
import com.apps.adrcotfas.goodtime.onboarding.darkGray
import com.apps.adrcotfas.goodtime.onboarding.lightGray
import kotlinx.coroutines.launch

data class TutorialScreen(
    @StringRes val title: Int,
    @RawRes val animation: Int,
) {
    companion object {
        val pages =
            listOf(
                TutorialScreen(title = R.string.tutorial_tap, animation = R.raw.tap),
                TutorialScreen(title = R.string.tutorial_swipe_right, animation = R.raw.swipe_right),
                TutorialScreen(title = R.string.tutorial_swipe_up, animation = R.raw.swipe_up),
                TutorialScreen(title = R.string.tutorial_swipe_down, animation = R.raw.swipe_down),
            )
    }
}

@Composable
fun TutorialScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = TutorialScreen.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })

    val black = Color.Black

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(black.copy(alpha = 0.68f))
                .then(modifier),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterVertically),
        ) {
            HorizontalPager(state = pagerState) { page ->
                TutorialPage(
                    title = stringResource(pages[page].title),
                    animation = pages[page].animation,
                )
            }
        }

        val isLastPage = pagerState.currentPage == pages.lastIndex

        FloatingActionButton(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
                    .size(72.dp),
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            shape = CircleShape,
            onClick = {
                if (isLastPage) {
                    onClose()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
        ) {
            Crossfade(isLastPage, label = "onboarding button") {
                if (it) {
                    Icon(Icons.Filled.Check, contentDescription = "Finish")
                } else {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "Finish",
                    )
                }
            }
        }

        PageIndicator(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            color = darkGray,
            selectionColor = lightGray,
        )

        IconButton(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp), onClick = {
            onClose()
        }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

@Composable
fun TutorialPage(
    title: String,
    animation: Int,
) {
    val white = Color.White

    val isPortrait = LocalConfiguration.current.isPortrait
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animation))
    val lottieModifier = if (isPortrait) Modifier.fillMaxSize(0.5f) else Modifier
    val lottieAnimation: @Composable () -> Unit = {
        LottieAnimation(
            modifier = lottieModifier,
            composition = composition,
            contentScale = ContentScale.FillHeight,
            iterations = LottieConstants.IterateForever,
        )
    }

    if (isPortrait) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            lottieAnimation()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Use gestures to control the timer",
                modifier = Modifier.fillMaxWidth(0.9f),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = white,
                    ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(0.9f),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = white,
                    ),
            )
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            lottieAnimation()
            Column {
                Text(
                    text = "Use gestures to control the timer",
                    modifier = Modifier.fillMaxWidth(0.9f),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = white,
                        ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = white,
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
fun TutorialPagePreview() {
    TutorialPage("Tap the timer to start and pause", R.raw.tap)
}
