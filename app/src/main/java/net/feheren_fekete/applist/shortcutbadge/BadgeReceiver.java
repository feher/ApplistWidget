package net.feheren_fekete.applist.shortcutbadge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.model.BadgeStore;

public class BadgeReceiver extends BroadcastReceiver {

    private static final String TAG = BadgeReceiver.class.getSimpleName();

    private static final String DEFAULT_ACTION_UPDATE_BADGE = "android.intent.action.BADGE_COUNT_UPDATE";
    private static final String DEFAULT_EXTRA_BADGE_COUNT = "badge_count";
    private static final String DEFAULT_EXTRA_PACKAGE_NAME = "badge_count_package_name";
    private static final String DEFAULT_EXTRA_CLASS_NAME = "badge_count_class_name";

    private static final String SONY_ACTION_UPDATE_BADGE = "com.sonyericsson.home.action.UPDATE_BADGE";
    private static final String SONY_EXTRA_PACKAGE_NAME = "com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME";
    private static final String SONY_EXTRA_CLASS_NAME = "com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME";
    private static final String SONY_EXTRA_BADGE_COUNT = "com.sonyericsson.home.intent.extra.badge.MESSAGE";
    private static final String SONY_EXTRA_SHOW_MESSAGE = "com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE";

    public static final String HTC_1_ACTION_UPDATE_BADGE = "com.htc.launcher.action.SET_NOTIFICATION";
    public static final String HTC_1_EXTRA_COMPONENT = "com.htc.launcher.extra.COMPONENT";
    public static final String HTC_1_EXTRA_BADGE_COUNT = "com.htc.launcher.extra.COUNT";

    public static final String HTC_2_ACTION_UPDATE_BADGE = "com.htc.launcher.action.UPDATE_SHORTCUT";
    public static final String HTC_2_EXTRA_PACKAGE_NAME = "packagename";
    public static final String HTC_2_EXTRA_BADGE_COUNT = "count";

    public static final String ADW_ACTION_UPDATE_BADGE = "org.adw.launcher.counter.SEND";
    public static final String ADW_EXTRA_PACKAGE_NAME = "PNAME";
    public static final String ADW_EXTRA_CLASS_NAME = "CNAME";
    public static final String ADW_EXTRA_BADGE_COUNT = "COUNT";

    private static final String APEX_ACTION_UPDATE_BADGE = "com.anddoes.launcher.COUNTER_CHANGED";
    private static final String APEX_EXTRA_PACKAGE_NAME = "package";
    private static final String APEX_EXTRA_CLASS_NAME = "class";
    private static final String APEX_EXTRA_BADGE_COUNT = "count";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent.toUri(0));
        if (DEFAULT_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleDefault(context, intent);
        } else if (SONY_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleSony(context, intent);
        } else if (ADW_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleAdw(context, intent);
        } else if (APEX_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleApex(context, intent);
        } else if (HTC_1_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleHtc1(context, intent);
        } else if (HTC_2_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
            handleHtc2(context, intent);
        }
        // handle app uninstalls: remove obsolete BadgeStore items.
    }

    public void handleDefault(Context context, Intent intent) {
        String packageName = intent.getStringExtra(DEFAULT_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(DEFAULT_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(DEFAULT_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, className, badgeCount);
        }
    }

    public void handleSony(Context context, Intent intent) {
        String packageName = intent.getStringExtra(SONY_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(SONY_EXTRA_CLASS_NAME);
        String badgeCountString = intent.getStringExtra(SONY_EXTRA_BADGE_COUNT);
        boolean showMessage = intent.getBooleanExtra(SONY_EXTRA_SHOW_MESSAGE, false);
        if (packageName != null && className != null) {
            int badgeCount = 0;
            if (badgeCountString != null) {
                try {
                    badgeCount = Integer.parseInt(badgeCountString);
                } catch (NumberFormatException e) {
                    ApplistLog.getInstance().log(new RuntimeException("Bad badge count format: " + badgeCountString, e));
                }
            }
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            if (!showMessage) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, className, badgeCount);
        }
    }

    public void handleAdw(Context context, Intent intent) {
        String packageName = intent.getStringExtra(ADW_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(ADW_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(ADW_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, className, badgeCount);
        }
    }

    public void handleApex(Context context, Intent intent) {
        String packageName = intent.getStringExtra(APEX_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(APEX_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(APEX_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, className, badgeCount);
        }
    }

    public void handleHtc1(Context context, Intent intent) {
        String componentNameString = intent.getStringExtra(HTC_1_EXTRA_COMPONENT);
        int badgeCount = intent.getIntExtra(HTC_1_EXTRA_BADGE_COUNT, 0);
        if (componentNameString != null) {
            ComponentName componentName = ComponentName.unflattenFromString(componentNameString);
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, className, badgeCount);
        }
    }

    public void handleHtc2(Context context, Intent intent) {
        String packageName = intent.getStringExtra(HTC_2_EXTRA_PACKAGE_NAME);
        int badgeCount = intent.getIntExtra(HTC_2_EXTRA_BADGE_COUNT, 0);
        if (packageName != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(context, packageName, "", badgeCount);
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
