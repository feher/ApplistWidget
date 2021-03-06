package net.feheren_fekete.applist.iap

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.donut_activity.*
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ScreenUtils
import org.koin.android.ext.android.get

class DonutActivity: AppCompatActivity() {

    private val screenUtils = get<ScreenUtils>()

    private lateinit var viewModel: DonutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.donut_activity)

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        val topPadding: Int = screenUtils.getStatusBarHeight(this)
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        val bottomPadding =
            if (screenUtils.hasNavigationBar(this)) {
                screenUtils.getNavigationBarHeight(this)
            } else 0
        donutActivityRoot.setPadding(0, topPadding, 0, bottomPadding)

        progressBar.visibility = View.VISIBLE
        giveOneDonut.visibility = View.INVISIBLE
        giveTwoDonuts.visibility = View.INVISIBLE
        giveThreeDonuts.visibility = View.INVISIBLE

        viewModel = ViewModelProvider(this).get(DonutViewModel::class.java)
        viewModel.products().observe(this, Observer { products ->
            if (products == null) {
                return@Observer
            }
            if (products.size >= 3) {
                progressBar.visibility = View.GONE
                giveOneDonut.visibility = View.VISIBLE
                giveOneDonut.text = getString(R.string.donut_page_donut, products[0].price)
                giveOneDonut.setOnClickListener { purchaseDonut(products[0]) }
                giveTwoDonuts.visibility = View.VISIBLE
                giveTwoDonuts.text = getString(R.string.donut_page_donut, products[1].price)
                giveTwoDonuts.setOnClickListener { purchaseDonut(products[1]) }
                giveThreeDonuts.visibility = View.VISIBLE
                giveThreeDonuts.text = getString(R.string.donut_page_donut, products[2].price)
                giveThreeDonuts.setOnClickListener { purchaseDonut(products[2]) }
            }
        })
        viewModel.purchasedProduct().observe(this, Observer {
            if (it == null) {
                return@Observer
            }
        })
    }

    private fun purchaseDonut(donut: IapProduct) {
        viewModel.purchaseProduct(this, donut)
    }

}
