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
package com.apps.adrcotfas.goodtime.main

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.ui.common.IconTextButton
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Heart

@Composable
fun ProListItem(
    subtitle: String? = null,
    centered: Boolean = false,
    onClick: () -> Unit,
) {
    IconTextButton(
        title = stringResource(R.string.support_development),
        subtitle = subtitle,
        icon = {
            Icon(
                imageVector = EvaIcons.Outline.Heart,
                contentDescription = stringResource(R.string.support_development),
            )
        },
        isFilled = true,
        centered = centered,
    ) { onClick() }
}
