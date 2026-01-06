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
fun UnlockFeaturesActionCard(hasPro: Boolean, onNavigateToPro: () -> Unit) {
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