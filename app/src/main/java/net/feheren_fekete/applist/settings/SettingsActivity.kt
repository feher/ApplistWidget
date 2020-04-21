package net.feheren_fekete.applist.settings

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.MainActivity
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.ApplistDialogs
import net.feheren_fekete.applist.launcher.LauncherUtils
import net.feheren_fekete.applist.utils.AppUtils
import net.feheren_fekete.applist.utils.ScreenUtils
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get


class SettingsActivity : PreferenceActivity() {

    private val settingsUtils: SettingsUtils by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)
        settingsUtils.applyColorTheme(this)

        // We don't want the settings screen to be transparent.
        // So, we revert the windowShowWallpaper flag and restore the background color.
        // REF: 2017_06_23_22_41_dont_show_wallpaper_in_settings_screen.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.white)

        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
                .replace(
                        android.R.id.content,
                        SettingsFragment(),
                        SettingsFragment::class.java.name)
                .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        // This is a framework bugfix.
        // https://code.google.com/p/android/issues/detail?id=189121#c5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragment = fragmentManager.findFragmentByTag(
                SettingsFragment::class.java.name)
        fragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val screenUtils = get(ScreenUtils::class.java)
        private val launcherUtils = get(LauncherUtils::class.java)
        private var defaultThemeValue: String? = null
        private var defaultColumnWidthValue: String? = null

        private val colorTheme: String
            get() {
                val colorThemeValue = preferenceScreen.sharedPreferences.getString(PREF_KEY_COLOR_THEME, defaultThemeValue)
                val colorThemeValues = resources.getStringArray(R.array.color_theme_values)
                val colorThemes = resources.getStringArray(R.array.color_themes)
                for (i in colorThemeValues.indices) {
                    if (colorThemeValues[i] == colorThemeValue) {
                        return colorThemes[i]
                    }
                }
                return ""
            }

        private val columnWidth: String
            get() {
                val columnWidthValue = preferenceScreen.sharedPreferences.getString(PREF_KEY_COLUMN_WIDTH, defaultColumnWidthValue)
                val columnWidthValues = resources.getStringArray(R.array.column_width_values)
                val columnWidths = resources.getStringArray(R.array.column_widths)
                for (i in columnWidthValues.indices) {
                    if (columnWidthValues[i] == columnWidthValue) {
                        return columnWidths[i]
                    }
                }
                return ""
            }

        private val showSmsBadge: Boolean
            get() = preferenceScreen.sharedPreferences.getBoolean(
                    PREF_KEY_SHOW_SMS_BADGE, false)

        private val showPhoneBadge: Boolean
            get() = preferenceScreen.sharedPreferences.getBoolean(
                    PREF_KEY_SHOW_PHONE_BADGE, false)

        private val showNotificationBadge: Boolean
            get() = preferenceScreen.sharedPreferences.getBoolean(
                    PREF_KEY_SHOW_NOTIFICATION_BADGE, false)

        private val showNewContentBadge: Boolean
            get() = preferenceScreen.sharedPreferences.getBoolean(
                    PREF_KEY_SHOW_NEW_CONTENT_BADGE, false)

        private val keepAppsSortedAlphabetically: Boolean
            get() = preferenceScreen.sharedPreferences.getBoolean(
                    PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY, false)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            defaultThemeValue = resources.getString(R.string.color_theme_value_default)
            defaultColumnWidthValue = resources.getString(R.string.column_width_value_default)
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
            val topPadding = screenUtils.getStatusBarHeight(activity)
            // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
            val bottomPadding = if (screenUtils.hasNavigationBar(activity)) screenUtils.getNavigationBarHeight(activity) else 0
            view!!.setPadding(0, topPadding, 0, bottomPadding)
        }

        override fun onStart() {
            super.onStart()
            preferenceScreen.sharedPreferences
                    .registerOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()
            var preference = findPreference(PREF_KEY_COLOR_THEME)
            preference.summary = colorTheme

            preference = findPreference(PREF_KEY_COLUMN_WIDTH)
            preference.summary = columnWidth
        }

        override fun onStop() {
            super.onStop()
            preferenceScreen.sharedPreferences
                    .unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY -> handleChangeKeepAppsSorted()
                PREF_KEY_COLOR_THEME -> handleChangeColorTheme()
                PREF_KEY_COLUMN_WIDTH -> handleChangeColumnWidth()
                PREF_KEY_SHOW_SMS_BADGE -> handleChangeShowSmsBadge()
                PREF_KEY_SHOW_PHONE_BADGE -> handleChangeShowPhoneBadge()
                PREF_KEY_SHOW_NOTIFICATION_BADGE -> handleChangeShowNotificationBadge()
                PREF_KEY_SHOW_NEW_CONTENT_BADGE -> handleChangeShowNewContentBagde()
            }
        }

        private fun handleChangeKeepAppsSorted() {
            if (keepAppsSortedAlphabetically) {
                ApplistDialogs.questionDialog(
                        activity,
                        resources.getString(R.string.settings_keep_apps_sorted_alphabetically),
                        resources.getString(R.string.settings_keep_apps_sorted_dialog_message),
                        {
                            ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_KEEP_APPS_SORTED_ON, ApplistLog.SETTINGS)
                        },
                        {
                            val keepAppsSorted = findPreference(
                                    PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY) as SwitchPreference
                            keepAppsSorted.isChecked = false
                        }
                )
            } else {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_KEEP_APPS_SORTED_OFF, ApplistLog.SETTINGS)
            }
        }

        private fun handleChangeColorTheme() {
            ApplistLog.getInstance().analytics(
                    ApplistLog.SETTINGS_COLOR_THEME
                            + preferenceScreen.sharedPreferences.getString(PREF_KEY_COLOR_THEME, defaultThemeValue),
                    ApplistLog.SETTINGS)
            val preference = findPreference(PREF_KEY_COLOR_THEME)
            preference.summary = colorTheme
            restartMainActivity()
        }

        private fun handleChangeColumnWidth() {
            ApplistLog.getInstance().analytics(
                    ApplistLog.SETTINGS_COLUMN_WIDTH
                            + preferenceScreen.sharedPreferences.getString(PREF_KEY_COLUMN_WIDTH, defaultColumnWidthValue),
                    ApplistLog.SETTINGS)
            val preference = findPreference(PREF_KEY_COLUMN_WIDTH)
            preference.summary = columnWidth
            restartMainActivity()
        }

        private fun handleChangeShowSmsBadge() {}

        private fun handleChangeShowNewContentBagde() {
            if (showNewContentBadge) {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_NEW_CONTENT_BADGE_ON, ApplistLog.SETTINGS)
            } else {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_NEW_CONTENT_BADGE_OFF, ApplistLog.SETTINGS)
            }
        }

        private fun handleChangeShowPhoneBadge() {
            if (showPhoneBadge) {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_PHONE_BADGE_ON, ApplistLog.SETTINGS)
                if (ensurePermission(Manifest.permission.READ_PHONE_STATE, PHONE_PERMISSION_REQUEST_CODE)) {
                    ensureDefaultPhoneApp()
                }
            } else {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_PHONE_BADGE_OFF, ApplistLog.SETTINGS)
            }
        }

        private fun handleChangeShowNotificationBadge() {
            if (showNotificationBadge) {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_NOTIFICATION_BADGE_ON, ApplistLog.SETTINGS)
                val alertDialog = AlertDialog.Builder(activity)
                        .setTitle(R.string.settings_show_notification_badge_dialog_title)
                        .setMessage(R.string.settings_show_notification_badge_dialog_message)
                        .setPositiveButton(R.string.settings_open_system_settings) { _, _ ->
                            val intentAction = if (Build.VERSION.SDK_INT >= 22)
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                            else
                                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                            startActivity(Intent(intentAction))
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            val showNotificationBadge = findPreference(PREF_KEY_SHOW_NOTIFICATION_BADGE) as SwitchPreference
                            showNotificationBadge.isChecked = false
                        }
                        .setOnCancelListener {
                            val showNotificationBadge = findPreference(PREF_KEY_SHOW_NOTIFICATION_BADGE) as SwitchPreference
                            showNotificationBadge.isChecked = false
                        }
                        .setCancelable(true)
                        .create()
                alertDialog.show()
            } else {
                ApplistLog.getInstance().analytics(ApplistLog.SETTINGS_SHOW_NOTIFICATION_BADGE_OFF, ApplistLog.SETTINGS)
            }
        }

        private fun ensurePermission(permission: String, requestCode: Int): Boolean {
            val activity = activity ?: return false
            val permissionState = ActivityCompat.checkSelfPermission(activity, permission)
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
                return false
            } else {
                Log.d(TAG, "Already has permission $permission")
                return true
            }
        }

        private fun ensureDefaultPhoneApp(): Boolean {
            val appContext = activity.applicationContext
            if (AppUtils.getPhoneApp(appContext) == null) {
                val showPhoneBadge = findPreference(PREF_KEY_SHOW_PHONE_BADGE) as SwitchPreference
                showPhoneBadge.isChecked = false
                launcherUtils.chooseAppDialog(
                        activity,
                        getString(R.string.settings_show_phone_badge_select_default_app),
                        AppUtils.getAvailableAppsForIntent(activity, AppUtils.getPhoneIntent())
                ) { selectedPhoneApp ->
                    val componentName = ComponentName(
                            selectedPhoneApp.activityInfo.packageName,
                            selectedPhoneApp.activityInfo.name)
                    AppUtils.savePhoneApp(appContext, componentName)
                    showPhoneBadge.isChecked = true
                }
                return false
            } else {
                return true
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int,
                                                permissions: Array<String>,
                                                grantResults: IntArray) {
            when (requestCode) {
                SMS_PERMISSION_REQUEST_CODE -> {
                    if (grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "SMS permission granted")
                    } else {
                        val showSmsBadge = findPreference(PREF_KEY_SHOW_SMS_BADGE) as SwitchPreference
                        showSmsBadge.isChecked = false
                    }
                }
                PHONE_PERMISSION_REQUEST_CODE -> {
                    val showPhoneBadge = findPreference(PREF_KEY_SHOW_PHONE_BADGE) as SwitchPreference
                    if (grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Phone permission granted")
                        if (!ensureDefaultPhoneApp()) {
                            showPhoneBadge.isChecked = false
                        }
                    } else {
                        showPhoneBadge.isChecked = false
                    }
                }
            }
        }

        // REF: 2017_06_22_22_36_launchMode_needed_for_restart
        private fun restartMainActivity() {
            val i = Intent(activity, MainActivity::class.java)
            i.action = MainActivity.ACTION_RESTART
            startActivity(i)
            activity.finish()
        }

        companion object {
            private const val SMS_PERMISSION_REQUEST_CODE = 1
            private const val PHONE_PERMISSION_REQUEST_CODE = 2
        }

    }

    companion object {
        private val TAG = SettingsActivity::class.java.simpleName

        const val PREF_KEY_COLOR_THEME = "pref_key_color_theme"
        const val PREF_KEY_COLUMN_WIDTH = "pref_key_column_width"
        const val PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY = "pref_key_keep_apps_sorted_alphabetically"
        const val PREF_KEY_SHOW_NEW_CONTENT_BADGE = "pref_key_show_new_content_badge"
        const val PREF_KEY_SHOW_SMS_BADGE = "pref_key_show_sms_badge"
        const val PREF_KEY_SHOW_PHONE_BADGE = "pref_key_show_phone_badge"
        const val PREF_KEY_SHOW_NOTIFICATION_BADGE = "pref_key_show_notification_badge"
    }

}
