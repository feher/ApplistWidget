package net.feheren_fekete.applist.iap

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import net.feheren_fekete.applist.ApplistLog

class IapRepository(
    context: Context,
    private val applistLog: ApplistLog
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context).setListener(this).build()

    private val skuDetailsList = ArrayList<SkuDetails>()
    private val productsLiveData = ProductsLiveData()
    private val purchasedProductLiveData = MutableLiveData<IapProduct>()

    val products: LiveData<List<IapProduct>> = productsLiveData
    val purchasedProduct: LiveData<IapProduct> = purchasedProductLiveData

    inner class ProductsLiveData : MutableLiveData<List<IapProduct>>() {
        override fun onActive() {
            super.onActive()
            queryProducts()
        }
    }

    //
    // From https://developer.android.com/google/play/billing/billing_library_overview
    // <quote>
    // You should call queryPurchases() at least twice in your code:
    // * Call queryPurchases() every time your app launches so that you can restore any
    //   purchases that a user has made since the app last stopped.
    // * Call queryPurchases() in your onResume() method, because a user can make a purchase
    //   when your app is in the background (for example, redeeming a promo code in the Google Play Store app).
    // </quote>
    //
    fun queryAndHandlePurchases() {
        if (!billingClient.isReady) {
            initBillingClient()
            return
        }
        val purchasesResult: Purchase.PurchasesResult =
            billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        for (purchase in purchasesResult.purchasesList) {
            handlePurchase(purchase)
        }
    }

    private fun queryProducts() {
        if (!billingClient.isReady) {
            initBillingClient()
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val products = ArrayList<IapProduct>()
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(listOf("donut_1", "donut_2", "donut_3"))
            params.setType(BillingClient.SkuType.INAPP)
            val skuDetailsResult = billingClient.querySkuDetails(params.build())
            if (skuDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                skuDetailsList.clear()
                skuDetailsResult.skuDetailsList?.forEach {
                    skuDetailsList.add(it)
                    products.add(IapProduct(it.sku, it.price, it.iconUrl))
                }
            } else {
                logIfError("Unhandled error", skuDetailsResult.billingResult)
            }
            productsLiveData.postValue(products)
        }
    }

    fun purchaseProduct(activity: Activity, product: IapProduct) {
        if (!billingClient.isReady) {
            applistLog.log(RuntimeException("Billing client is not ready"))
            return
        }
        val skuDetails = getSkuDetails(product.productId)
        if (skuDetails == null) {
            applistLog.log(RuntimeException("Invalid product: ${product.productId}"))
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        logIfError("Cannot launch billing flow", responseCode)
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
            logIfError("Unhandled error", billingResult)
        }
    }

    private fun initBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAndHandlePurchases()
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                GlobalScope.launch(Dispatchers.IO) {
                    val ackPurchaseResult =
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                    if (ackPurchaseResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        getProduct(purchase.sku)?.let {
                            purchasedProductLiveData.postValue(it)
                        }
                    } else {
                        logIfError("Unhandled purchase ack result", ackPurchaseResult)
                    }
                }
            }
        } else {
            applistLog.log(
                RuntimeException("Unhandled purchase state: ${purchase.purchaseState}")
            )
        }
    }

    private fun getSkuDetails(productId: String) =
        skuDetailsList.find {
            it.sku == productId
        }

    private fun getProduct(productId: String) =
        getSkuDetails(productId)?.let {
            IapProduct(it.sku, it.price, it.iconUrl)
        }

    private fun logIfError(message: String, billingResult: BillingResult) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            applistLog.log(RuntimeException("$message: ${billingResult.responseCode} ${billingResult.debugMessage}"))
        }
    }

}
