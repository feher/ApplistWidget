package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class IconPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.iconpack_picker_activity)
        showIconPickerFragment()
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
    fun onIconPickerCancelEvent(event: IconPickerFragment.CancelEvent) {
        finish()
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIconPickerDoneEvent(event: IconPickerFragment.DoneEvent) {
        finish()
    }

    private fun showIconPickerFragment() {
        val appItemId =
            intent?.getLongExtra(EXTRA_APPLIST_ITEM_ID, ApplistItemData.INVALID_ID) ?: return
        val appName =
            intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
        val componentName =
            intent?.getParcelableExtra<ComponentName>(EXTRA_APP_COMPONENT_NAME)
        val iconPath =
            intent?.getStringExtra(EXTRA_ICON_PATH)
        val customIconPath =
            intent?.getStringExtra(EXTRA_CUSTOM_ICON_PATH) ?: return
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.iconPickerActivityContainer,
                IconPickerFragment.newInstance(
                    getString(R.string.iconpack_picker_change_icon),
                    appItemId, appName, componentName, iconPath, customIconPath
                )
            )
            .commit()
    }

    companion object {
        const val EXTRA_APPLIST_ITEM_ID = "extra.APPLIST_ITEM_ID"
        const val EXTRA_APP_NAME = "extra.APP_NAME"
        const val EXTRA_APP_COMPONENT_NAME = "extra.APP_COMPONENT_NAME"
        const val EXTRA_ICON_PATH = "extra.ICON_PATH"
        const val EXTRA_CUSTOM_ICON_PATH = "extra.CUSTOM_ICON_PATH"
    }

}
