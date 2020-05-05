package net.feheren_fekete.applist.iap

import android.app.Activity
import androidx.lifecycle.ViewModel
import org.koin.java.KoinJavaComponent.get

class DonutViewModel: ViewModel() {

    private val repository = get(IapRepository::class.java)

    fun products() = repository.products

    fun purchasedProduct() = repository.purchasedProduct

    fun purchaseProduct(activity: Activity, product: IapProduct) {
        repository.purchaseProduct(activity, product)
    }

}
