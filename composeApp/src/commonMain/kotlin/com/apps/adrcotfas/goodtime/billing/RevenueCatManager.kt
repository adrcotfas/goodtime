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
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.floor

const val DEFAULT_PRO_ENTITLEMENT_ID: String = "PREMIUM"

/**
 * Call as early as possible (Android: right after `super.onCreate()`, iOS: in `App.init()`).
 *
 * This is a thin wrapper around `Purchases.configure(...)` so that platform code doesn't need to
 * depend on RevenueCat directly (and to keep configuration idempotent).
 */
fun configureRevenueCat(apiKey: String?) {
    val key = apiKey?.takeIf { it.isNotBlank() } ?: return
    if (Purchases.isConfigured) return
    Purchases.configure(apiKey = key)
}

data class RevenueCatConfig(
    val apiKey: String? = null,
    val proEntitlementId: String = DEFAULT_PRO_ENTITLEMENT_ID,
)

class RevenueCatManager(
    private val config: RevenueCatConfig,
    private val settingsRepository: SettingsRepository,
    private val dataRepository: LocalDataRepository,
    private val ioScope: CoroutineScope,
    private val log: Logger,
) : PurchasesDelegate {
    private var started = false

    fun start() {
        if (started) return
        started = true

        if (!Purchases.isConfigured) {
            val apiKey = requireNotNull(config.apiKey) { "RevenueCat enabled but not configured and apiKey is null" }
            log.i { "Configuring RevenueCat" }
            Purchases.configure(apiKey = apiKey)
        }

        Purchases.sharedInstance.delegate = this

        // Reconcile on app start (e.g. refund happened while app wasn't running).
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
        val hasPro = customerInfo.entitlements.active.containsKey(config.proEntitlementId)
        log.i { "handleCustomerInfo: hasPro=$hasPro" }

        ioScope.launch {
            val wasPro = settingsRepository.settings.first().isPro
            if (wasPro && !hasPro) {
                log.i { "Pro entitlement revoked; resetting Pro-only settings" }
                resetPreferencesOnProRevoked()
            }
            settingsRepository.setPro(hasPro)
        }
    }

    private suspend fun resetPreferencesOnProRevoked() {
        resetTimerStyle()
        with(settingsRepository) {
            updateUiSettings {
                it.copy(fullscreenMode = false, screensaverMode = false)
            }
            setEnableTorch(false)
            setEnableFlashScreen(false)
            setInsistentNotification(false)
            activateDefaultLabel()
        }
        dataRepository.archiveAllButDefault()
    }

    private suspend fun resetTimerStyle() {
        val oldTimerStyle = settingsRepository.settings.first().timerStyle
        val newTimerStyle =
            TimerStyleData(
                minSize = oldTimerStyle.minSize,
                maxSize = oldTimerStyle.maxSize,
                fontSize = floor(oldTimerStyle.maxSize * 0.9f),
                currentScreenWidth = oldTimerStyle.currentScreenWidth,
            )
        settingsRepository.updateTimerStyle { newTimerStyle }
    }
}
