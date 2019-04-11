package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.AppUtils;

import static org.koin.java.KoinJavaComponent.get;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = SmsReceiver.class.getSimpleName();

    private SettingsUtils mSettingsUtils = get(SettingsUtils.class);
    private BadgeStore mBadgeStore = get(BadgeStore.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0));

        if (!mSettingsUtils.getShowSmsBadge()) {
            return;
        }

        ComponentName smsAppComponentName = AppUtils.getSmsApp(context);
        if (smsAppComponentName != null) {
            Log.d(TAG, "RECEIVED: " + smsAppComponentName.flattenToString());
            setBadgeCount(
                    smsAppComponentName.getPackageName(),
                    smsAppComponentName.getClassName(),
                    BadgeUtils.NOT_NUMBERED_BADGE_COUNT);
        }
    }

    private void setBadgeCount(String packageName,
                               String className,
                               int badgeCount) {
        mBadgeStore.setBadgeCount(packageName, className, badgeCount);
    }

}
