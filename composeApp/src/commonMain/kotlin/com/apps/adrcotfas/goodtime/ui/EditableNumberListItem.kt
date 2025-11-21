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
package com.apps.adrcotfas.goodtime.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.max
import kotlin.math.min
import kotlin.toString

@Composable
fun EditableNumberListItem(
    title: String,
    value: Int?,
    minValue: Int = 1,
    maxValue: Int = 900,
    enabled: Boolean = true,
    restoreValueOnClearFocus: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onValueChange: (Int) -> Unit,
    onValueEmpty: (Boolean) -> Unit = {},
    enableSwitch: Boolean = false,
    switchValue: Boolean = true,
    onSwitchChange: (Boolean) -> Unit = {},
) {
    val maxValueDigits = maxValue.toString().length

    var text by remember(value) {
        mutableStateOf(
            value?.toString() ?: "",
        )
    }
    var textFieldValue by remember(text) {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(maxValueDigits, maxValueDigits),
            ),
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        val endRange = if (isFocused) text.length else 0
        textFieldValue =
            TextFieldValue(
                text = text,
                selection = TextRange(0, endRange),
            )
    }

    val clickableModifier =
        if (enabled && switchValue) {
            Modifier.clickable {
                focusRequester.requestFocus()
            }
        } else {
            Modifier
        }

    val colors =
        if (enabled && switchValue) {
            ListItemDefaults.colors(containerColor = Color.Transparent)
        } else {
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = ListItemDefaults.colors().disabledHeadlineColor,
            )
        }
    val strokeColor =
        if (enabled && switchValue) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(
                0.38f,
            )
        }

    ListItem(
        modifier = clickableModifier,
        colors = colors,
        headlineContent = { Text(text = title) },
        trailingContent = {
            BasicTextField(
                value = textFieldValue,
                enabled = enabled && switchValue,
                interactionSource = interactionSource,
                onValueChange = {
                    if (it.text != text) {
                        if (it.text.length <= maxValue.toString().length && it.text.all { char -> char.isDigit() }) {
                            val newValue = min(max(it.text.toIntOrNull() ?: 0, minValue), maxValue)
                            val empty = it.text.isEmpty()
                            onValueEmpty(empty)
                            val newText = if (empty) "" else newValue.toString()
                            val newSelection = TextRange(newText.length)
                            textFieldValue = it.copy(text = newText, selection = newSelection)
                            if (!empty) {
                                onValueChange(newValue)
                            }
                        }
                    } else {
                        textFieldValue = it
                    }
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center,
                        color = colors.headlineColor,
                    ),
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                modifier =
                    Modifier
                        .widthIn(min = 32.dp, max = 64.dp)
                        .border(1.dp, strokeColor, MaterialTheme.shapes.medium)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .focusRequester(focusRequester)
                        .clearFocusOnKeyboardDismiss {
                            if (restoreValueOnClearFocus) {
                                textFieldValue =
                                    textFieldValue.copy(
                                        text = value.toString(),
                                    )
                            }
                        },
            )
        },
        leadingContent = {
            if (enableSwitch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.clip(CircleShape)) {
                        Checkbox(
                            checked = switchValue,
                            enabled = enabled,
                            onCheckedChange = onSwitchChange,
                        )
                    }
                    VerticalDivider(modifier = Modifier.height(32.dp), color = colors.headlineColor)
                }
            } else if (icon != null) {
                icon()
            }
        },
    )
}

@Preview
@Composable
fun EditableNumberListItemPreview() {
    EditableNumberListItem(
        title = "Focus time",
        value = 25,
        onValueChange = {},
        enableSwitch = true,
    )
}
