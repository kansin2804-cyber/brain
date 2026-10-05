package com.shiftsleep.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.shiftsleep.app.data.ShiftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Play Billing wrapper. Safe when Play Store is missing: [available] stays false
 * and the app falls back to local 7-day trial + debug unlock.
 */
class BillingManager(
    context: Context,
    private val repository: ShiftRepository,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _available.value = true
                    scope.launch {
                        queryProducts()
                        refreshPurchases()
                    }
                } else {
                    _available.value = false
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _available.value = false
            }
        })
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun queryProducts() {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProducts.all.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList.orEmpty()
        }
    }

    suspend fun refreshPurchases() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        val active = result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        active.forEach { acknowledgeIfNeeded(it) }
        val productId = active.firstOrNull()?.products?.firstOrNull().orEmpty()
        val prefs = repository.ensurePrefs().copy(
            subscriptionActive = active.isNotEmpty(),
            subscriptionProductId = productId,
        )
        repository.savePrefs(prefs)
    }

    fun launchPurchase(activity: Activity, productId: String): Boolean {
        val details = _products.value.firstOrNull { it.productId == productId }
        if (details == null) {
            _message.value = "상품을 불러오지 못했습니다. Play Console에 구독 상품을 등록했는지 확인하세요."
            return false
        }
        val offer = details.subscriptionOfferDetails?.firstOrNull()
        if (offer == null) {
            _message.value = "구독 오퍼가 없습니다. Play Console base plan을 확인하세요."
            return false
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = client.launchBillingFlow(activity, flow)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    purchases.orEmpty().forEach { acknowledgeIfNeeded(it) }
                    refreshPurchases()
                    _message.value = "구독이 활성화되었습니다."
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _message.value = "구매가 취소되었습니다."
            }
            else -> {
                _message.value = "결제 오류: ${result.debugMessage.ifBlank { result.responseCode.toString() }}"
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params)
    }

    fun destroy() {
        client.endConnection()
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
