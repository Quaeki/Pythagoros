package com.example.pythagoros.data.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

data class BillingProductPrice(
    val productId: String,
    val formattedPrice: String,
)

class PlayBillingClient(
    context: Context,
    private val onEntitlementChanged: (isPro: Boolean, productId: String?) -> Unit,
    private val onPurchaseMessage: (String?) -> Unit = {},
) : PurchasesUpdatedListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val productDetailsById = mutableMapOf<String, ProductDetails>()

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun start(onReady: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (billingClient.isReady) {
            onReady()
            return
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        post { onReady() }
                    } else {
                        post { onError(billingResult.readableMessage("Google Play Billing недоступен")) }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    post { onError("Соединение с Google Play Billing потеряно. Попробуйте ещё раз.") }
                }
            }
        )
    }

    fun queryProductPrices(onResult: (List<BillingProductPrice>) -> Unit) {
        if (!billingClient.isReady) {
            onResult(emptyList())
            return
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                post { onResult(emptyList()) }
                return@queryProductDetailsAsync
            }
            val details = productDetailsResult.productDetailsList
            productDetailsById.clear()
            details.forEach { productDetailsById[it.productId] = it }
            val prices = details.mapNotNull { detail ->
                val price = detail.firstSubscriptionPrice() ?: return@mapNotNull null
                BillingProductPrice(productId = detail.productId, formattedPrice = price)
            }
            post { onResult(prices) }
        }
    }

    fun launchPurchase(activity: Activity, productId: String, onLaunchResult: (String?) -> Unit) {
        val productDetails = productDetailsById[productId]
        if (productDetails == null) {
            onLaunchResult("Тариф ещё не загружен из Google Play. Попробуйте через пару секунд.")
            return
        }
        val offerToken = productDetails.preferredSubscriptionOffer()?.offerToken
        if (offerToken.isNullOrBlank()) {
            onLaunchResult("Для тарифа $productId не найден base plan или offer token в Google Play Console.")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        onLaunchResult(
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                null
            } else {
                result.readableMessage("Не удалось открыть оплату Google Play")
            }
        )
    }

    fun restorePurchases(onResult: (Boolean) -> Unit = {}) {
        if (!billingClient.isReady) {
            onResult(false)
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                post {
                    onPurchaseMessage(billingResult.readableMessage("Не удалось проверить подписку"))
                    onResult(false)
                }
                return@queryPurchasesAsync
            }
            val activeProductId = handlePurchases(purchases)
            post { onResult(activeProductId != null) }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                post { onPurchaseMessage("Покупка отменена") }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
            else -> {
                post { onPurchaseMessage(billingResult.readableMessage("Покупка не завершена")) }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>): String? {
        val activePurchase = purchases.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in ProductIds }
        }
        val productId = activePurchase?.products?.firstOrNull { it in ProductIds }
        if (activePurchase != null) {
            acknowledgeIfNeeded(activePurchase)
            post {
                onPurchaseMessage(null)
                onEntitlementChanged(true, productId)
            }
            return productId
        }

        val pending = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PENDING &&
                purchase.products.any { it in ProductIds }
        }
        post {
            if (pending) {
                onPurchaseMessage("Платёж ожидает подтверждения в Google Play. Pro включится после оплаты.")
            } else {
                onEntitlementChanged(false, null)
            }
        }
        return null
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                post { onPurchaseMessage(result.readableMessage("Не удалось подтвердить покупку")) }
            }
        }
    }

    private fun ProductDetails.firstSubscriptionPrice(): String? =
        preferredSubscriptionOffer()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull { it.priceAmountMicros > 0L }
            ?.formattedPrice

    private fun ProductDetails.preferredSubscriptionOffer(): ProductDetails.SubscriptionOfferDetails? =
        subscriptionOfferDetails
            ?.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.any { phase -> phase.priceAmountMicros == 0L }
            }
            ?: subscriptionOfferDetails?.firstOrNull()

    private fun BillingResult.readableMessage(defaultMessage: String): String =
        debugMessage.takeIf { it.isNotBlank() } ?: defaultMessage

    private fun post(block: () -> Unit) {
        mainHandler.post(block)
    }

    companion object {
        const val ProMonthly = "pro_monthly"
        const val ProMaxMonthly = "pro_max_monthly"

        val ProductIds = listOf(ProMonthly, ProMaxMonthly)
    }
}
