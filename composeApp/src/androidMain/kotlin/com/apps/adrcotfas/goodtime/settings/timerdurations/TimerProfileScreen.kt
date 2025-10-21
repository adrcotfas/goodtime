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
package com.apps.adrcotfas.goodtime.settings.timerdurations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.BreakBudgetInfoDialog
import com.apps.adrcotfas.goodtime.common.TimerProfileSettings
import com.apps.adrcotfas.goodtime.settings.TimerProfileViewModel
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Lock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_save
import goodtime_productivity.composeapp.generated.resources.settings_create_profile
import goodtime_productivity.composeapp.generated.resources.settings_timer_durations_title
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerProfileScreen(
    viewModel: TimerProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberScrollState()

    if (uiState.isLoading) return

    val label = uiState.tmpLabel
    val isDifferentFromDefault = uiState.defaultLabel != label

    var showBreakBudgetInfoDialog by remember { mutableStateOf(false) }

    var showAddTimerProfileDialog by remember { mutableStateOf(false) }
    var showTimerProfilesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_timer_durations_title),
                onNavigateBack = onNavigateBack,
                icon = Icons.AutoMirrored.Default.ArrowBack,
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
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding(),
        ) {
            Column {
                TimerProfileSettings(
                    timerProfile = label.timerProfile,
                    timerProfiles = uiState.timerProfiles,
                    onTimerProfileChange = { updated ->
                        viewModel.updateTmpLabel(label.copy(timerProfile = updated))
                    },
                    onTimerProfileSelect = { selected ->
                        viewModel.updateTmpLabel(
                            label.copy(timerProfile = selected),
                            resetProfile = false,
                        )
                    },
                    onEditProfiles =
                        if (uiState.isPro) {
                            { showTimerProfilesSheet = true }
                        } else {
                            null
                        },
                    onBreakBudgetInfo = { showBreakBudgetInfoDialog = true },
                )

                if (isDifferentFromDefault || label.timerProfile.name == null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val createProfileButtonTargetWeight =
                            if (label.timerProfile.name == null) 1f else 0f
                        val createProfileButtonAnimatedWeight by animateFloatAsState(
                            targetValue = createProfileButtonTargetWeight,
                            label = "createProfileButtonWeight",
                        )

                        val saveButtonTargetWeight =
                            if (isDifferentFromDefault) 1f else 0f
                        val saveButtonAnimatedWeight by animateFloatAsState(
                            targetValue = saveButtonTargetWeight,
                            label = "createProfileButtonWeight",
                        )

                        if (createProfileButtonAnimatedWeight > 0.5f) {
                            FilledTonalButton(
                                enabled = uiState.isPro,
                                modifier = Modifier.weight(createProfileButtonAnimatedWeight),
                                colors =
                                    ButtonDefaults.filledTonalButtonColors().copy(
                                        containerColor =
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.18f,
                                            ),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                onClick = { showAddTimerProfileDialog = true },
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!uiState.isPro) {
                                        Icon(
                                            EvaIcons.Outline.Lock,
                                            contentDescription = null,
                                        )
                                    }
                                    Text(
                                        text = stringResource(Res.string.settings_create_profile),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        if (saveButtonAnimatedWeight > 0.5f) {
                            Button(
                                modifier = Modifier.weight(saveButtonAnimatedWeight),
                                enabled = isDifferentFromDefault,
                                onClick = { viewModel.saveChanges(label = uiState.tmpLabel) },
                            ) {
                                Text(stringResource(Res.string.main_save))
                            }
                        }
                    }
                }
            }
        }
        if (showBreakBudgetInfoDialog) {
            BreakBudgetInfoDialog(
                onDismiss = { showBreakBudgetInfoDialog = false },
            )
        }
        if (showAddTimerProfileDialog) {
            CreateTimerProfileDialog(
                profileNames = uiState.timerProfiles.mapNotNull { it.name },
                onConfirm = {
                    val timerProfile = label.timerProfile.copy(name = it)
                    viewModel.createTimerProfile(timerProfile)
                    val newLabel = label.copy(timerProfile = timerProfile)
                    viewModel.updateTmpLabel(
                        newLabel = newLabel,
                        resetProfile = false,
                    )
                    showAddTimerProfileDialog = false
                },
                onDismiss = {
                    showAddTimerProfileDialog = false
                },
            )
        }
        if (showTimerProfilesSheet && uiState.timerProfiles.isNotEmpty()) {
            TimerProfileBottomSheet(
                profiles = uiState.timerProfiles,
                sheetState = sheetState,
                onDismiss = { showTimerProfilesSheet = false },
                onDelete = {
                    viewModel.deleteTimerProfile(it)
                    if (uiState.timerProfiles.size <= 1) {
                        showTimerProfilesSheet = false
                    }
                },
            )
        }
    }
}
