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

import androidx.compose.foundation.Image
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.openUrl
import com.apps.adrcotfas.goodtime.ui.TopBar
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.product_name_long
import goodtime_productivity.composeapp.generated.resources.support_development
import goodtime_productivity.composeapp.generated.resources.support_donate_desc
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc1
import goodtime_productivity.composeapp.generated.resources.unlock_premium_desc3
import org.jetbrains.compose.resources.stringResource

private const val PAYPAL_URL = "https://paypal.me/adrcotfas"
private const val BTC_URL =
    "https://btcscan.org/address/bc1q0y78e0ylcfme8tc5eakhdp8akywpmhhrmcnmrt"
private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/adrcotfas"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ProScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.support_development),
                icon = Icons.Default.Close,
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
                    .verticalScroll(listState),
        ) {
            val productName = stringResource(Res.string.product_name_long)
            Text(
                modifier = Modifier.padding(16.dp),
                text =
                    stringResource(Res.string.unlock_premium_desc1, productName) + "\n" + "\n" +
                        stringResource(Res.string.support_donate_desc) + "\n" +
                        stringResource(Res.string.unlock_premium_desc3),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            )

            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0xFFFFDD00)),
                    onClick = {
                        context.openUrl(BUY_ME_A_COFFEE_URL)
                    },
                ) {
                    Image(
                        modifier = Modifier.height(32.dp),
                        painter = painterResource(R.drawable.bmc_button),
                        contentDescription = "",
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0xFF003286)),
                    onClick = {
                        context.openUrl(PAYPAL_URL)
                    },
                ) {
                    Image(
                        modifier = Modifier.height(32.dp),
                        painter = painterResource(R.drawable.pp_button),
                        contentDescription = "",
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0xFFF7931A)),
                    onClick = {
                        context.openUrl(BTC_URL)
                    },
                ) {
                    Image(
                        modifier = Modifier.height(32.dp),
                        painter = painterResource(R.drawable.btc_button),
                        contentDescription = "",
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ProScreenPreview() {
    ProScreen(onNavigateBack = {})
}
