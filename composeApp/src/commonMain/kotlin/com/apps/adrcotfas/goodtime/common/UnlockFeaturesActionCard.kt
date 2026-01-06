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
package com.apps.adrcotfas.goodtime.common

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.apps.adrcotfas.goodtime.ui.ActionCard
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Unlock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import goodtime_productivity.composeapp.generated.resources.unlock_premium_to_access_features
import org.jetbrains.compose.resources.stringResource

@Composable
fun UnlockFeaturesActionCard(
    hasPro: Boolean,
    onNavigateToPro: () -> Unit,
) {
    if (!hasPro) {
        ActionCard(
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Unlock,
                    contentDescription = stringResource(Res.string.unlock_premium),
                )
            },
            description = stringResource(Res.string.unlock_premium_to_access_features),
        ) {
            onNavigateToPro()
        }
    }
}
