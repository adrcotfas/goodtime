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
package com.apps.adrcotfas.goodtime.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.ui.common.IconListItem
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.BookOpen
import compose.icons.evaicons.outline.Github
import compose.icons.evaicons.outline.Globe
import compose.icons.evaicons.outline.PaperPlane
import compose.icons.evaicons.outline.Star
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateToLicenses: () -> Unit,
    onNavigateToAcknowledgements: () -> Unit,
    isLicensesSelected: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val context = LocalContext.current

    val mainViewModel = koinInject<MainViewModel>()
    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.about_and_feedback_title),
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(listState)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            IconListItem(
                title = stringResource(R.string.about_source_code),
                icon = { Icon(EvaIcons.Outline.Github, contentDescription = "GitHub") },
                onClick = {
                    openUrl(context, REPO_URL)
                },
            )
            IconListItem(
                title = stringResource(R.string.about_open_source_licenses),
                icon = {
                    Icon(
                        EvaIcons.Outline.BookOpen,
                        contentDescription = stringResource(R.string.about_open_source_licenses),
                    )
                },
                onClick = {
                    onNavigateToLicenses()
                },
                isSelected = isLicensesSelected,
            )
            IconListItem(
                title = stringResource(R.string.about_acknowledgements),
                icon = {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.about_acknowledgements),
                    )
                },
                onClick = {
                    onNavigateToAcknowledgements()
                },
            )
            SubtleHorizontalDivider()
            IconListItem(
                title = stringResource(R.string.about_app_intro),
                icon = {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = stringResource(R.string.about_app_intro),
                    )
                },
                onClick = {
                    mainViewModel.setShowOnboarding(true)
                },
            )
            IconListItem(
                title = stringResource(R.string.tutorial_title),
                icon = {
                    Icon(
                        Icons.Outlined.Preview,
                        contentDescription = stringResource(R.string.tutorial_title),
                    )
                },
                onClick = {
                    mainViewModel.setShowTutorial(true)
                    onNavigateToMain()
                },
            )
            SubtleHorizontalDivider()
            IconListItem(
                title = stringResource(R.string.about_feedback),
                icon = {
                    Icon(
                        EvaIcons.Outline.PaperPlane,
                        contentDescription = stringResource(R.string.about_feedback),
                    )
                },
                onClick = { sendFeedback(context) },
            )
            IconListItem(
                title = stringResource(R.string.about_translate_this_app),
                icon = {
                    Icon(
                        EvaIcons.Outline.Globe,
                        contentDescription = stringResource(R.string.about_translate_this_app),
                    )
                },
                onClick = {
                    openUrl(context, TRANSLATE_URL)
                },
            )
            IconListItem(
                title = stringResource(R.string.about_rate_this_app),
                icon = {
                    Icon(
                        EvaIcons.Outline.Star,
                        contentDescription = stringResource(R.string.about_rate_this_app),
                    )
                },
                onClick = {
                    openUrl(context, GOOGLE_PLAY_URL)
                },
            )
        }
    }
}

const val GOOGLE_PLAY_URL =
    "https://play.google.com/store/apps/details?id=com.apps.adrcotfas.goodtime"
const val REPO_URL = "https://github.com/adrcotfas/goodtime"
const val TRANSLATE_URL = "https://crowdin.com/project/goodtime"

@Preview
@Composable
fun AboutScreenPreview() {
    AboutScreen(
        onNavigateToLicenses = {},
        onNavigateToAcknowledgements = {},
        onNavigateBack = { },
        onNavigateToMain = {},
    )
}
