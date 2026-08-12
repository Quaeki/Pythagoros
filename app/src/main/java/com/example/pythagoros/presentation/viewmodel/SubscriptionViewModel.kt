package com.example.pythagoros.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pythagoros.data.billing.BillingProductPrice
import com.example.pythagoros.data.billing.PlayBillingClient
import com.example.pythagoros.data.prefs.AppPreferences
import com.example.pythagoros.presentation.screens.DefaultPlans
import com.example.pythagoros.presentation.screens.SubscriptionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    var isPro by mutableStateOf(prefs.isPro)
        private set

    var plans by mutableStateOf(DefaultPlans)
        private set

    var purchaseLoading by mutableStateOf(false)
        private set

    var purchaseError by mutableStateOf<String?>(null)
        private set

    fun updatePro(active: Boolean) {
        isPro = active
        prefs.isPro = active
    }

    fun setPlansFromBilling(prices: List<BillingProductPrice>) {
        plans = DefaultPlans.withBillingPrices(prices)
    }

    fun updatePurchaseLoading(loading: Boolean) {
        purchaseLoading = loading
    }

    fun updatePurchaseError(message: String?) {
        purchaseError = message
    }
}

private fun List<SubscriptionPlan>.withBillingPrices(prices: List<BillingProductPrice>): List<SubscriptionPlan> {
    val pricesById = prices.associateBy { it.productId }
    return map { plan ->
        val billingPrice = pricesById[plan.id]?.formattedPrice ?: return@map plan
        val limit = when (plan.id) {
            PlayBillingClient.ProMaxMonthly -> "400 AI-задач"
            else -> "150 AI-задач"
        }
        plan.copy(
            price = "$billingPrice в месяц · $limit",
            renewalText = "Потом $billingPrice в месяц. $limit обновляются каждый месяц.",
        )
    }
}
