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

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.common.getVersionName
import com.apps.adrcotfas.goodtime.settings.ActionSection
import com.apps.adrcotfas.goodtime.ui.IconTextButton
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Info
import compose.icons.evaicons.outline.PieChart
import compose.icons.evaicons.outline.Settings
import compose.icons.evaicons.outline.Sync
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.about_and_feedback_title
import goodtime_productivity.composeapp.generated.resources.backup_and_restore_title
import goodtime_productivity.composeapp.generated.resources.labels_title
import goodtime_productivity.composeapp.generated.resources.product_name_long
import goodtime_productivity.composeapp.generated.resources.settings_title
import goodtime_productivity.composeapp.generated.resources.stats_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationSheet(
    navController: NavController,
    onHideSheet: () -> Unit,
    actionBadgeCount: Int,
    showPro: Boolean,
    isUpdateAvailable: Boolean,
    wasNotificationPermissionDenied: Boolean,
    onUpdateClicked: () -> Unit,
    onNotificationPermissionGranted: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onHideSheet,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        MainNavigationSheetContent(
            settingsBadgeItemCount = actionBadgeCount,
            onUpdateClicked = onUpdateClicked,
            showPro = showPro,
            isUpdateAvailable = isUpdateAvailable,
            wasNotificationPermissionDenied = wasNotificationPermissionDenied,
            onNotificationPermissionGranted = onNotificationPermissionGranted,
            navigateToLabels = {
                navController.navigate(LabelsDest)
                onHideSheet()
            },
            navigateToStats = {
                navController.navigate(StatsDest)
                onHideSheet()
            },
            navigateToSettings = {
                navController.navigate(SettingsDest)
                onHideSheet()
            },
            navigateToBackup = {
                navController.navigate(BackupDest)
                onHideSheet()
            },
            navigateToAbout = {
                navController.navigate(AboutDest)
                onHideSheet()
            },
            navigateToPro = {
                navController.navigate(ProDest)
                onHideSheet()
            },
        )
    }
}

@Composable
fun MainNavigationSheetContent(
    settingsBadgeItemCount: Int,
    onUpdateClicked: () -> Unit,
    showPro: Boolean,
    isUpdateAvailable: Boolean,
    wasNotificationPermissionDenied: Boolean,
    onNotificationPermissionGranted: (Boolean) -> Unit,
    navigateToLabels: () -> Unit,
    navigateToStats: () -> Unit,
    navigateToSettings: () -> Unit,
    navigateToBackup: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToPro: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .animateContentSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                bitmap = icon.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(),
                contentDescription = null,
            )
            Text(
                text = stringResource(Res.string.product_name_long),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
        SubtleHorizontalDivider()

        if (showPro) {
            Spacer(Modifier.height(12.dp))
            ProListItem { navigateToPro() }
        }
        IconTextButton(
            title = stringResource(Res.string.labels_title),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = stringResource(Res.string.labels_title),
                )
            },
            onClick = navigateToLabels,
        )

        IconTextButton(
            title = stringResource(Res.string.stats_title),
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.PieChart,
                    contentDescription = stringResource(Res.string.stats_title),
                )
            },
            onClick = navigateToStats,
        )
        IconTextButton(
            title = stringResource(Res.string.backup_and_restore_title),
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Sync,
                    contentDescription = stringResource(Res.string.backup_and_restore_title),
                )
            },
            onClick = {
                navigateToBackup()
            },
        )
        if (settingsBadgeItemCount != 0) {
            ActionSection(
                wasNotificationPermissionDenied = wasNotificationPermissionDenied,
                isUpdateAvailable = isUpdateAvailable,
                onNotificationPermissionGranted = onNotificationPermissionGranted,
                onUpdateClicked = onUpdateClicked,
            )
        } else {
            SubtleHorizontalDivider()
        }
        IconTextButton(
            title = stringResource(Res.string.settings_title),
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Settings,
                    contentDescription = stringResource(Res.string.settings_title),
                )
            },
            onClick = navigateToSettings,
        )
        IconTextButton(
            title = stringResource(Res.string.about_and_feedback_title),
            subtitle = "v${context.getVersionName()}",
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Info,
                    contentDescription = stringResource(Res.string.about_and_feedback_title),
                )
            },
            onClick = {
                navigateToAbout()
            },
        )
    }
}
