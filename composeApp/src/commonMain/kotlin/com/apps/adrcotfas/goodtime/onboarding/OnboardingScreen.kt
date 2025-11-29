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
package com.apps.adrcotfas.goodtime.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.isPortrait
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.intro1
import goodtime_productivity.composeapp.generated.resources.intro1_desc1
import goodtime_productivity.composeapp.generated.resources.intro1_desc2
import goodtime_productivity.composeapp.generated.resources.intro1_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel = koinViewModel()) {
    val pages = OnboardingPage.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    OnboardingPage(
                        title = stringResource(pages[page].title),
                        description1 = stringResource(pages[page].description1),
                        description2 = stringResource(pages[page].description2),
                        image = {
                            Image(
                                painter = painterResource(pages[page].image),
                                contentDescription = stringResource(pages[page].title),
                            )
                        },
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                shape = CircleShape,
                onClick = {
                    if (isLastPage) {
                        viewModel.setShowOnboarding(false)
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                selectionColor = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp), onClick = {
                viewModel.setShowOnboarding(false)
            }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description1: String,
    description2: String,
    image: @Composable () -> Unit,
) {
    val isPortraitOrientation = isPortrait()
    if (isPortraitOrientation) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            image()
            Spacer(modifier = Modifier.padding(16.dp))
            OnboardingPageTextSection(title, description1, description2)
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
            image()
            Spacer(modifier = Modifier.padding(32.dp))
            OnboardingPageTextSection(title, description1, description2)
        }
    }
}

@Composable
fun OnboardingPageTextSection(
    title: String,
    description1: String,
    description2: String,
) {
    Column(
        modifier = Modifier.widthIn(max = 400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = description1,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = description2,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun PageIndicator(
    color: Color,
    selectionColor: Color,
    modifier: Modifier = Modifier,
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        modifier
            .height(18.dp)
            .widthIn(max = 128.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { iteration ->
            Box(
                modifier =
                    Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (currentPage == iteration) selectionColor else color)
                        .size(10.dp),
            )
        }
    }
}

@Preview
@Composable
fun OnboardingPagePreview() {
    OnboardingPage(
        title = stringResource(Res.string.intro1_title),
        description1 = stringResource(Res.string.intro1_desc1),
        description2 = stringResource(Res.string.intro1_desc2),
        image = {
            Image(
                painter = painterResource(Res.drawable.intro1),
                contentDescription = stringResource(Res.string.intro1_title),
            )
        },
    )
}
