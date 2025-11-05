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

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.apps.adrcotfas.goodtime.BuildConfig
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.floor

class GoogleBilling(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) : BillingAbstract,
    PurchasesUpdatedListener,
    ProductDetailsResponseListener {
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails

    private val purchases = MutableStateFlow<List<Purchase>?>(null)
    private val _hasPro = MutableStateFlow<Boolean?>(null)
    val hasPro: StateFlow<Boolean?> = _hasPro.asStateFlow()

    private val _purchasePending = MutableStateFlow(false)
    val purchasePending = _purchasePending.asStateFlow()

    private val isPurchaseAcknowledged = MutableStateFlow(value = false)

    private val billingClient =
        BillingClient
            .newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()

    val isPro = settingsRepository.settings.map { it.isPro }.distinctUntilChanged()

    /**
     * Use the Google Play Billing Library to make a purchase.
     *
     * @param activity [Activity] instance.
     */
    fun buy(activity: Activity) {
        log.i { "buy: attempt to upgrade to PRO" }
        productDetails.value?.let {
            launchBillingFlow(
                activity,
                billingFlowParamsBuilder(productDetails = it).build(),
            )
        } ?: log.e("buy: Invalid product details")
    }

    override fun init() {
        log.i("Initializing BillingClient")

        coroutineScope.launch {
            purchases.collect { purchaseList ->
                _hasPro.value = purchaseList?.any { it.products.contains(PRO_VERSION) }
            }
        }

        coroutineScope.launch {
            /**
             * [isPro] - whether the user has the pro version as stored in persistence
             * [hasPro] - whether the billing client states that the user has the pro version
             * [acknowledged] - whether the purchase has been acknowledged
             */
            data class ProState(
                val isPro: Boolean,
                val hasPro: Boolean?,
                val acknowledged: Boolean,
            )
            combine(
                settingsRepository.settings.distinctUntilChanged().map { it.isPro },
                hasPro,
                isPurchaseAcknowledged,
            ) { isPro, hasPro, acknowledged ->
                ProState(isPro, hasPro, acknowledged)
            }.distinctUntilChanged().collect {
                log.i("ProState: $it")
                if (it.isPro && it.hasPro == false) {
                    log.i("Purchase was refunded")
                    setPro(false)
                    resetPreferencesOnRefund()
                } else if (!it.isPro) {
                    if (it.hasPro == true) {
                        log.i("Purchase was confirmed (or restored)")
                        setPro(true)
                    } else if (it.acknowledged && it.hasPro != false) {
                        log.i("Purchase was confirmed")
                        setPro(true)
                    }
                }
            }
        }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        log.i("Billing response OK")
                        queryPurchases()
                        queryProductDetails()
                    } else {
                        log.e(billingResult.debugMessage)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    log.i("Billing connection disconnected")
                    retryBillingServiceConnection()
                }
            },
        )
    }

    override fun terminate() {
        log.i("Terminating connection")
        billingClient.endConnection()
    }

    private fun retryBillingServiceConnection() {
        val maxTries = 3
        var tries = 1
        var isConnectionEstablished = false
        do {
            try {
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingServiceDisconnected() {}

                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (billingResult.responseCode == BillingResponseCode.OK) {
                                queryPurchases()
                                queryProductDetails()
                                isConnectionEstablished = true
                                log.i("Billing connection retry succeeded.")
                            } else {
                                log.e(
                                    "Billing connection retry failed: ${billingResult.debugMessage}",
                                )
                            }
                        }
                    },
                )
            } catch (e: Exception) {
                e.message?.let { log.e(it) }
            } finally {
                tries++
            }
        } while (tries <= maxTries && !isConnectionEstablished)
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            log.e("queryPurchases: BillingClient is not ready")
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                log.i("onQueryPurchasesResponse: ${purchaseList.map { it.toString() }}")
                purchases.update { purchaseList }
                purchaseList.forEach {
                    if (it.isAcknowledged) {
                        log.w("Item already acknowledged")
                        isPurchaseAcknowledged.update { true }
                    } else {
                        acknowledge(it.purchaseToken)
                    }
                }
            } else {
                log.e(billingResult.debugMessage)
            }
        }
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        productList.add(
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(PRO_VERSION)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        params.setProductList(productList).let { productDetailsParams ->
            billingClient.queryProductDetailsAsync(productDetailsParams.build(), this)
        }
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        result: QueryProductDetailsResult,
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        val productDetailsList = result.productDetailsList
        log.i(
            "onProductDetailsResponse: responseCode: $responseCode, debugMessage: $debugMessage " +
                "productDetails: ${productDetailsList.map { it.toString() }}",
        )

        when (responseCode) {
            BillingResponseCode.OK -> {
                if (productDetailsList.isEmpty()) {
                    log.e(
                        "onProductDetailsResponse: " +
                            "Found null or empty ProductDetails. " +
                            "Check to see if the Products you requested are correctly " +
                            "published in the Google Play Console.",
                    )
                } else {
                    _productDetails.update {
                        productDetailsList.firstOrNull { it.productId == PRO_VERSION }
                    }
                }
            }

            else -> {
                log.e("onProductDetailsResponse: $responseCode $debugMessage")
            }
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ) {
        log.i("launchBillingFlow...")
        if (!billingClient.isReady) {
            log.e("launchBillingFlow: BillingClient is not ready")
        }
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        log.i(
            "onPurchasesUpdated: responseCode: $responseCode, debugMessage: $debugMessage purchases: ${purchases?.map { it.toString() }}",
        )
        if (responseCode == BillingResponseCode.OK &&
            !purchases.isNullOrEmpty()
        ) {
            this.purchases.update { purchases }
            coroutineScope.launch {
                purchases.forEach {
                    if (it.isAcknowledged) {
                        log.i("Item already acknowledged")
                        isPurchaseAcknowledged.update { true }
                    } else {
                        acknowledgePurchase(it.purchaseToken)
                    }
                }
            }
            _purchasePending.update { true }
        } else {
            log.e("onPurchasesUpdated: $responseCode $debugMessage")
        }
    }

    private fun acknowledge(purchaseToken: String): BillingResult {
        log.i("Acknowledge: $purchaseToken")
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        var ackResult = BillingResult()
        billingClient.acknowledgePurchase(params) { billingResult ->
            ackResult = billingResult
        }
        return ackResult
    }

    private suspend fun acknowledgePurchase(purchaseToken: String) {
        val retryDelayMs = 2000L
        val retryFactor = 2
        val maxTries = 3

        withContext(Dispatchers.IO) {
            acknowledge(purchaseToken)
        }

        AcknowledgePurchaseResponseListener { acknowledgePurchaseResult ->
            when (acknowledgePurchaseResult.responseCode) {
                BillingResponseCode.OK -> {
                    log.i("Acknowledgement was successful")
                    isPurchaseAcknowledged.update { true }
                }

                BillingResponseCode.ITEM_NOT_OWNED -> {
                    // This is possibly related to a stale Play cache.
                    // Querying purchases again.
                    log.i("Acknowledgement failed with ITEM_NOT_OWNED")
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams
                            .newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    ) { billingResult, purchaseList ->
                        when (billingResult.responseCode) {
                            BillingResponseCode.OK -> {
                                purchaseList.forEach { purchase ->
                                    acknowledge(purchase.purchaseToken)
                                }
                            }
                        }
                    }
                }

                in
                setOf(
                    BillingResponseCode.ERROR,
                    BillingResponseCode.SERVICE_DISCONNECTED,
                    BillingResponseCode.SERVICE_UNAVAILABLE,
                ),
                -> {
                    log.w(
                        """
                        Acknowledgement failed, but can be retried --
                        Response Code: ${acknowledgePurchaseResult.responseCode} --
                        Debug Message: ${acknowledgePurchaseResult.debugMessage}
                        """.trimIndent(),
                    )
                    runBlocking {
                        exponentialRetry(
                            maxTries = maxTries,
                            initialDelay = retryDelayMs,
                            retryFactor = retryFactor,
                        ) { acknowledge(purchaseToken) }
                    }
                }

                in
                setOf(
                    BillingResponseCode.BILLING_UNAVAILABLE,
                    BillingResponseCode.DEVELOPER_ERROR,
                    BillingResponseCode.FEATURE_NOT_SUPPORTED,
                ),
                -> {
                    log.e(
                        """
                        Acknowledgement failed and cannot be retried --
                        Response Code: ${acknowledgePurchaseResult.responseCode} --
                        Debug Message: ${acknowledgePurchaseResult.debugMessage}
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    private suspend fun <T> exponentialRetry(
        maxTries: Int = Int.MAX_VALUE,
        initialDelay: Long = Long.MAX_VALUE,
        retryFactor: Int = Int.MAX_VALUE,
        block: suspend () -> T,
    ): T? {
        var currentDelay = initialDelay
        var retryAttempt = 1
        do {
            runCatching {
                delay(currentDelay)
                block()
            }.onSuccess {
                log.i("Retry succeeded")
                return@onSuccess
            }.onFailure { throwable ->
                log.e(
                    "Retry Failed -- Cause: ${throwable.cause} -- Message: ${throwable.message}",
                )
            }
            currentDelay *= retryFactor
            retryAttempt++
        } while (retryAttempt < maxTries)

        return block() // last attempt
    }

    private fun billingFlowParamsBuilder(productDetails: ProductDetails) =
        BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
                    .build(),
            ),
        )

    /**
     * Persist the pro status.
     */
    private suspend fun setPro(value: Boolean) {
        settingsRepository.setPro(value)
    }

    /**
     * Reset the state of the app when a refund is made.
     */
    private suspend fun resetPreferencesOnRefund() {
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

    companion object {
        val PRO_VERSION = if (BuildConfig.DEBUG) "test_item" else "upgraded_version"
    }
}
