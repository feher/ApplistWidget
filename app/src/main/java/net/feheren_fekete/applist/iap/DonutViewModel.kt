package net.feheren_fekete.applist.iap

import android.app.Activity
import androidx.lifecycle.ViewModel
import org.koin.java.KoinJavaComponent.inject

class DonutViewModel: ViewModel() {

    private val repository: IapRepository by inject(IapRepository::class.java)

    fun products() = repository.products

    fun purchasedProduct() = repository.purchasedProduct

    fun purchaseProduct(activity: Activity, product: IapProduct) {
        repository.purchaseProduct(activity, product)
    }

}
