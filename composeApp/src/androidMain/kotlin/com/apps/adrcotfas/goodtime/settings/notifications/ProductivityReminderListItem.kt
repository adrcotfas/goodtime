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

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.bl.AndroidTimeUtils.getLocalizedDayNamesForStats
import com.apps.adrcotfas.goodtime.common.entriesStartingWithThis
import com.apps.adrcotfas.goodtime.common.secondsOfDayToTimerFormat
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import com.apps.adrcotfas.goodtime.ui.common.BetterListItem
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_days_of_the_week
import goodtime_productivity.composeapp.generated.resources.settings_reminder_time
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Composable
fun ProductivityReminderListItem(
    firstDayOfWeek: DayOfWeek,
    selectedDays: Set<DayOfWeek>,
    reminderSecondOfDay: Int,
    onSelectDay: (DayOfWeek) -> Unit,
    onReminderTimeClick: () -> Unit,
) {
    val context = LocalContext.current
    val locale = androidx.compose.ui.text.intl.Locale.current
    val javaLocale = remember(locale) { Locale.forLanguageTag(locale.toLanguageTag()) }

    val iconButtonColors = IconButtonDefaults.filledIconButtonColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        BetterListItem(
            title = stringResource(Res.string.settings_days_of_the_week),
            supporting = {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val daysInOrder = firstDayOfWeek.entriesStartingWithThis()
                    for (day in daysInOrder) {
                        FilledIconButton(
                            colors =
                                if (selectedDays.contains(day)) {
                                    iconButtonColors.copy(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    iconButtonColors.copy(
                                        containerColor = iconButtonColors.disabledContainerColor,
                                        contentColor = iconButtonColors.disabledContentColor,
                                    )
                                },
                            onClick = { onSelectDay(day) },
                        ) {
                            Text(
                                text = getLocalizedDayNamesForStats(javaLocale)[day.ordinal],
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
        )

        BetterListItem(
            title = stringResource(Res.string.settings_reminder_time),
            trailing =
                secondsOfDayToTimerFormat(
                    reminderSecondOfDay,
                    DateFormat.is24HourFormat(context),
                ),
            enabled = selectedDays.isNotEmpty(),
            onClick = { onReminderTimeClick() },
        )
    }
}

@Preview
@Composable
fun ProductivityReminderSectionPreview() {
    ApplicationTheme {
        ProductivityReminderListItem(
            firstDayOfWeek = DayOfWeek.MONDAY,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            reminderSecondOfDay = 10 * 60 * 60,
            onSelectDay = {},
            onReminderTimeClick = {},
        )
    }
}
