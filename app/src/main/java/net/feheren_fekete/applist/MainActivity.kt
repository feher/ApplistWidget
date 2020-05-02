package net.feheren_fekete.applist

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.launcher.LauncherFragment
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.ProgressDialog
import net.feheren_fekete.applist.utils.WriteSettingsPermissionHelper
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity() {

    private val applistLog: ApplistLog by inject()
    private val shortcutHelper: ShortcutHelper by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val appWidgetHost: AppWidgetHost by inject()
    private val settingsUtils: SettingsUtils by inject()
    private val writeSettingsPermissionHelper: WriteSettingsPermissionHelper by inject()

    private var isHomePressed = false
    private var shouldHandleIntent = false
    private val handler = Handler()

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
        if (shouldHandleIntent) {
            shouldHandleIntent = false
            handleIntent(intent)
        }
        if (!writeSettingsPermissionHelper.hasWriteSettingsPermission()) {
            writeSettingsPermissionHelper.requestWriteSettingsPermission(this)
        }
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onBackPressed() {
        // Don't exit on back-press. We are a launcher.
    }

    private fun handleIntent(intent: Intent?) {
        var handled: Boolean
        if (intent != null) {
            handled = shortcutHelper.handleIntent(this, intent)
            if (!handled) {
                handled = widgetHelper.handleIntent(this, intent)
            }
            if (!handled) {
                // TODO: Move this "restart" code to the settings activity.
                if (ACTION_RESTART == intent.action) {
                    val progressDialog = ProgressDialog(R.string.settings_restarting_app)
                    progressDialog.show(supportFragmentManager, "ProgressDialog")
                    handler.postDelayed(::restartApp, 1000)
                }
            }
        }
    }

    private fun restartApp() {
        val packageManager = getPackageManager()
        val intent = packageManager.getLaunchIntentForPackage(getPackageName())
        if (intent == null) {
            applistLog.log(RuntimeException("Cannot restart app: null intent"))
            return
        }
        val componentName = intent.component
        if (componentName == null) {
            applistLog.log(RuntimeException("Cannot restart app: null component name"))
            return
        }
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        try {
            startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        } catch (e: Exception) {
            applistLog.log(RuntimeException("Cannot restart app", e))
        }
    }

    private fun showLauncherFragment(activePageId: Long) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.main_activity_fragment_container,
                LauncherFragment.newInstance(activePageId)
            )
            .commit()
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        val ACTION_RESTART = MainActivity::class.java.canonicalName!! + "ACTION_RESTART"
    }

}
