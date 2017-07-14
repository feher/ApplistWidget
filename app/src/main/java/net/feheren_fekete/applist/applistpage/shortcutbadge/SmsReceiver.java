package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.utils.AppUtils;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = SmsReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0));
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
        BadgeStore.getInstance().setBadgeCount(packageName, className, badgeCount);
    }

}
