package com.apps.adrcotfas.goodtime.settings.user_interface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.DomainLabel
import com.apps.adrcotfas.goodtime.bl.TimerState
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.settings.LongBreakData
import com.apps.adrcotfas.goodtime.main.MainTimerView
import com.apps.adrcotfas.goodtime.main.TimerUiState
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.ui.common.CheckboxListItem
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.dashedBorder
import com.apps.adrcotfas.goodtime.ui.lightPalette
import com.apps.adrcotfas.goodtime.ui.timerFontWeights
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

//TODO: Try to find a workaround until this is fixed: https://issuetracker.google.com/issues/372044241
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerStyleScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Boolean,
    showTopBar: Boolean,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isLoading) return

    val timerStyle = uiState.settings.timerStyle

    Scaffold(
        topBar = {
            TopBar(
                isVisible = showTopBar,
                title = "Timer style",
                onNavigateBack = { onNavigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            var colorIndex by rememberSaveable { mutableIntStateOf(17) }
            var baseTime by rememberSaveable { mutableLongStateOf(25.minutes.inWholeMilliseconds) }
            var sessionsBeforeLongBreak by rememberSaveable { mutableIntStateOf(4) }
            var streak by rememberSaveable { mutableIntStateOf(1) }
            var timerType by rememberSaveable { mutableStateOf(TimerType.WORK) }

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
                    showValue = false
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
                    showValue = false
                )
            }
            Box(
                modifier = Modifier
                    .size(timerStyle.currentScreenWidth.dp)
                    .padding(16.dp)
                    .dashedBorder(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "demo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = {
                        val oldColorIndex = colorIndex
                        var newColorIndex = Random.nextInt(lightPalette.lastIndex)
                        while (newColorIndex == oldColorIndex) {
                            newColorIndex = Random.nextInt(lightPalette.lastIndex)
                        }
                        colorIndex = newColorIndex
                        baseTime = Random.nextLong(
                            1.minutes.inWholeMilliseconds,
                            30.minutes.inWholeMilliseconds
                        )
                        sessionsBeforeLongBreak = Random.nextInt(2, 8)
                        streak = Random.nextInt(1, sessionsBeforeLongBreak)
                        timerType = if (Random.nextBoolean()) TimerType.WORK else TimerType.BREAK
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh demo label",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val demoLabelNames = listOf(
                    "numerical methods",
                    "particle physics",
                    "epigenetics",
                    "astrophysics",
                    "kinetics",
                    "computer vision",
                    "neurobiology",
                    "dermatology",
                    "nutrition",
                    "philosophy",
                    "calligraphy",
                    "surrealism",
                    "meditation",
                    "guitar",
                    "drums",
                    "piano",
                    "thermodynamics",
                    "calculus",
                    "ecology",
                    "nanophotonics"
                )
                val timerUiState = TimerUiState(
                    baseTime = baseTime,
                    timerState = TimerState.RUNNING,
                    timerType = timerType,
                    sessionsBeforeLongBreak = sessionsBeforeLongBreak,
                    longBreakData = LongBreakData(streak = streak)
                )
                assert(lightPalette.lastIndex == demoLabelNames.lastIndex)

                MainTimerView(
                    modifier = Modifier,
                    gestureModifier = Modifier,
                    timerUiState = timerUiState,
                    timerStyle = timerStyle,
                    domainLabel = DomainLabel(
                        label = Label(
                            name = demoLabelNames[colorIndex],
                            colorIndex = colorIndex.toLong()
                        )
                    ),
                    onStart = {},
                    onToggle = null,
                )
            }
            Column {
                CheckboxListItem(
                    title = "Show status",
                    subtitle = "An icon indicating work or break",
                    checked = timerStyle.showStatus,
                    onCheckedChange = {
                        viewModel.setShowStatus(it)
                    })
                CheckboxListItem(
                    title = "Show current streak",
                    subtitle = "For countdown timers with long breaks enabled",
                    checked = timerStyle.showStreak,
                    onCheckedChange = {
                        viewModel.setShowStreak(it)
                    })
                CheckboxListItem(
                    title = "Show label",
                    checked = timerStyle.showLabel,
                    onCheckedChange = {
                        viewModel.setShowLabel(it)
                    })
//                CheckboxListItem(
//                    title = "Minutes only",
//                    checked = timerStyle.minutesOnly,
//                    onCheckedChange = {
//                        viewModel.setTimerMinutesOnly(it)
//                    })
            }
        }
    }
}
