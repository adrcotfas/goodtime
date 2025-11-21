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
package com.apps.adrcotfas.goodtime.settings.timerstyle

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.DomainLabel
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.common.getAppLanguage
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.data.settings.ThemePreference
import com.apps.adrcotfas.goodtime.main.MainTimerView
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.settings.updateLauncherName
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.BetterListItem
import com.apps.adrcotfas.goodtime.ui.CheckboxListItem
import com.apps.adrcotfas.goodtime.ui.ColorSelectRow
import com.apps.adrcotfas.goodtime.ui.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.DropdownMenuListItem
import com.apps.adrcotfas.goodtime.ui.SliderListItem
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.TopBar
import com.apps.adrcotfas.goodtime.ui.dashedBorder
import com.apps.adrcotfas.goodtime.ui.timerFontWeights
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Unlock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_demo
import goodtime_productivity.composeapp.generated.resources.settings_general_title
import goodtime_productivity.composeapp.generated.resources.settings_hide_seconds
import goodtime_productivity.composeapp.generated.resources.settings_language
import goodtime_productivity.composeapp.generated.resources.settings_launcher_name
import goodtime_productivity.composeapp.generated.resources.settings_refresh_demo_label
import goodtime_productivity.composeapp.generated.resources.settings_show_sessions_long_break_desc
import goodtime_productivity.composeapp.generated.resources.settings_show_sessions_long_break_title
import goodtime_productivity.composeapp.generated.resources.settings_show_status_desc
import goodtime_productivity.composeapp.generated.resources.settings_show_status_title
import goodtime_productivity.composeapp.generated.resources.settings_theme
import goodtime_productivity.composeapp.generated.resources.settings_theme_options
import goodtime_productivity.composeapp.generated.resources.settings_timer_style_title
import goodtime_productivity.composeapp.generated.resources.settings_use_dynamic_color
import goodtime_productivity.composeapp.generated.resources.settings_user_interface
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import goodtime_productivity.composeapp.generated.resources.unlock_timer_style
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInterfaceScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPro = uiState.settings.isPro

    if (uiState.isLoading) return

    val timerStyle = if (isPro) uiState.settings.timerStyle else uiState.lockedTimerStyle
    val colorIndex = if (isPro) uiState.defaultLabel.colorIndex.toInt() else uiState.lockedTimerStyle.colorIndex
    val listState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_user_interface),
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
            var baseTime by rememberSaveable { mutableLongStateOf(25.minutes.inWholeMilliseconds) }
            var sessionsBeforeLongBreak by rememberSaveable { mutableIntStateOf(4) }
            var streak by rememberSaveable { mutableIntStateOf(1) }
            var timerType by rememberSaveable { mutableStateOf(TimerType.FOCUS) }

            CompactPreferenceGroupTitle(text = stringResource(Res.string.settings_general_title))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context.findActivity()
                BetterListItem(
                    title = stringResource(Res.string.settings_language),
                    trailing = context.getAppLanguage(),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                        intent.data = Uri.fromParts("package", activity?.packageName, null)
                        activity?.startActivity(intent)
                    },
                )
            }

            DropdownMenuListItem(
                title = stringResource(Res.string.settings_theme),
                value = stringArrayResource(Res.array.settings_theme_options)[uiState.settings.uiSettings.themePreference.ordinal],
                dropdownMenuOptions = stringArrayResource(Res.array.settings_theme_options).toList(),
                onDropdownMenuItemSelected = {
                    viewModel.setThemeOption(ThemePreference.entries[it])
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                CheckboxListItem(
                    title = stringResource(Res.string.settings_use_dynamic_color),
                    checked = uiState.settings.uiSettings.useDynamicColor,
                ) {
                    viewModel.setUseDynamicColor(it)
                }
            }

            DropdownMenuListItem(
                title = stringResource(Res.string.settings_launcher_name),
                value = stringArrayResource(Res.array.settings_launcher_name)[uiState.settings.uiSettings.launcherNameIndex],
                dropdownMenuOptions = stringArrayResource(Res.array.settings_launcher_name).toList(),
                onDropdownMenuItemSelected = { index ->
                    viewModel.setLauncherNameIndex(index)
                    context.findActivity()?.let { activity ->
                        updateLauncherName(context.packageManager, activity, index)
                    }
                },
            )

            SubtleHorizontalDivider()
            CompactPreferenceGroupTitle(text = stringResource(Res.string.settings_timer_style_title))

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                SliderListItem(
                    modifier = Modifier.weight(0.5f),
                    icon = { Icon(Icons.Default.FormatSize, contentDescription = null) },
                    min = timerStyle.minSize.toInt(),
                    max = timerStyle.maxSize.toInt(),
                    value = timerStyle.fontSize.toInt(),
                    onValueChange = {
                        viewModel.setTimerSize(it.toFloat())
                    },
                    showValue = false,
                )
                SliderListItem(
                    modifier = Modifier.weight(0.5f),
                    icon = { Icon(Icons.Default.FormatBold, contentDescription = null) },
                    min = timerFontWeights.first(),
                    max = timerFontWeights.last(),
                    steps = timerFontWeights.size - 2,
                    value = timerStyle.fontWeight,
                    onValueChange = {
                        viewModel.setTimerWeight(it)
                    },
                    showValue = false,
                )
            }
            ColorSelectRow(
                selectedIndex = colorIndex,
            ) {
                viewModel.setDefaultLabelColor(
                    it.toLong(),
                )
            }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(timerStyle.currentScreenWidth.dp)
                        .height(timerStyle.currentScreenWidth.dp * 0.6f)
                        .padding(16.dp)
                        .dashedBorder(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.medium,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(Res.string.settings_demo),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = {
                        baseTime =
                            Random.nextLong(
                                1.minutes.inWholeMilliseconds,
                                30.minutes.inWholeMilliseconds,
                            )
                        sessionsBeforeLongBreak = Random.nextInt(2, 8)
                        streak = Random.nextInt(1, sessionsBeforeLongBreak)
                        timerType = if (Random.nextBoolean()) TimerType.FOCUS else TimerType.BREAK
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.settings_refresh_demo_label),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                val timerUiState =
                    TimerUiState(
                        baseTime = baseTime,
                        timerState = TimerState.RUNNING,
                        timerType = timerType,
                        sessionsBeforeLongBreak = sessionsBeforeLongBreak,
                        longBreakData = LongBreakData(streak = streak),
                    )

                MainTimerView(
                    modifier =
                        Modifier
                            .padding(vertical = 16.dp)
                            .align(Alignment.Center),
                    gestureModifier = Modifier,
                    timerUiState = timerUiState,
                    timerStyle = timerStyle,
                    domainLabel =
                        DomainLabel(
                            label =
                                uiState.defaultLabel.copy(colorIndex = colorIndex.toLong()),
                        ),
                    onStart = {},
                    onToggle = null,
                )
            }
            Column {
                CheckboxListItem(
                    title = stringResource(Res.string.settings_show_status_title),
                    subtitle = stringResource(Res.string.settings_show_status_desc),
                    checked = timerStyle.showStatus,
                    onCheckedChange = {
                        viewModel.setShowStatus(it)
                    },
                )
                CheckboxListItem(
                    title = stringResource(Res.string.settings_show_sessions_long_break_title),
                    subtitle = stringResource(Res.string.settings_show_sessions_long_break_desc),
                    checked = timerStyle.showStreak,
                    onCheckedChange = {
                        viewModel.setShowStreak(it)
                    },
                )
                CheckboxListItem(
                    title = stringResource(Res.string.settings_hide_seconds),
                    checked = timerStyle.minutesOnly,
                    onCheckedChange = {
                        viewModel.setTimerMinutesOnly(it)
                    },
                )
                if (!isPro) {
                    ActionCard(icon = {
                        Icon(
                            imageVector = EvaIcons.Outline.Unlock,
                            contentDescription = stringResource(Res.string.unlock_premium),
                        )
                    }, description = stringResource(Res.string.unlock_timer_style)) {
                        onNavigateToPro()
                    }
                }
            }
        }
    }
}
