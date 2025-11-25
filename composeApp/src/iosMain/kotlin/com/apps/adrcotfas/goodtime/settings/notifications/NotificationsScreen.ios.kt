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
package com.apps.adrcotfas.goodtime.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.LockedCheckboxListItem
import com.apps.adrcotfas.goodtime.ui.TopBar
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_break_complete_sound
import goodtime_productivity.composeapp.generated.resources.settings_default_notification_sound
import goodtime_productivity.composeapp.generated.resources.settings_focus_complete_sound
import goodtime_productivity.composeapp.generated.resources.settings_notifications_title
import goodtime_productivity.composeapp.generated.resources.settings_screen_flash_title
import goodtime_productivity.composeapp.generated.resources.settings_silent
import goodtime_productivity.composeapp.generated.resources.settings_torch_desc
import goodtime_productivity.composeapp.generated.resources.settings_torch_title
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun NotificationsScreen(onNavigateBack: () -> Boolean) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    val workRingTone = toSoundData(settings.workFinishedSound)
    val breakRingTone = toSoundData(settings.breakFinishedSound)
    val candidateRingTone = uiState.notificationSoundCandidate?.let { toSoundData(it) }

    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_notifications_title),
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
            BetterListItem(
                title = stringResource(Res.string.settings_focus_complete_sound),
                subtitle = notificationSoundName(workRingTone),
                onClick = { viewModel.setShowSelectWorkSoundPicker(true) },
            )

            BetterListItem(
                title = stringResource(Res.string.settings_break_complete_sound),
                subtitle = notificationSoundName(breakRingTone),
                onClick = { viewModel.setShowSelectBreakSoundPicker(true) },
            )

            LockedCheckboxListItem(
                title = stringResource(Res.string.settings_torch_title),
                enabled = settings.isPro,
                subtitle = stringResource(Res.string.settings_torch_desc),
                checked = settings.enableTorch,
            ) {
                viewModel.setEnableTorch(it)
            }

            LockedCheckboxListItem(
                title = stringResource(Res.string.settings_screen_flash_title),
                enabled = settings.isPro,
                subtitle = stringResource(Res.string.settings_torch_desc),
                checked = settings.flashScreen,
            ) {
                viewModel.setEnableFlashScreen(it)
            }
        }

        if (uiState.showSelectWorkSoundPicker) {
            NotificationSoundPickerDialog(
                title = stringResource(Res.string.settings_focus_complete_sound),
                selectedItem = candidateRingTone ?: workRingTone,
                onSelected = {
                    viewModel.setNotificationSoundCandidate(Json.encodeToString(it))
                },
                onSave = { viewModel.setWorkFinishedSound(Json.encodeToString(it)) },
                onDismiss = { viewModel.setShowSelectWorkSoundPicker(false) },
            )
        }
        if (uiState.showSelectBreakSoundPicker) {
            NotificationSoundPickerDialog(
                title = stringResource(Res.string.settings_break_complete_sound),
                selectedItem = candidateRingTone ?: breakRingTone,
                onSelected = {
                    viewModel.setNotificationSoundCandidate(Json.encodeToString(it))
                },
                onSave = { viewModel.setBreakFinishedSound(Json.encodeToString(it)) },
                onDismiss = { viewModel.setShowSelectBreakSoundPicker(false) },
            )
        }
    }
}

@Composable
private fun notificationSoundName(it: SoundData) =
    if (it.isSilent) {
        stringResource(Res.string.settings_silent)
    } else {
        it.name.ifEmpty {
            stringResource(Res.string.settings_default_notification_sound)
        }
    }
