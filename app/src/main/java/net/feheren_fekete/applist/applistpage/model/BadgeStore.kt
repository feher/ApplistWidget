package net.feheren_fekete.applist.applistpage.model

import android.content.Context
import android.util.Log

import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.utils.AppUtils

import org.greenrobot.eventbus.EventBus

class BadgeStore(private val context: Context,
                 private val badgeUtils: BadgeUtils) {

    private val sharedPreferences =
            context.applicationContext.getSharedPreferences(
                    SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    class BadgeEvent(val packageName: String,
                     val className: String,
                     val badgeCount: Int)

    fun getBadgeCount(packageName: String, className: String): Int {
        val fullKey = createFullKey(KEY_BADGE_COUNT, packageName, className)
        if (sharedPreferences.contains(fullKey)) {
            val badgeCount = sharedPreferences.getInt(fullKey, DEFAULT_BADGE_COUNT)
            if (badgeCount != 0) {
                return badgeCount
            }
        }

        val halfKey = createFullKey(KEY_BADGE_COUNT, packageName, "")
        if (sharedPreferences.contains(halfKey)) {
            return sharedPreferences.getInt(halfKey, DEFAULT_BADGE_COUNT)
        }

        val badgeCount = badgeUtils.getBadgeCountFromLauncher(packageName, className)
        if (badgeCount != BadgeUtils.INVALID_BADGE_COUNT) {
            setBadgeCount(packageName, className, badgeCount, false)
        }
        return sharedPreferences.getInt(fullKey, DEFAULT_BADGE_COUNT)
    }

    fun setBadgeCount(packageName: String, className: String, badgeCount: Int) {
        setBadgeCount(packageName, className, badgeCount, true)
    }

    fun updateBadgesFromLauncher() {
        val items = sharedPreferences.all
        for (fullKey in items.keys) {
            val keyParts = splitFullKey(fullKey)
            val keyName = keyParts[0]
            if (KEY_BADGE_COUNT == keyName) {
                val packageName = keyParts[1]
                val className = keyParts[2]
                val badgeCount = badgeUtils.getBadgeCountFromLauncher(packageName, className)
                if (badgeCount != BadgeUtils.INVALID_BADGE_COUNT) {
                    Log.d(TAG, "Updating $packageName $className $badgeCount")
                    setBadgeCount(packageName, className, badgeCount, false)
                }
            }
        }
        EventBus.getDefault().post(BadgeEvent("", "", 0))
    }

    fun cleanup() {
        val installedApps = AppUtils.getInstalledApps(context)
        val items = sharedPreferences.all
        for (fullKey in items.keys) {
            val keyParts = splitFullKey(fullKey)
            val keyName = keyParts[0]
            if (KEY_BADGE_COUNT == keyName) {
                val packageName = keyParts[1]
                var isAppDeleted = true
                for (appData in installedApps) {
                    if (packageName == appData.packageName) {
                        isAppDeleted = false
                        break
                    }
                }
                if (isAppDeleted) {
                    sharedPreferences.edit().remove(fullKey).apply()
                }
            }
        }
        EventBus.getDefault().post(BadgeEvent("", "", 0))
    }

    private fun setBadgeCount(packageName: String, className: String, badgeCount: Int,
                              sendNotification: Boolean) {
        val fullKey = createFullKey(KEY_BADGE_COUNT, packageName, className)
        sharedPreferences.edit().putInt(fullKey, badgeCount).apply()
        if (sendNotification) {
            EventBus.getDefault().post(BadgeEvent(packageName, className, badgeCount))
        }
    }

    private fun createFullKey(key: String, packageName: String, className: String): String {
        return "$key::$packageName::$className"
    }

    private fun splitFullKey(fullKey: String): Array<String> {
        return fullKey.split("::").toTypedArray()
    }

    companion object {
        private val TAG = BadgeStore::class.java.simpleName

        private const val SHARED_PREFERENCES_NAME = "Badges"
        private const val KEY_BADGE_COUNT = "badge_count"
        private const val DEFAULT_BADGE_COUNT = 0
    }

}
