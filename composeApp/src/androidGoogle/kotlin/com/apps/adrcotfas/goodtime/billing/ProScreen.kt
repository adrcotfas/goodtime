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
package com.apps.adrcotfas.goodtime.billing

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.main.ProListItem
import com.apps.adrcotfas.goodtime.ui.ApplicationTheme
import com.apps.adrcotfas.goodtime.ui.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.TopBar
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Bell
import compose.icons.evaicons.outline.ColorPalette
import compose.icons.evaicons.outline.Heart
import compose.icons.evaicons.outline.PieChart
import compose.icons.evaicons.outline.Sync
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.feature_backup_desc1
import goodtime_productivity.composeapp.generated.resources.feature_backup_desc2
import goodtime_productivity.composeapp.generated.resources.feature_backup_title
import goodtime_productivity.composeapp.generated.resources.feature_labels_desc1
import goodtime_productivity.composeapp.generated.resources.feature_labels_desc2
import goodtime_productivity.composeapp.generated.resources.feature_labels_title
import goodtime_productivity.composeapp.generated.resources.feature_notifications_desc1
import goodtime_productivity.composeapp.generated.resources.feature_notifications_title
import goodtime_productivity.composeapp.generated.resources.feature_stats_desc1
import goodtime_productivity.composeapp.generated.resources.feature_stats_desc2
import goodtime_productivity.composeapp.generated.resources.feature_stats_desc3
import goodtime_productivity.composeapp.generated.resources.feature_stats_title
import goodtime_productivity.composeapp.generated.resources.feature_support_desc1
import goodtime_productivity.composeapp.generated.resources.feature_support_title
import goodtime_productivity.composeapp.generated.resources.feature_timer_customization_desc1
import goodtime_productivity.composeapp.generated.resources.feature_timer_customization_desc2
import goodtime_productivity.composeapp.generated.resources.feature_timer_customization_title
import goodtime_productivity.composeapp.generated.resources.product_name_long
import goodtime_productivity.composeapp.generated.resources.unlock_premium
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc1
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc2
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc3
import goodtime_productivity.composeapp.generated.resources.unlock_premium_tagline
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ProScreen(
    billing: GoogleBilling = koinInject<BillingAbstract>() as GoogleBilling,
    onNavigateBack: () -> Unit,
) {
    val activity = LocalActivity.current
    val productDetails by billing.productDetails.collectAsStateWithLifecycle()

    val isPro by billing.isPro.collectAsStateWithLifecycle(false)
    val isPending by billing.purchasePending.collectAsStateWithLifecycle(false)

    LaunchedEffect(isPending, isPro) {
        if (isPending || isPro) {
            onNavigateBack()
        }
    }

    productDetails.let {
        val offerDetails = it?.oneTimePurchaseOfferDetails
        if (it == null || activity == null || offerDetails == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val priceString = offerDetails.formattedPrice
            ProScreenContent(onNavigateBack, priceString) { billing.buy(activity) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreenContent(
    onNavigateBack: () -> Unit,
    priceString: String,
    onClick: () -> Unit,
) {
    val listState = rememberScrollState()
    val color = MaterialTheme.colorScheme.primary
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.unlock_premium),
                icon = Icons.Default.Close,
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
        bottomBar = {
            if (listState.canScrollForward) {
                SubtleHorizontalDivider()
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(Res.string.unlock_premium_tagline),
                    style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                )
                ProListItem(subtitle = priceString, centered = true) {
                    onClick()
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(Modifier.verticalScroll(listState)) {
                val productName = stringResource(Res.string.product_name_long)
                Text(
                    modifier = Modifier.padding(12.dp),
                    text =
                        stringResource(Res.string.unlock_premium_desc1, productName) + "\n" +
                            "\n" +
                            stringResource(Res.string.unlock_premium_desc2) +
                            "\n" +
                            stringResource(Res.string.unlock_premium_desc3),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                )

                Spacer(modifier = Modifier.height(16.dp))
                ProFeatureListItem(
                    title = stringResource(Res.string.feature_labels_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_labels_desc1),
                            stringResource(Res.string.feature_labels_desc2),
                        ),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    color = color,
                )

                ProFeatureListItem(
                    title = stringResource(Res.string.feature_timer_customization_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_timer_customization_desc1),
                            stringResource(Res.string.feature_timer_customization_desc2),
                        ),
                    icon = EvaIcons.Outline.ColorPalette,
                    color = color,
                )

                ProFeatureListItem(
                    title = stringResource(Res.string.feature_notifications_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_notifications_desc1),
                        ),
                    icon = EvaIcons.Outline.Bell,
                    color = color,
                )

                ProFeatureListItem(
                    title = stringResource(Res.string.feature_stats_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_stats_desc1),
                            stringResource(Res.string.feature_stats_desc2),
                            stringResource(Res.string.feature_stats_desc3),
                        ),
                    icon = EvaIcons.Outline.PieChart,
                    color = color,
                )

                ProFeatureListItem(
                    title = stringResource(Res.string.feature_backup_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_backup_desc1),
                            stringResource(Res.string.feature_backup_desc2),
                        ),
                    icon = EvaIcons.Outline.Sync,
                    color = color,
                )
                ProFeatureListItem(
                    title = stringResource(Res.string.feature_support_title),
                    subtitle =
                        listOf(
                            stringResource(Res.string.feature_support_desc1),
                        ),
                    icon = EvaIcons.Outline.Heart,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun ProFeatureListItem(
    title: String,
    subtitle: List<String>,
    icon: ImageVector,
    color: Color,
) {
    val background = color.copy(alpha = 0.38f)
    ListItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        leadingContent = {
            Icon(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(background)
                        .padding(4.dp),
                imageVector = icon,
                tint = color,
                contentDescription = title,
            )
        },
        headlineContent = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                subtitle.forEach {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}

@Preview
@Composable
fun ProScreenPreview() {
    ApplicationTheme(darkTheme = true) {
        ProScreenContent(onNavigateBack = {}, onClick = {}, priceString = "42 USD")
    }
}
