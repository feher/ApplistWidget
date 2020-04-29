package net.feheren_fekete.applist.launcher.pageeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.feheren_fekete.applist.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PageEditorActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher_page_editor_activity)
        showPageEditorFragment()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageEditorDoneEvent(event: PageEditorFragment.DoneEvent) {
        finish()
    }

    private fun showPageEditorFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.page_editor_activity_fragment_container,
                PageEditorFragment.newInstance(true, false, Bundle()))
            .commit()
    }

}
