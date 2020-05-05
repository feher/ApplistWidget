package net.feheren_fekete.applist.launcher.pagepicker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorFragment
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.get

class PagePickerActivity: AppCompatActivity() {

    private val widgetHelper = get<WidgetHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_picker_activity)
        showPagePickerFragment()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageEditorPageTappedEvent(event: PageEditorFragment.PageTappedEvent) {
        if (widgetHelper.handlePagePicked(this, event.pageData, event.requestData)) {
            finish()
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageEditorDoneEvent(event: PageEditorFragment.DoneEvent) {
        finish()
    }

    private fun showPagePickerFragment() {
        val title = intent?.extras?.getString(EXTRA_TITLE, "") ?: ""
        val message = intent?.extras?.getString(EXTRA_MESSAGE, "") ?: ""
        val requestData = intent?.extras?.getBundle(EXTRA_REQUEST_DATA) ?: Bundle()
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.page_picker_activity_fragment_container,
                PagePickerFragment.newInstance(title, message, requestData))
            .commit()
    }

    companion object {
        const val EXTRA_TITLE = "extra.TITLE"
        const val EXTRA_MESSAGE = "extra.MESSAGE"
        const val EXTRA_REQUEST_DATA = "extra.REQUEST_DATA"
    }

}
