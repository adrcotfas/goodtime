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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun RadioGroupDialog(
    title: String,
    onDismiss: () -> Unit,
    initialIndex: Int,
    radioOptions: List<String>,
    onItemSelected: (Int) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .background(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                    ),
        ) {
            val radioOptionsIndexes = radioOptions.indices.toList()
            val (selectedOption, onOptionSelected) =
                remember {
                    mutableIntStateOf(
                        radioOptionsIndexes[initialIndex],
                    )
                }
            Column(
                modifier =
                    Modifier
                        .padding(
                            top = 24.dp,
                        ).fillMaxWidth()
                        .selectableGroup(),
            ) {
                Text(
                    modifier =
                        Modifier
                            .padding(start = 24.dp, bottom = 20.dp)
                            .fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                radioOptions.forEach { text ->
                    val selectedIndex = radioOptions.indexOf(text)
                    Row(
                        Modifier
                            .height(56.dp)
                            .selectable(
                                selected = (selectedIndex == selectedOption),
                                onClick = {
                                    onOptionSelected(selectedIndex)
                                    onItemSelected(selectedIndex)
                                    onDismiss()
                                },
                                role = Role.RadioButton,
                            ).padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (selectedIndex == selectedOption),
                            onClick = null,
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
                TextButton(
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(end = 8.dp, bottom = 4.dp),
                    onClick = onDismiss,
                ) { Text(stringResource(id = android.R.string.cancel)) }
            }
        }
    }
}
