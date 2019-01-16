package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.settings.SettingsUtils;

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

    // TODO: Inject
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Note: intent.toUri(0) may crash with "Duplicate key in ArrayMap: badge_count"
        // Log.d(TAG, "RECEIVED: " + intent.toUri(0));

        if (!mSettingsUtils.getShowNewContentBadge()) {
            return;
        }

        try {
            if (DEFAULT_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleDefault(intent);
            } else if (SONY_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleSony(intent);
            } else if (ADW_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleAdw(intent);
            } else if (APEX_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleApex(intent);
            } else if (HTC_1_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleHtc1(intent);
            } else if (HTC_2_ACTION_UPDATE_BADGE.equals(intent.getAction())) {
                handleHtc2(intent);
            }
        } catch (IllegalArgumentException e) {
            ApplistLog.getInstance().log(e);
        }
    }

    public void handleDefault(Intent intent) {
        String packageName = intent.getStringExtra(DEFAULT_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(DEFAULT_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(DEFAULT_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(packageName, className, badgeCount);
        }
    }

    public void handleSony(Intent intent) {
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
            setBadgeCount(packageName, className, badgeCount);
        }
    }

    public void handleAdw(Intent intent) {
        String packageName = intent.getStringExtra(ADW_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(ADW_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(ADW_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(packageName, className, badgeCount);
        }
    }

    public void handleApex(Intent intent) {
        String packageName = intent.getStringExtra(APEX_EXTRA_PACKAGE_NAME);
        String className = intent.getStringExtra(APEX_EXTRA_CLASS_NAME);
        int badgeCount = intent.getIntExtra(APEX_EXTRA_BADGE_COUNT, 0);
        if (packageName != null && className != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(packageName, className, badgeCount);
        }
    }

    public void handleHtc1(Intent intent) {
        String componentNameString = intent.getStringExtra(HTC_1_EXTRA_COMPONENT);
        int badgeCount = intent.getIntExtra(HTC_1_EXTRA_BADGE_COUNT, 0);
        if (componentNameString != null) {
            ComponentName componentName = ComponentName.unflattenFromString(componentNameString);
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(packageName, className, badgeCount);
        }
    }

    public void handleHtc2(Intent intent) {
        String packageName = intent.getStringExtra(HTC_2_EXTRA_PACKAGE_NAME);
        int badgeCount = intent.getIntExtra(HTC_2_EXTRA_BADGE_COUNT, 0);
        if (packageName != null) {
            if (badgeCount < 0) {
                badgeCount = 0;
            }
            setBadgeCount(packageName, "", badgeCount);
        }
    }

    private void setBadgeCount(String packageName,
                               String className,
                               int badgeCount) {
        BadgeStore.getInstance().setBadgeCount(packageName, className, badgeCount);
    }

}
