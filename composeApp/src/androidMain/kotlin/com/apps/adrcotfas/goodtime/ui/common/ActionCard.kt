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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Heart

@Composable
fun ActionCard(
    icon: (@Composable () -> Unit)? = null,
    useSecondaryColor: Boolean = false,
    cta: String? = null,
    description: String,
    onClick: () -> Unit,
) {
    val colors =
        CardDefaults.cardColors().copy(
            containerColor =
                if (useSecondaryColor) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.38f,
                    )
                },
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    Card(
        colors = colors,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    onClick()
                },
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    it()
                }
            }
            Text(
                modifier =
                    Modifier
                        .padding(12.dp)
                        .weight(1f),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
            cta?.let {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = cta,
                        color = if (useSecondaryColor) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AskForPermissionCardPreview() {
    ActionCard(
        cta = "Allow",
        description = "Allow this app to run in the background",
        onClick = {},
    )
}

@Preview
@Composable
fun ActionCardWithIcon() {
    ActionCard(
        icon = {
            Icon(
                imageVector = EvaIcons.Outline.Heart,
                contentDescription = "Support development",
            )
        },
        description = "Allow this app to run in the background",
        onClick = {},
    )
}
