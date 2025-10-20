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
package com.apps.adrcotfas.goodtime.settings

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.AndroidTimeUtils.localizedDayNamesFull
import com.apps.adrcotfas.goodtime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.goodtime.common.areNotificationsEnabled
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.common.secondsOfDayToTimerFormat
import com.apps.adrcotfas.goodtime.data.settings.NotificationPermissionState
import com.apps.adrcotfas.goodtime.data.settings.isDarkTheme
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel.Companion.firstDayOfWeekOptions
import com.apps.adrcotfas.goodtime.settings.notifications.ProductivityReminderListItem
import com.apps.adrcotfas.goodtime.ui.common.BetterListItem
import com.apps.adrcotfas.goodtime.ui.common.CheckboxListItem
import com.apps.adrcotfas.goodtime.ui.common.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.common.DropdownMenuListItem
import com.apps.adrcotfas.goodtime.ui.common.IconListItem
import com.apps.adrcotfas.goodtime.ui.common.LockedCheckboxListItem
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.toSecondOfDay
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Bell
import compose.icons.evaicons.outline.ColorPalette
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserInterface: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDefaultLabel: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    val locale = androidx.compose.ui.text.intl.Locale.current
    val javaLocale = remember(locale) { Locale.forLanguageTag(locale.toLanguageTag()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val notificationManager = koinInject<NotificationArchManager>()
    var isNotificationPolicyAccessGranted by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted()) }
    var isNotificationPolicyAccessRequested by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                isNotificationPolicyAccessGranted =
                    notificationManager.isNotificationPolicyAccessGranted()
                if (isNotificationPolicyAccessRequested && isNotificationPolicyAccessGranted) {
                    viewModel.setDndDuringWork(true)
                }
                if (!isNotificationPolicyAccessGranted) {
                    viewModel.setDndDuringWork(false)
                }
            }

            else -> {
                // do nothing
            }
        }
    }

    val notificationPermissionState = settings.notificationPermissionState

    val listState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.settings_title),
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
                    .animateContentSize(),
        ) {
            AnimatedVisibility(
                notificationPermissionState == NotificationPermissionState.GRANTED ||
                    context.areNotificationsEnabled(),
            ) {
                Column {
                    CompactPreferenceGroupTitle(text = stringResource(R.string.settings_productivity_reminder_title))
                    val reminderSettings = settings.productivityReminderSettings
                    ProductivityReminderListItem(
                        firstDayOfWeek = DayOfWeek(settings.firstDayOfWeek),
                        selectedDays = reminderSettings.days.map { DayOfWeek(it) }.toSet(),
                        reminderSecondOfDay = reminderSettings.secondOfDay,
                        onSelectDay = viewModel::onToggleProductivityReminderDay,
                        onReminderTimeClick = { viewModel.setShowTimePicker(true) },
                    )
                    SubtleHorizontalDivider()
                }
            }
            CompactPreferenceGroupTitle(text = stringResource(R.string.settings_timer_and_sessions))

            IconListItem(
                title = stringResource(R.string.settings_timer_durations_title),
                subtitle = stringResource(R.string.settings_timer_durations_desc),
                icon = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        painter = painterResource(R.drawable.ic_status_goodtime),
                        contentDescription = stringResource(R.string.stats_focus),
                    )
                },
                onClick = onNavigateToDefaultLabel,
            )
            CheckboxListItem(
                title = stringResource(R.string.settings_auto_start_focus_title),
                subtitle = stringResource(R.string.settings_auto_start_focus_desc),
                checked = settings.autoStartFocus,
            ) {
                viewModel.setAutoStartWork(it)
            }
            CheckboxListItem(
                title = stringResource(R.string.settings_auto_start_break_title),
                subtitle = stringResource(R.string.settings_auto_start_break_desc),
                checked = settings.autoStartBreak,
            ) {
                viewModel.setAutoStartBreak(it)
            }

            BetterListItem(
                title = stringResource(R.string.settings_custom_start_of_day_title),
                subtitle = stringResource(R.string.settings_custom_start_of_day_desc),
                trailing =
                    secondsOfDayToTimerFormat(
                        uiState.settings.workdayStart,
                        DateFormat.is24HourFormat(context),
                    ),
                onClick = {
                    viewModel.setShowWorkdayStartPicker(true)
                },
            )

            val days =
                firstDayOfWeekOptions.map {
                    it.getDisplayName(
                        java.time.format.TextStyle.FULL_STANDALONE,
                        Locale.getDefault(),
                    )
                }

            DropdownMenuListItem(
                title = stringResource(R.string.settings_start_of_the_week),
                value = localizedDayNamesFull(javaLocale)[DayOfWeek.of(uiState.settings.firstDayOfWeek).ordinal],
                dropdownMenuOptions = days,
                onDropdownMenuItemSelected = {
                    viewModel.setFirstDayOfWeek(firstDayOfWeekOptions[it].isoDayNumber)
                },
            )

            SubtleHorizontalDivider()

            CompactPreferenceGroupTitle(text = stringResource(R.string.settings_display_and_appearance))

            IconListItem(
                title = stringResource(R.string.settings_user_interface),
                icon = {
                    Icon(
                        modifier = Modifier.padding(vertical = 12.dp),
                        imageVector = EvaIcons.Outline.ColorPalette,
                        contentDescription = stringResource(R.string.settings_user_interface),
                    )
                },
                onClick = onNavigateToUserInterface,
            )
            CheckboxListItem(
                title = stringResource(R.string.settings_keep_the_screen_on),
                checked = uiState.settings.uiSettings.keepScreenOn,
            ) {
                viewModel.setKeepScreenOn(it)
                if (!it) {
                    viewModel.setScreensaverMode(false)
                }
            }
            if (uiState.settings.isPro) {
                CheckboxListItem(
                    title = stringResource(R.string.settings_fullscreen_mode),
                    checked = uiState.settings.uiSettings.fullscreenMode,
                ) {
                    viewModel.setFullscreenMode(it)
                    if (!it) {
                        viewModel.setScreensaverMode(false)
                    }
                }
            } else {
                LockedCheckboxListItem(
                    title = stringResource(R.string.settings_fullscreen_mode),
                    checked = false,
                    enabled = false,
                ) {
                    viewModel.setFullscreenMode(it)
                    if (!it) {
                        viewModel.setScreensaverMode(false)
                    }
                }
            }
            if (uiState.settings.isPro) {
                CheckboxListItem(
                    title = stringResource(R.string.settings_screensaver_mode),
                    checked = uiState.settings.uiSettings.screensaverMode,
                    enabled = uiState.settings.uiSettings.keepScreenOn && uiState.settings.uiSettings.fullscreenMode,
                ) {
                    viewModel.setScreensaverMode(it)
                }
            } else {
                LockedCheckboxListItem(
                    title = stringResource(R.string.settings_screensaver_mode),
                    checked = false,
                    enabled = false,
                ) {
                }
            }
            AnimatedVisibility(
                uiState.settings.uiSettings.useDynamicColor &&
                    uiState.settings.uiSettings.themePreference.isDarkTheme(
                        isSystemInDarkTheme(),
                    ),
            ) {
                CheckboxListItem(
                    title = stringResource(R.string.settings_true_black_mode_title),
                    subtitle = stringResource(R.string.settings_true_black_mode_desc),
                    checked = uiState.settings.uiSettings.trueBlackMode,
                ) {
                    viewModel.setTrueBlackMode(it)
                }
            }
            CheckboxListItem(
                title = stringResource(R.string.settings_display_over_lock_screen),
                subtitle = stringResource(R.string.settings_display_over_lock_screen_desc),
                checked = uiState.settings.uiSettings.showWhenLocked,
            ) {
                viewModel.setShowWhenLocked(it)
            }

            SubtleHorizontalDivider()
            CompactPreferenceGroupTitle(text = stringResource(R.string.settings_notifications_title))
            IconListItem(
                title = stringResource(R.string.settings_notifications_title),
                icon = {
                    Icon(
                        modifier = Modifier.padding(vertical = 12.dp),
                        imageVector = EvaIcons.Outline.Bell,
                        contentDescription = stringResource(R.string.settings_notifications_title),
                    )
                },
                onClick = onNavigateToNotifications,
            )
            CheckboxListItem(
                title = stringResource(R.string.settings_do_not_disturb_mode),
                subtitle = if (isNotificationPolicyAccessGranted) null else stringResource(R.string.settings_click_to_grant_permission),
                checked = uiState.settings.uiSettings.dndDuringWork,
            ) {
                if (isNotificationPolicyAccessGranted) {
                    viewModel.setDndDuringWork(it)
                } else {
                    isNotificationPolicyAccessRequested = true
                    requestDndPolicyAccess(context.findActivity()!!)
                }
            }
        }
        if (uiState.showWorkdayStartPicker) {
            val workdayStart = LocalTime.fromSecondOfDay(uiState.settings.workdayStart)
            val timePickerState =
                rememberTimePickerState(
                    initialHour = workdayStart.hour,
                    initialMinute = workdayStart.minute,
                    is24Hour = DateFormat.is24HourFormat(context),
                )
            TimePicker(
                onDismiss = { viewModel.setShowWorkdayStartPicker(false) },
                onConfirm = {
                    viewModel.setWorkDayStart(timePickerState.toSecondOfDay())
                    viewModel.setShowWorkdayStartPicker(false)
                },
                timePickerState = timePickerState,
            )
        }
        if (uiState.showTimePicker) {
            val reminderTime =
                LocalTime.fromSecondOfDay(settings.productivityReminderSettings.secondOfDay)
            val timePickerState =
                rememberTimePickerState(
                    initialHour = reminderTime.hour,
                    initialMinute = reminderTime.minute,
                    is24Hour = DateFormat.is24HourFormat(context),
                )
            TimePicker(
                onDismiss = { viewModel.setShowTimePicker(false) },
                onConfirm = {
                    viewModel.setReminderTime(timePickerState.toSecondOfDay())
                    viewModel.setShowTimePicker(false)
                },
                timePickerState = timePickerState,
            )
        }
    }
}

private fun requestDndPolicyAccess(activity: ComponentActivity) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    activity.startActivity(intent)
}
