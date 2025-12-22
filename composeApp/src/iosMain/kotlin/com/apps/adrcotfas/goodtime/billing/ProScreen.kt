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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.UrlOpener
import com.apps.adrcotfas.goodtime.ui.TopBar
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.product_name_long
import goodtime_productivity.composeapp.generated.resources.support_development
import goodtime_productivity.composeapp.generated.resources.support_donate_desc
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc1
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc3
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ProScreen(onNavigateBack: () -> Unit) {
    val options =
        remember {
            PaywallOptions(dismissRequest = onNavigateBack)
        }

    Paywall(options)
//    val listState = rememberScrollState()
//    Scaffold(
//        topBar = {
//            TopBar(
//                title = stringResource(Res.string.support_development),
//                icon = Icons.Default.Close,
//                onNavigateBack = { onNavigateBack() },
//                showSeparator = listState.canScrollBackward,
//            )
//        },
//    ) { paddingValues ->
//        Column(
//            modifier =
//                Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues)
//                    .verticalScroll(listState),
//        ) {
//            val productName = stringResource(Res.string.product_name_long)
//            Text(
//                modifier = Modifier.padding(16.dp),
//                text =
//                    stringResource(Res.string.unlock_premium_desc1, productName) + "\n" + "\n" +
//                        stringResource(Res.string.support_donate_desc) + "\n" +
//                        stringResource(Res.string.unlock_premium_desc3),
//                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
//            )
//        }
//    }
}
