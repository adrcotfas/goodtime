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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun BetterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        modifier =
            Modifier
                .crop(vertical = DROPDOWN_MENU_CORNER.dp)
                .clip(RoundedCornerShape(DROPDOWN_MENU_CORNER.dp)),
        shape = MaterialTheme.shapes.medium,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        content()
    }
}

@Composable
fun BetterDropdownMenu(
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    expanded: Boolean,
    value: String,
    dropdownMenuOptions: List<String>,
    onDismissRequest: () -> Unit,
    onDropdownMenuItemSelected: (Int) -> Unit,
) {
    val paddingModifier = Modifier.padding(end = DROPDOWN_MENU_END_PADDING.dp)
    BetterDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        dropdownMenuOptions.forEachIndexed { index, it ->
            val isSelected = it == value
            val isFirstIndex = index == 0
            val isLastIndex = index == dropdownMenuOptions.lastIndex

            val indexModifier =
                if (isFirstIndex) {
                    firstMenuItemModifier
                } else if (isLastIndex) {
                    lastMenuItemModifier
                } else {
                    Modifier
                }

            val selectionModifier =
                Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.18f,
                    ),
                )

            val modifier = if (isSelected) indexModifier.then(selectionModifier) else indexModifier

            DropdownMenuItem(
                modifier = modifier,
                text = {
                    Text(
                        modifier = paddingModifier,
                        text = it,
                        style =
                            if (it == value) {
                                textStyle.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                textStyle
                            },
                    )
                },
                onClick = {
                    onDropdownMenuItemSelected(index)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun DropdownMenuBox(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier.wrapContentSize(),
    colored: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    value: String,
    options: List<String>,
    onDismissRequest: () -> Unit,
    onDropdownMenuItemSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val selectionModifier =
        Modifier.background(
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.18f,
            ),
        )

    Box(modifier) {
        Row(
            modifier =
                contentModifier
                    .clip(MaterialTheme.shapes.medium)
                    .then(if (colored) selectionModifier else Modifier)
                    .clickable {
                        expanded = true
                    }.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value,
                style = textStyle.copy(color = if (colored) MaterialTheme.colorScheme.primary else textStyle.color),
            )
            Spacer(modifier = Modifier.width(DROPDOWN_MENU_END_PADDING.dp))
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "dropdown icon rotation",
            )
            Icon(
                modifier =
                    Modifier.graphicsLayer {
                        rotationZ = rotation
                    },
                imageVector = Icons.Default.ExpandMore,
                tint = if (colored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                contentDescription = "Dropdown",
            )
        }
        BetterDropdownMenu(
            textStyle = textStyle,
            expanded = expanded,
            value = value,
            onDismissRequest = {
                expanded = false
                onDismissRequest()
            },
            dropdownMenuOptions = options,
            onDropdownMenuItemSelected = {
                onDropdownMenuItemSelected(it)
                expanded = false
            },
        )
    }
}

private const val DROPDOWN_MENU_CORNER = 8
private const val DROPDOWN_MENU_END_PADDING = 32

val firstMenuItemModifier =
    Modifier.clip(
        RoundedCornerShape(
            topStart = DROPDOWN_MENU_CORNER.dp,
            topEnd = DROPDOWN_MENU_CORNER.dp,
        ),
    )

val lastMenuItemModifier =
    Modifier.clip(
        RoundedCornerShape(
            bottomStart = DROPDOWN_MENU_CORNER.dp,
            bottomEnd = DROPDOWN_MENU_CORNER.dp,
        ),
    )
