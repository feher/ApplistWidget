package net.feheren_fekete.applist.shortcutbadge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.utils.AppUtils;

public class CallStateReceiver extends BroadcastReceiver {

    private static final String TAG = CallStateReceiver.class.getSimpleName();

    private static final String SHARED_PREFERENCES_NAME = "CallState";
    private static final String PREFERENCE_KEY_PREVIOUS_STATE = "PreviousState";

    private SharedPreferences mSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0));

        mSharedPreferences = context.getApplicationContext().getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE, "");
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            mSharedPreferences.edit().putString(PREFERENCE_KEY_PREVIOUS_STATE, state).apply();
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            mSharedPreferences.edit().putString(PREFERENCE_KEY_PREVIOUS_STATE, state).apply();
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            String previousState = mSharedPreferences.getString(PREFERENCE_KEY_PREVIOUS_STATE, "");
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(previousState)) {
                ComponentName phoneAppComponentName = AppUtils.getPhoneApp(context);
                if (phoneAppComponentName != null) {
                    setBadgeCount(
                            context,
                            phoneAppComponentName.getPackageName(),
                            phoneAppComponentName.getClassName(),
                            BadgeUtils.NOT_NUMBERED_BADGE_COUNT);
                }
            }
        }
    }

    private void setBadgeCount(Context context,
                               String packageName,
                               String className,
                               int badgeCount) {
        BadgeStore badgeStore = new BadgeStore(
                context,
                context.getPackageManager(),
                new BadgeUtils(context));
        badgeStore.setBadgeCount(packageName, className, badgeCount);
    }

}
