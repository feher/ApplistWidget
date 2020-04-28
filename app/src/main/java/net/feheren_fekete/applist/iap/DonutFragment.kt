package net.feheren_fekete.applist.iap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.donut_fragment.view.*
import net.feheren_fekete.applist.R
import org.greenrobot.eventbus.EventBus

class DonutFragment: Fragment() {

    class DoneEvent

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
    }

}
