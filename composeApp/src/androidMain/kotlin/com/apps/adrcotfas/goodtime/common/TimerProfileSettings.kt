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
package com.apps.adrcotfas.goodtime.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuBox
import com.apps.adrcotfas.goodtime.ui.common.EditableNumberListItem
import com.apps.adrcotfas.goodtime.ui.common.InfoDialog
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.TimerTypeRow
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Edit
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.labels_break_budget_desc1
import goodtime_productivity.composeapp.generated.resources.labels_break_budget_desc2
import goodtime_productivity.composeapp.generated.resources.labels_break_budget_info
import goodtime_productivity.composeapp.generated.resources.labels_break_time
import goodtime_productivity.composeapp.generated.resources.labels_custom
import goodtime_productivity.composeapp.generated.resources.labels_enable_break_budget
import goodtime_productivity.composeapp.generated.resources.labels_focus_break_ratio
import goodtime_productivity.composeapp.generated.resources.labels_focus_time
import goodtime_productivity.composeapp.generated.resources.labels_long_break
import goodtime_productivity.composeapp.generated.resources.labels_sessions_before_long_break
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable UI for editing a [TimerProfile].
 *
 * @param timerProfile the current profile being edited
 * @param timerProfiles list of all saved profiles (used for the dropdown)
 * @param onTimerProfileChange invoked with an updated profile after any field change
 * @param onTimerProfileSelect invoked when the user selects a profile from the dropdown
 */
@Composable
fun TimerProfileSettings(
    timerProfile: TimerProfile,
    timerProfiles: List<TimerProfile>,
    onTimerProfileChange: (TimerProfile) -> Unit,
    onTimerProfileSelect: (TimerProfile) -> Unit,
    onEditProfiles: (() -> Unit)? = null,
    onBreakBudgetInfo: () -> Unit,
) {
    // ---- Profile selector -------------------------------------------------
    AnimatedVisibility(timerProfiles.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DropdownMenuBox(
                    contentModifier = Modifier.fillMaxWidth(),
                    colored = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    value = timerProfile.name ?: stringResource(Res.string.labels_custom),
                    options = timerProfiles.mapNotNull { it.name },
                    onDismissRequest = {},
                    onDropdownMenuItemSelected = {
                        val selected = timerProfiles[it]
                        onTimerProfileSelect(selected)
                    },
                )
            }
            onEditProfiles?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = EvaIcons.Outline.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    // ---- Countdown / Break‑budget switch ---------------------------------
    TimerTypeRow(
        isCountDown = timerProfile.isCountdown,
        onCountDownEnabled = {
            onTimerProfileChange(timerProfile.copy(isCountdown = it))
        },
    )

    // ---- Countdown mode ---------------------------------------------------
    AnimatedVisibility(timerProfile.isCountdown, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            EditableNumberListItem(
                title = stringResource(Res.string.labels_focus_time),
                value = timerProfile.workDuration,
                onValueChange = {
                    onTimerProfileChange(timerProfile.copy(workDuration = it))
                },
            )
            EditableNumberListItem(
                title = stringResource(Res.string.labels_break_time),
                value = timerProfile.breakDuration,
                onValueChange = {
                    onTimerProfileChange(timerProfile.copy(breakDuration = it))
                },
                enableSwitch = true,
                switchValue = timerProfile.isBreakEnabled,
                onSwitchChange = { enabled ->
                    val longEnabled = if (!enabled) false else timerProfile.isLongBreakEnabled
                    onTimerProfileChange(
                        timerProfile.copy(
                            isBreakEnabled = enabled,
                            isLongBreakEnabled = longEnabled,
                        ),
                    )
                },
            )
            EditableNumberListItem(
                title = stringResource(Res.string.labels_long_break),
                value = timerProfile.longBreakDuration,
                onValueChange = {
                    onTimerProfileChange(timerProfile.copy(longBreakDuration = it))
                },
                enabled = timerProfile.isBreakEnabled,
                enableSwitch = true,
                switchValue = timerProfile.isLongBreakEnabled,
                onSwitchChange = { enabled ->
                    onTimerProfileChange(timerProfile.copy(isLongBreakEnabled = enabled))
                },
            )
            EditableNumberListItem(
                title = stringResource(Res.string.labels_sessions_before_long_break),
                value = timerProfile.sessionsBeforeLongBreak,
                minValue = 2,
                maxValue = 8,
                enabled = timerProfile.isBreakEnabled && timerProfile.isLongBreakEnabled,
                onValueChange = {
                    onTimerProfileChange(timerProfile.copy(sessionsBeforeLongBreak = it))
                },
            )
        }
    }

    // ---- Break‑budget mode (non‑countdown) -------------------------------
    AnimatedVisibility(!timerProfile.isCountdown, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            val toggleBreak = {
                onTimerProfileChange(
                    timerProfile.copy(isBreakEnabled = !timerProfile.isBreakEnabled),
                )
            }
            ListItem(
                modifier =
                    Modifier.toggleable(
                        value = timerProfile.isBreakEnabled,
                        onValueChange = { toggleBreak() },
                    ),
                headlineContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    ) {
                        Text(stringResource(Res.string.labels_enable_break_budget))
                        IconButton(onClick = onBreakBudgetInfo) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(Res.string.labels_break_budget_info),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                trailingContent = {
                    Checkbox(checked = timerProfile.isBreakEnabled, onCheckedChange = null)
                },
            )
            SliderListItem(
                title = stringResource(Res.string.labels_focus_break_ratio),
                min = 2,
                max = 6,
                enabled = timerProfile.isBreakEnabled,
                value = timerProfile.workBreakRatio,
                showValue = true,
                onValueChange = {
                    onTimerProfileChange(timerProfile.copy(workBreakRatio = it))
                },
            )
        }
    }
}

/**
 * A small wrapper around [InfoDialog] that shows the break‑budget information.
 *
 * @param onDismiss Called when the dialog is dismissed (e.g. user taps outside or presses OK).
 */
@Composable
fun BreakBudgetInfoDialog(onDismiss: () -> Unit) {
    InfoDialog(
        title = stringResource(Res.string.labels_break_budget_info),
        subtitle =
            "${stringResource(Res.string.labels_break_budget_desc1)}\n" +
                stringResource(Res.string.labels_break_budget_desc2),
    ) {
        onDismiss()
    }
}
