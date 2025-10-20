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
package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.common.rememberMutableStateListOf
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.stats.LabelChip

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SelectLabelDialog(
    title: String,
    singleSelection: Boolean,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    labels: List<LabelData>,
    showIcons: Boolean = true,
    forceShowClearLabel: Boolean = false,
    extraContent: @Composable (() -> Unit)? = null,
    initialSelectedLabels: List<String> = emptyList(),
    buttons: @Composable (() -> Unit)? = null,
) {
    val selectedLabels = rememberMutableStateListOf(*initialSelectedLabels.toTypedArray())

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .background(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = 24.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                extraContent?.invoke()
                FlowRow(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.forEach { label ->
                        LabelChip(
                            label.name,
                            label.colorIndex,
                            selected = selectedLabels.contains(label.name),
                            showIcon = showIcons,
                        ) {
                            if (singleSelection) {
                                onConfirm(listOf(label.name))
                            } else {
                                val alreadySelected = selectedLabels.contains(label.name)
                                if (selectedLabels.size == 1 && alreadySelected) return@LabelChip
                                if (selectedLabels.contains(label.name)) {
                                    selectedLabels.remove(label.name)
                                } else {
                                    selectedLabels.add(label.name)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                if (!singleSelection) {
                    Row(
                        modifier =
                            Modifier
                                .height(40.dp)
                                .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(id = android.R.string.cancel)) }
                        TextButton(onClick = { onConfirm(selectedLabels) }) {
                            Text(
                                stringResource(id = android.R.string.ok),
                            )
                        }
                    }
                } else if (buttons != null) {
                    buttons()
                } else if (forceShowClearLabel ||
                    (initialSelectedLabels.isNotEmpty() && initialSelectedLabels.first() != Label.DEFAULT_LABEL_NAME)
                ) {
                    AlertDialogButtonStack {
                        TextButton(onClick = { onConfirm(listOf(Label.DEFAULT_LABEL_NAME)) }) {
                            Text(stringResource(R.string.labels_clear_label))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom layout for stacking buttons as per the material design spec.
 * If all buttons fit on one line. Place them like that.
 * When they don't fit, put them vertically below each other.
 *
 * We need to implement this manually because the compose material implementation does not follow these guidelines and places buttons like [FlowRow].
 */
@Composable
fun AlertDialogButtonStack(
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = buttons,
    ) { measurables, constraints ->
        // Don't constrain child views further, measure them with given constraints.
        // List of measured children
        val placeables =
            measurables.map { measurable ->
                // Measure each children
                measurable.measure(constraints)
            }

        // Calculate amount of required space for the buttons.
        val widthOfButtons =
            placeables.sumOf { placeable ->
                placeable.width
            }

        if (widthOfButtons < constraints.maxWidth) {
            // Calculate height of the whole button row.
            val maxHeight =
                placeables.maxOf { placeable ->
                    placeable.height
                }

            // When all buttons fit horizontally, place them like that.
            // Place buttons from the end to the start of the layout.
            layout(constraints.maxWidth, maxHeight) {
                // Track the current X coordinate for placing children.
                var xPosition = constraints.maxWidth

                // Place children in the parent layout.
                placeables.forEachIndexed { index, placeable ->
                    if (index == 2 && index == placeables.lastIndex) {
                        // When we place the third button horizontally,
                        // it is a neutral button and it should be pushed to the side.
                        placeable.placeRelative(
                            x = 0,
                            y = 0,
                        )
                    } else {
                        // Normal buttons are placed next to each other.
                        placeable.placeRelative(
                            x = xPosition - placeable.width,
                            y = 0,
                        )
                    }

                    // Move the X coordinate by the currently placed button.
                    xPosition -= placeable.width
                }
            }
        } else {
            val heightOfButtons =
                placeables.sumOf { placeable ->
                    placeable.height
                }

            // When all buttons don't fit. Place them vertically.
            layout(constraints.maxWidth, heightOfButtons) {
                // Track the Y coordinate we have placed children up to.
                var yPosition = 0

                // Place children in the parent layout.
                placeables.forEach { placeable ->
                    // Position item on the screen.
                    placeable.placeRelative(
                        x = constraints.maxWidth - placeable.width,
                        y = yPosition,
                    )

                    // Move the Y coordinate by the currently placed button.
                    yPosition += placeable.height
                }
            }
        }
    }
}
