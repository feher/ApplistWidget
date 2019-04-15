package net.feheren_fekete.applist.applistpage.shortcutbadge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.AppUtils
import org.koin.java.KoinJavaComponent.inject

class SmsReceiver : BroadcastReceiver() {

    private val settingsUtils: SettingsUtils by inject(SettingsUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0))

        if (!settingsUtils.showSmsBadge) {
            return
        }

        val smsAppComponentName = AppUtils.getSmsApp(context)
        if (smsAppComponentName != null) {
            Log.d(TAG, "RECEIVED: " + smsAppComponentName.flattenToString())
            setBadgeCount(
                    smsAppComponentName.packageName,
                    smsAppComponentName.className,
                    BadgeUtils.NOT_NUMBERED_BADGE_COUNT)
        }
    }

    private fun setBadgeCount(packageName: String,
                              className: String,
                              badgeCount: Int) {
        badgeStore.setBadgeCount(packageName, className, badgeCount)
    }

    companion object {
        private val TAG = SmsReceiver::class.java.simpleName
    }

}
