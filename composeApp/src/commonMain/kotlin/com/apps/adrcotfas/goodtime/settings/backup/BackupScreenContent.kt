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
package com.apps.adrcotfas.goodtime.settings.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.apps.adrcotfas.goodtime.data.local.backup.BackupUiState
import com.apps.adrcotfas.goodtime.data.local.backup.isBusy
import com.apps.adrcotfas.goodtime.ui.ActionCard
import com.apps.adrcotfas.goodtime.ui.CircularProgressListItem
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.SwitchListItem
import com.apps.adrcotfas.goodtime.ui.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Unlock
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.backup_auto_backup
import goodtime_productivity.composeapp.generated.resources.backup_export_backup
import goodtime_productivity.composeapp.generated.resources.backup_export_csv
import goodtime_productivity.composeapp.generated.resources.backup_export_json
import goodtime_productivity.composeapp.generated.resources.backup_restore_backup
import goodtime_productivity.composeapp.generated.resources.backup_the_file_can_be_imported_back
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import goodtime_productivity.composeapp.generated.resources.unlock_premium_to_access_features
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreenContent(
    uiState: BackupUiState,
    onNavigateToPro: () -> Unit,
    onNavigateBack: () -> Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onBackupToCsv: () -> Unit,
    onBackupToJson: () -> Unit,
) {
    val listState = rememberScrollState()
    val enabled = uiState.isPro

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = stringResource(Res.string.backup_and_restore_title),
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
                if (!enabled) {
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
                SwitchListItem(
                    title = stringResource(Res.string.backup_auto_backup),
                    checked = uiState.backupSettings.autoBackupEnabled,
                    enabled = enabled,
                    onCheckedChange = { onAutoBackupToggle(it) },
                )
                SubtleHorizontalDivider()
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_backup),
                    subtitle = stringResource(Res.string.backup_the_file_can_be_imported_back),
                    enabled = enabled,
                    showProgress = uiState.isBackupInProgress,
                ) {
                    onBackup()
                }
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_restore_backup),
                    enabled = enabled,
                    showProgress = uiState.isRestoreInProgress,
                ) {
                    onRestore()
                }
                SubtleHorizontalDivider()
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_csv),
                    enabled = enabled,
                    showProgress = uiState.isCsvBackupInProgress,
                ) {
                    onBackupToCsv()
                }
                CircularProgressListItem(
                    title = stringResource(Res.string.backup_export_json),
                    enabled = enabled,
                    showProgress = uiState.isJsonBackupInProgress,
                ) {
                    onBackupToJson()
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isBusy,
            enter = fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clearAndSetSemantics { }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume taps */ },
                        ),
            )
        }
    }
}
