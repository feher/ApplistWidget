package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.AppUtils;

import static org.koin.java.KoinJavaComponent.get;

public class CallStateReceiver extends BroadcastReceiver {

    private static final String TAG = CallStateReceiver.class.getSimpleName();

    private static final String SHARED_PREFERENCES_NAME = "CallState";
    private static final String PREFERENCE_KEY_PREVIOUS_STATE = "PreviousState";

    private SettingsUtils mSettingsUtils = get(SettingsUtils.class);
    private BadgeStore mBadgeStore = get(BadgeStore.class);

    private SharedPreferences mSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0));

        if (!mSettingsUtils.getShowPhoneBadge()) {
            return;
        }

        mSharedPreferences = context.getApplicationContext().getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            final int permissionState = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
            ApplistLog.getInstance().log(new RuntimeException(
                    "Intent has no extras: "
                            + "Intent action = " + intent.getAction()
                            + ", Permission " + Manifest.permission.READ_PHONE_STATE + " = " + (permissionState == PackageManager.PERMISSION_GRANTED)));
            return;
        }

        String state = bundle.getString(TelephonyManager.EXTRA_STATE, "");
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
                            phoneAppComponentName.getPackageName(),
                            phoneAppComponentName.getClassName(),
                            BadgeUtils.NOT_NUMBERED_BADGE_COUNT);
                }
            }
        }
    }

    private void setBadgeCount(String packageName,
                               String className,
                               int badgeCount) {
        mBadgeStore.setBadgeCount(packageName, className, badgeCount);
    }

}
