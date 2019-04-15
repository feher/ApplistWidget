package net.feheren_fekete.applist.applistpage.shortcutbadge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.settings.SettingsUtils

import org.koin.java.KoinJavaComponent.inject

class BadgeReceiver : BroadcastReceiver() {

    private val settingsUtils: SettingsUtils by inject(SettingsUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        // Note: intent.toUri(0) may crash with "Duplicate key in ArrayMap: badge_count"
        // Log.d(TAG, "RECEIVED: " + intent.toUri(0));

        if (!settingsUtils.showNewContentBadge) {
            return
        }

        try {
            when (intent.action) {
                DEFAULT_ACTION_UPDATE_BADGE -> handleDefault(intent)
                SONY_ACTION_UPDATE_BADGE -> handleSony(intent)
                ADW_ACTION_UPDATE_BADGE -> handleAdw(intent)
                APEX_ACTION_UPDATE_BADGE -> handleApex(intent)
                HTC_1_ACTION_UPDATE_BADGE -> handleHtc1(intent)
                HTC_2_ACTION_UPDATE_BADGE -> handleHtc2(intent)
            }
        } catch (e: IllegalArgumentException) {
            ApplistLog.getInstance().log(e)
        }

    }

    private fun handleDefault(intent: Intent) {
        val packageName = intent.getStringExtra(DEFAULT_EXTRA_PACKAGE_NAME)
        val className = intent.getStringExtra(DEFAULT_EXTRA_CLASS_NAME)
        if (packageName != null && className != null) {
            var badgeCount = intent.getIntExtra(DEFAULT_EXTRA_BADGE_COUNT, 0)
            if (badgeCount < 0) {
                badgeCount = 0
            }
            setBadgeCount(packageName, className, badgeCount)
        }
    }

    private fun handleSony(intent: Intent) {
        val packageName = intent.getStringExtra(SONY_EXTRA_PACKAGE_NAME)
        val className = intent.getStringExtra(SONY_EXTRA_CLASS_NAME)
        val badgeCountString = intent.getStringExtra(SONY_EXTRA_BADGE_COUNT)
        val showMessage = intent.getBooleanExtra(SONY_EXTRA_SHOW_MESSAGE, false)
        if (packageName != null && className != null) {
            var badgeCount = 0
            if (badgeCountString != null) {
                try {
                    badgeCount = Integer.parseInt(badgeCountString)
                } catch (e: NumberFormatException) {
                    ApplistLog.getInstance().log(RuntimeException("Bad badge count format: $badgeCountString", e))
                }
            }
            if (badgeCount < 0) {
                badgeCount = 0
            }
            if (!showMessage) {
                badgeCount = 0
            }
            setBadgeCount(packageName, className, badgeCount)
        }
    }

    private fun handleAdw(intent: Intent) {
        val packageName = intent.getStringExtra(ADW_EXTRA_PACKAGE_NAME)
        val className = intent.getStringExtra(ADW_EXTRA_CLASS_NAME)
        var badgeCount = intent.getIntExtra(ADW_EXTRA_BADGE_COUNT, 0)
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0
            }
            setBadgeCount(packageName, className, badgeCount)
        }
    }

    private fun handleApex(intent: Intent) {
        val packageName = intent.getStringExtra(APEX_EXTRA_PACKAGE_NAME)
        val className = intent.getStringExtra(APEX_EXTRA_CLASS_NAME)
        var badgeCount = intent.getIntExtra(APEX_EXTRA_BADGE_COUNT, 0)
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0
            }
            setBadgeCount(packageName, className, badgeCount)
        }
    }

    private fun handleHtc1(intent: Intent) {
        val componentNameString = intent.getStringExtra(HTC_1_EXTRA_COMPONENT)
        var badgeCount = intent.getIntExtra(HTC_1_EXTRA_BADGE_COUNT, 0)
        if (componentNameString != null) {
            val componentName = ComponentName.unflattenFromString(componentNameString)
            val packageName = componentName!!.packageName
            val className = componentName.className
            if (badgeCount < 0) {
                badgeCount = 0
            }
            setBadgeCount(packageName, className, badgeCount)
        }
    }

    private fun handleHtc2(intent: Intent) {
        val packageName = intent.getStringExtra(HTC_2_EXTRA_PACKAGE_NAME)
        var badgeCount = intent.getIntExtra(HTC_2_EXTRA_BADGE_COUNT, 0)
        if (packageName != null) {
            if (badgeCount < 0) {
                badgeCount = 0
            }
            setBadgeCount(packageName, "", badgeCount)
        }
    }

    private fun setBadgeCount(packageName: String,
                              className: String,
                              badgeCount: Int) {
        badgeStore.setBadgeCount(packageName, className, badgeCount)
    }

    companion object {

        private val TAG = BadgeReceiver::class.java.simpleName

        private const val DEFAULT_ACTION_UPDATE_BADGE = "android.intent.action.BADGE_COUNT_UPDATE"
        private const val DEFAULT_EXTRA_BADGE_COUNT = "badge_count"
        private const val DEFAULT_EXTRA_PACKAGE_NAME = "badge_count_package_name"
        private const val DEFAULT_EXTRA_CLASS_NAME = "badge_count_class_name"

        private const val SONY_ACTION_UPDATE_BADGE = "com.sonyericsson.home.action.UPDATE_BADGE"
        private const val SONY_EXTRA_PACKAGE_NAME = "com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME"
        private const val SONY_EXTRA_CLASS_NAME = "com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME"
        private const val SONY_EXTRA_BADGE_COUNT = "com.sonyericsson.home.intent.extra.badge.MESSAGE"
        private const val SONY_EXTRA_SHOW_MESSAGE = "com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE"

        const val HTC_1_ACTION_UPDATE_BADGE = "com.htc.launcher.action.SET_NOTIFICATION"
        const val HTC_1_EXTRA_COMPONENT = "com.htc.launcher.extra.COMPONENT"
        const val HTC_1_EXTRA_BADGE_COUNT = "com.htc.launcher.extra.COUNT"

        const val HTC_2_ACTION_UPDATE_BADGE = "com.htc.launcher.action.UPDATE_SHORTCUT"
        const val HTC_2_EXTRA_PACKAGE_NAME = "packagename"
        const val HTC_2_EXTRA_BADGE_COUNT = "count"

        const val ADW_ACTION_UPDATE_BADGE = "org.adw.launcher.counter.SEND"
        const val ADW_EXTRA_PACKAGE_NAME = "PNAME"
        const val ADW_EXTRA_CLASS_NAME = "CNAME"
        const val ADW_EXTRA_BADGE_COUNT = "COUNT"

        private const val APEX_ACTION_UPDATE_BADGE = "com.anddoes.launcher.COUNTER_CHANGED"
        private const val APEX_EXTRA_PACKAGE_NAME = "package"
        private const val APEX_EXTRA_CLASS_NAME = "class"
        private const val APEX_EXTRA_BADGE_COUNT = "count"
    }

}
