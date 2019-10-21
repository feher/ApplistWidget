package net.feheren_fekete.applist

import android.content.Context

class ApplistPreferences(context: Context) {

    private val sharedPreferences =
            context.applicationContext.getSharedPreferences(APPLIST_PREFERENCES, Context.MODE_PRIVATE)

    var versionCode: Int
        get() = sharedPreferences.getInt(PREFERENCE_VERSION_CODE, DEFAULT_VERSION_CODE)
        set(versionCode) = sharedPreferences.edit().putInt(PREFERENCE_VERSION_CODE, versionCode).apply()

    var showWhatsNew: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_SHOW_WHATS_NEW, DEFAULT_SHOW_WHATS_NEW)
        set(show) = sharedPreferences.edit().putBoolean(PREFERENCE_SHOW_WHATS_NEW, show).apply()

    var showRearrangeItemsHelp: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_SHOW_REARRANGE_ITEMS_HELP, DEFAULT_SHOW_REARRANGE_ITEMS_HELP)
        set(show) = sharedPreferences.edit().putBoolean(PREFERENCE_SHOW_REARRANGE_ITEMS_HELP, show).apply()

    var showReorderAppsHelp: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_SHOW_REORDER_APPS_HELP, DEFAULT_SHOW_REORDER_APPS_HELP)
        set(show) = sharedPreferences.edit().putBoolean(PREFERENCE_SHOW_REORDER_APPS_HELP, show).apply()

    var showUseLauncherTip: Boolean
        get() = sharedPreferences.getBoolean(PREFERENCE_SHOW_USE_LAUNCHER_TIP, DEFAULT_SHOW_USE_LAUNCHER_TIP)
        set(show) = sharedPreferences.edit().putBoolean(PREFERENCE_SHOW_USE_LAUNCHER_TIP, show).apply()

    var deviceLocale: String
        get() = sharedPreferences.getString(PREFERENCE_DEVICE_LOCALE, DEFAULT_DEVICE_LOCALE)!!
        set(deviceLocale) = sharedPreferences.edit().putString(PREFERENCE_DEVICE_LOCALE, deviceLocale).apply()

    var lastActiveLauncherPagePosition: Int
        get() = sharedPreferences.getInt(PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION, DEFAULT_LAST_ACTIVE_LAUNCHER_PAGE_POSITION)
        set(pagePosition) = sharedPreferences.edit().putInt(PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION, pagePosition).apply()

    companion object {
        private const val APPLIST_PREFERENCES = "APPLIST_PREFERENCES"

        private const val PREFERENCE_VERSION_CODE = "VERSION_CODE"
        private const val DEFAULT_VERSION_CODE = 0

        private const val PREFERENCE_SHOW_WHATS_NEW = "PREFERENCE_SHOW_WHATS_NEW"
        private const val DEFAULT_SHOW_WHATS_NEW = true

        private const val PREFERENCE_SHOW_REARRANGE_ITEMS_HELP = "PREFERENCE_SHOW_REARRANGE_ITEMS_HELP"
        private const val DEFAULT_SHOW_REARRANGE_ITEMS_HELP = true

        private const val PREFERENCE_SHOW_REORDER_APPS_HELP = "PREFERENCE_SHOW_REORDER_APPS_HELP"
        private const val DEFAULT_SHOW_REORDER_APPS_HELP = true

        private const val PREFERENCE_SHOW_USE_LAUNCHER_TIP = "PREFERENCE_SHOW_USE_LAUNCHER_TIP"
        private const val DEFAULT_SHOW_USE_LAUNCHER_TIP = true

        private const val PREFERENCE_DEVICE_LOCALE = "DEVICE_LOCALE"
        private const val DEFAULT_DEVICE_LOCALE = ""

        private const val PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION = "LAST_ACTIVE_LAUNCHER_PAGE_POSITION"
        private const val DEFAULT_LAST_ACTIVE_LAUNCHER_PAGE_POSITION = -1
    }

}
