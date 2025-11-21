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
package com.apps.adrcotfas.goodtime.labels.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.common.BreakBudgetInfoDialog
import com.apps.adrcotfas.goodtime.common.TimerProfileSettings
import com.apps.adrcotfas.goodtime.data.model.Label.Companion.LABEL_NAME_MAX_LENGTH
import com.apps.adrcotfas.goodtime.labels.AddEditLabelViewModel
import com.apps.adrcotfas.goodtime.labels.labelNameIsValid
import com.apps.adrcotfas.goodtime.ui.ColorSelectRow
import com.apps.adrcotfas.goodtime.ui.TopBar
import com.apps.adrcotfas.goodtime.ui.clearFocusOnKeyboardDismiss
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Navigation2
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_add_label_name
import goodtime_productivity.composeapp.generated.resources.labels_default_label_name
import goodtime_productivity.composeapp.generated.resources.labels_follow_default_time_profile
import goodtime_productivity.composeapp.generated.resources.labels_label_name_already_exists
import goodtime_productivity.composeapp.generated.resources.main_save
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLabelScreen(
    viewModel: AddEditLabelViewModel = koinViewModel(),
    labelName: String,
    onNavigateToDefault: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isEditMode = labelName.isNotEmpty()

    val listState = rememberScrollState()

    LaunchedEffect(labelName) {
        val defaultLabelDisplayName = getString(Res.string.labels_default_label_name)
        viewModel.init(labelName, defaultLabelDisplayName)
    }

    if (uiState.isLoading) return

    val label = uiState.tmpLabel
    val labelNameToDisplay = label.name

    val followDefault = label.useDefaultTimeProfile
    var showBreakBudgetInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                onNavigateBack = onNavigateBack,
                icon = Icons.AutoMirrored.Default.ArrowBack,
                title = {
                    LabelNameRow(
                        isAddingNewLabel = !isEditMode,
                        labelName = labelNameToDisplay,
                        onValueChange = {
                            val newLabelName = it
                            viewModel.updateTmpLabel(
                                uiState.tmpLabel.copy(name = newLabelName),
                                resetProfile = false,
                            )
                        },
                        showError = !uiState.labelNameIsValid(),
                    )
                },
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
            ColorSelectRow(
                selectedIndex = label.colorIndex.toInt(),
            ) {
                viewModel.updateTmpLabel(label.copy(colorIndex = it.toLong()), resetProfile = false)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                modifier =
                    Modifier.clickable {
                        viewModel.updateTmpLabel(
                            label.copy(useDefaultTimeProfile = !followDefault),
                            resetProfile = false,
                        )
                    },
                leadingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                onNavigateToDefault()
                            },
                        ) {
                            Icon(
                                imageVector = EvaIcons.Outline.Navigation2,
                                contentDescription = stringResource(Res.string.labels_default_label_name),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        VerticalDivider(modifier = Modifier.height(32.dp))
                    }
                },
                headlineContent = {
                    Text(stringResource(Res.string.labels_follow_default_time_profile))
                },
                trailingContent = {
                    Switch(
                        checked = followDefault,
                        onCheckedChange = {
                            viewModel.updateTmpLabel(
                                label.copy(useDefaultTimeProfile = it),
                                resetProfile = false,
                            )
                        },
                    )
                },
            )
            AnimatedVisibility(!followDefault) {
                Column {
                    TimerProfileSettings(
                        timerProfile = label.timerProfile,
                        timerProfiles = uiState.timerProfiles,
                        onTimerProfileChange = { updated ->
                            viewModel.updateTmpLabel(label.copy(timerProfile = updated))
                        },
                        onTimerProfileSelect = { selected ->
                            viewModel.updateTmpLabel(label.copy(timerProfile = selected), resetProfile = false)
                        },
                        onBreakBudgetInfo = { showBreakBudgetInfoDialog = true },
                    )
                }
            }
            AnimatedVisibility(visible = uiState.labelToEdit != label) {
                Button(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    enabled = uiState.labelNameIsValid(),
                    onClick = {
                        if (isEditMode) {
                            viewModel.updateLabel(labelName, label)
                        } else {
                            viewModel.addLabel(label)
                        }
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(Res.string.main_save))
                }
            }
        }
        if (showBreakBudgetInfoDialog) {
            BreakBudgetInfoDialog(
                onDismiss = { showBreakBudgetInfoDialog = false },
            )
        }
    }
}

@Composable
private fun LabelNameRow(
    isAddingNewLabel: Boolean,
    labelName: String,
    onValueChange: (String) -> Unit,
    showError: Boolean,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier =
            Modifier
                .animateContentSize(),
    ) {
        val internalModifier =
            if (isAddingNewLabel) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            }

        Box {
            BasicTextField(
                modifier =
                    internalModifier
                        .fillMaxWidth()
                        .clearFocusOnKeyboardDismiss(),
                textStyle =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = TextDecoration.Underline,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                value = labelName,
                onValueChange = {
                    if (it.length <= LABEL_NAME_MAX_LENGTH) {
                        onValueChange(it)
                    }
                },
            )
            if (labelName.isEmpty()) {
                Text(
                    text =
                        stringResource(
                            Res.string.labels_add_label_name,
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(showError && labelName.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.labels_label_name_already_exists),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        LaunchedEffect(labelName) {
            if (isAddingNewLabel) focusRequester.requestFocus()
        }
    }
}
