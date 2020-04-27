package net.feheren_fekete.applist.iap

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.feheren_fekete.applist.ApplistLog

class IapRepository(
    private val applistLog: ApplistLog,
    private val context: Context,
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context).setListener(this).build()

    private val skuList = ArrayList<SkuDetails>()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    fun getAvailableProducts(): Flow<IapProduct> {
        if (!billingClient.isReady) {
            return emptyFlow()
        }
        return querySkuDetails().map {
            IapProduct(it.sku, it.price, it.iconUrl)
        }
    }

    fun purchaseProduct(activity: Activity, product: IapProduct) {
        val skuDetails = skuList.find {
            it.sku == product.productId
        }
        if (skuDetails == null) {
            applistLog.log(RuntimeException("Invalid product: ${product.productId}"))
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            applistLog.analytics(ApplistLog.IAP_REPOSITORY, ApplistLog.IAP_PURCHASE_CANCELLED)
        } else {
            applistLog.log(RuntimeException(
                "onPurchasesUpdated: Error code: ${billingResult.responseCode}"))
        }
    }

    private fun querySkuDetails(): Flow<SkuDetails> {
        val skuList = ArrayList<String>()
        skuList.add("donut_1")
        skuList.add("donut_2")
        skuList.add("donut_3")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        return flow {
            val skuDetailsResult = billingClient.querySkuDetails(params.build())
            if (skuDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                this@IapRepository.skuList.clear()
                skuDetailsResult.skuDetailsList?.forEach {
                    this@IapRepository.skuList.add(it)
                    emit(it)
                }
            } else {
                applistLog.log(
                    RuntimeException(
                        "querySkuDetails: Error code: ${skuDetailsResult.billingResult.responseCode}"
                    )
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            ...
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                val ackPurchaseResult = withContext(Dispatchers.IO) {
                    client.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            }
        }
    }

}
