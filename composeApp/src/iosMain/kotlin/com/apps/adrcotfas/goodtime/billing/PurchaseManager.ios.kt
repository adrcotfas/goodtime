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

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.backup.CloudBackupManager
import com.apps.adrcotfas.goodtime.backup.cancelCloudBackupTask
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class PurchaseManager actual constructor(
    private val settingsRepository: SettingsRepository,
    dataRepository: LocalDataRepository,
    ioScope: CoroutineScope,
    private val log: Logger,
) : PurchasesDelegate,
    KoinComponent {
    private val cloudBackupManager: CloudBackupManager by inject()

    private val proSync =
        ProStateSynchronizer(
            settingsRepository,
            dataRepository,
            ioScope,
            log,
            onProRevoked = { cancelCloudBackupTask() },
            onProGranted = { cloudBackupManager.autoEnableCloudAutoBackupIfEligible() },
        )
    private var started = false

    actual fun start() {
        if (started) return
        started = true

        if (!Purchases.isConfigured) {
            log.i { "Configuring purchases (RevenueCat)" }
            configurePurchasesFromPlatform()
            check(Purchases.isConfigured) { "Purchases not configured; missing RevenueCat API key?" }
        }

        Purchases.sharedInstance.delegate = this

        Purchases.sharedInstance.getCustomerInfo(
            fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
            onError = { error ->
                log.w { "Failed to fetch customer info: ${error.message}" }
            },
            onSuccess = { handleCustomerInfo(it) },
        )
    }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        handleCustomerInfo(customerInfo)
    }

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
        ) -> Unit,
    ) {
        // no promos for now
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val hasPro = customerInfo.entitlements.active.containsKey(DEFAULT_PRO_ENTITLEMENT_ID)
        log.i { "handleCustomerInfo: hasPro=$hasPro" }
        proSync.onHasProChanged(hasPro)
    }
}
