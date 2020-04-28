package net.feheren_fekete.applist.iap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.donut_fragment.view.*
import net.feheren_fekete.applist.R
import org.greenrobot.eventbus.EventBus

class DonutFragment: Fragment() {

    class DoneEvent

    private lateinit var viewModel: DonutViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.donut_fragment, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.closeButton.setOnClickListener {
            EventBus.getDefault().post(DoneEvent())
        }
        viewModel = ViewModelProvider(this).get(DonutViewModel::class.java)
        viewModel.products().observe(viewLifecycleOwner, Observer { products ->
            if (products == null) {
                return@Observer
            }
            if (products.size >= 3) {
                view.giveOneDonut.text = getString(R.string.donut_page_donut, products[0].price)
                view.giveOneDonut.setOnClickListener { purchaseDonut(products[0]) }
                view.giveTwoDonuts.text = getString(R.string.donut_page_donut, products[1].price)
                view.giveTwoDonuts.setOnClickListener { purchaseDonut(products[1]) }
                view.giveThreeDonuts.text = getString(R.string.donut_page_donut, products[2].price)
                view.giveThreeDonuts.setOnClickListener { purchaseDonut(products[2]) }
            }
        })
        viewModel.purchasedProduct().observe(viewLifecycleOwner, Observer {
            if (it == null) {
                return@Observer
            }
        })
    }

    private fun purchaseDonut(donut: IapProduct) {
        val a = activity ?: return
        viewModel.purchaseProduct(a, donut)
    }

}
