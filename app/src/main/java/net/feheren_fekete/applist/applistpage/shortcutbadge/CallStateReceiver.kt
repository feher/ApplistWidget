package net.feheren_fekete.applist.applistpage.shortcutbadge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.AppUtils
import org.koin.java.KoinJavaComponent.inject

class CallStateReceiver : BroadcastReceiver() {

    private val settingsUtils: SettingsUtils by inject(SettingsUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0))

        if (!settingsUtils.showPhoneBadge) {
            return
        }

        val sharedPreferences = context.applicationContext.getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val bundle = intent.extras
        if (bundle == null) {
            val permissionState = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            ApplistLog.getInstance().log(RuntimeException(
                    "Intent has no extras: "
                            + "Intent action = " + intent.action
                            + ", Permission " + Manifest.permission.READ_PHONE_STATE + " = " + (permissionState == PackageManager.PERMISSION_GRANTED)))
            return
        }

        val state = bundle.getString(TelephonyManager.EXTRA_STATE, "")
        if (TelephonyManager.EXTRA_STATE_RINGING == state) {
            sharedPreferences!!.edit().putString(PREFERENCE_KEY_PREVIOUS_STATE, state).apply()
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK == state) {
            sharedPreferences!!.edit().putString(PREFERENCE_KEY_PREVIOUS_STATE, state).apply()
        } else if (TelephonyManager.EXTRA_STATE_IDLE == state) {
            val previousState = sharedPreferences!!.getString(PREFERENCE_KEY_PREVIOUS_STATE, "")
            if (TelephonyManager.EXTRA_STATE_RINGING == previousState) {
                val phoneAppComponentName = AppUtils.getPhoneApp(context)
                if (phoneAppComponentName != null) {
                    setBadgeCount(
                            phoneAppComponentName.packageName,
                            phoneAppComponentName.className,
                            BadgeUtils.NOT_NUMBERED_BADGE_COUNT)
                }
            }
        }
    }

    private fun setBadgeCount(packageName: String,
                              className: String,
                              badgeCount: Int) {
        badgeStore.setBadgeCount(packageName, className, badgeCount)
    }

    companion object {

        private val TAG = CallStateReceiver::class.java.simpleName

        private const val SHARED_PREFERENCES_NAME = "CallState"
        private const val PREFERENCE_KEY_PREVIOUS_STATE = "PreviousState"
    }

}
