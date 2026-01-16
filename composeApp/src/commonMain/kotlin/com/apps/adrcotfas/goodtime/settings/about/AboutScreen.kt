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
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.ui.IconListItem
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.BookOpen
import compose.icons.evaicons.outline.Github
import compose.icons.evaicons.outline.Globe
import compose.icons.evaicons.outline.PaperPlane
import compose.icons.evaicons.outline.Star
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.about_acknowledgements
import goodtime_productivity.composeapp.generated.resources.about_and_feedback_title
import goodtime_productivity.composeapp.generated.resources.about_app_intro
import goodtime_productivity.composeapp.generated.resources.about_feedback
import goodtime_productivity.composeapp.generated.resources.about_open_source_licenses
import goodtime_productivity.composeapp.generated.resources.about_rate_this_app
import goodtime_productivity.composeapp.generated.resources.about_source_code
import goodtime_productivity.composeapp.generated.resources.about_translate_this_app
import goodtime_productivity.composeapp.generated.resources.tutorial_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    mainViewModel: MainViewModel,
    aboutViewModel: AboutViewModel = koinViewModel<AboutViewModel>(),
    onNavigateToLicenses: () -> Unit,
    onNavigateToAcknowledgements: () -> Unit,
    isLicensesSelected: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.about_and_feedback_title),
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
                title = stringResource(Res.string.about_source_code),
                icon = { Icon(EvaIcons.Outline.Github, contentDescription = "GitHub") },
                onClick = {
                    aboutViewModel.openSourceCode()
                },
            )
            IconListItem(
                title = stringResource(Res.string.about_open_source_licenses),
                icon = {
                    Icon(
                        EvaIcons.Outline.BookOpen,
                        contentDescription = stringResource(Res.string.about_open_source_licenses),
                    )
                },
                onClick = {
                    onNavigateToLicenses()
                },
                isSelected = isLicensesSelected,
            )
            IconListItem(
                title = stringResource(Res.string.about_acknowledgements),
                icon = {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(Res.string.about_acknowledgements),
                    )
                },
                onClick = {
                    onNavigateToAcknowledgements()
                },
            )
            SubtleHorizontalDivider()
            IconListItem(
                title = stringResource(Res.string.about_app_intro),
                icon = {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = stringResource(Res.string.about_app_intro),
                    )
                },
                onClick = {
                    mainViewModel.setShowOnboarding(true)
                },
            )
            IconListItem(
                title = stringResource(Res.string.tutorial_title),
                icon = {
                    Icon(
                        Icons.Outlined.Preview,
                        contentDescription = stringResource(Res.string.tutorial_title),
                    )
                },
                onClick = {
                    mainViewModel.setShowTutorial(true)
                    onNavigateToMain()
                },
            )
            SubtleHorizontalDivider()
            IconListItem(
                title = stringResource(Res.string.about_feedback),
                icon = {
                    Icon(
                        EvaIcons.Outline.PaperPlane,
                        contentDescription = stringResource(Res.string.about_feedback),
                    )
                },
                onClick = {
                    aboutViewModel.sendFeedback()
                },
            )
            IconListItem(
                title = stringResource(Res.string.about_translate_this_app),
                icon = {
                    Icon(
                        EvaIcons.Outline.Globe,
                        contentDescription = stringResource(Res.string.about_translate_this_app),
                    )
                },
                onClick = {
                    aboutViewModel.openTranslationPage()
                },
            )
            IconListItem(
                title = stringResource(Res.string.about_rate_this_app),
                icon = {
                    Icon(
                        EvaIcons.Outline.Star,
                        contentDescription = stringResource(Res.string.about_rate_this_app),
                    )
                },
                onClick = {
                    aboutViewModel.openGooglePlay()
                },
            )
        }
    }
}
