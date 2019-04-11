package net.feheren_fekete.applist

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import net.feheren_fekete.applist.applistpage.ApplistPageFragment
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.launcher.LauncherFragment
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorFragment
import net.feheren_fekete.applist.launcher.pagepicker.PagePickerFragment
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.WriteSettingsPermissionHelper
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val shortcutHelper: ShortcutHelper by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val appWidgetHost: AppWidgetHost by inject()
    private val settingsUtils: SettingsUtils by inject()
    private val writeSettingsPermissionHelper: WriteSettingsPermissionHelper by inject()

    private var isHomePressed = false
    private var shouldHandleIntent = false

    fun isHomePressed(): Boolean {
        val wasHomePressed = isHomePressed
        isHomePressed = false
        return wasHomePressed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)
        settingsUtils.applyColorTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        showLauncherFragment(-1)
        shouldHandleIntent = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shouldHandleIntent = true
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // This occurs when the Home button is pressed.
            // Be careful! It may not be true in the future or on some devices.
            isHomePressed = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgetHelper.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        if (shouldHandleIntent) {
            shouldHandleIntent = false
            handleIntent(intent)
        }
        if (!writeSettingsPermissionHelper.hasWriteSettingsPermission()) {
            writeSettingsPermissionHelper.requestWriteSettingsPermission(this)
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onBackPressed() {
        // Don't exit on back-press. We are a launcher.
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowPageEditorEvent(event: WidgetPageFragment.ShowPageEditorEvent) {
        showPageEditorFragment()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowPageEditorEvent(event: ApplistPageFragment.ShowPageEditorEvent) {
        showPageEditorFragment()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowPagePickerEvent(event: WidgetHelper.ShowPagePickerEvent) {
        showPagePickerFragment(
                resources.getString(R.string.page_picker_pin_widget_title),
                resources.getString(R.string.page_picker_message),
                event.data)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowPagePickerEvent(event: WidgetPageFragment.ShowPagePickerEvent) {
        showPagePickerFragment(
                resources.getString(R.string.page_picker_move_widget_title),
                resources.getString(R.string.page_picker_message),
                event.data)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageEditorDoneEvent(event: PageEditorFragment.DoneEvent) {
        showLauncherFragment(-1)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageEditorPageTappedEvent(event: PageEditorFragment.PageTappedEvent) {
        if (widgetHelper.handlePagePicked(this, event.pageData, event.requestData)) {
            showLauncherFragment(event.pageData.id)
        }
    }

    private fun handleIntent(intent: Intent?) {
        var handled: Boolean
        if (intent != null) {
            handled = shortcutHelper.handleIntent(this, intent)
            if (!handled) {
                handled = widgetHelper.handleIntent(this, intent)
            }
            if (!handled) {
                if (ACTION_RESTART == intent.action) {
                    val restartIntent = Intent(this, MainActivity::class.java)
                    restartIntent.action = Intent.ACTION_MAIN
                    restartIntent.addCategory(Intent.CATEGORY_HOME)
                    setIntent(restartIntent)
                    recreate()
                }
            }
        }
    }

    private fun showLauncherFragment(activePageId: Long) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container, LauncherFragment.newInstance(activePageId))
                .commit()
    }

    private fun showPageEditorFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container,
                        PageEditorFragment.newInstance(true, false, Bundle()))
                .commit()
    }

    private fun showPagePickerFragment(title: String,
                                       message: String,
                                       data: Bundle) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container,
                        PagePickerFragment.newInstance(title, message, data))
                .commit()
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        val ACTION_RESTART = MainActivity::class.java.canonicalName!! + "ACTION_RESTART"
    }

}
