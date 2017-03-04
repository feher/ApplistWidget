package net.feheren_fekete.applist.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import net.feheren_fekete.applist.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.AppUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Map;

public class BadgeStore {

    private static final String TAG = BadgeStore.class.getSimpleName();

    public static class BadgeEvent {
        public final String packageName;
        public final String className;
        public final int badgeCount;
        public BadgeEvent(String packageName, String className, int badgeCount) {
            this.packageName = packageName;
            this.className = className;
            this.badgeCount = badgeCount;
        }
    }

    private static final String SHARED_PREFERENCES_NAME = "Badges";
    private static final String KEY_BADGE_COUNT = "badge_count";
    private static final int DEFAULT_BADGE_COUNT = 0;

    private SharedPreferences mSharedPreferences;
    private PackageManager mPackageManager;
    private BadgeUtils mBadgeUtils;

    public BadgeStore(Context context, PackageManager packageManager, BadgeUtils badgeUtils) {
        mSharedPreferences = context.getApplicationContext().getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mPackageManager = packageManager;
        mBadgeUtils = badgeUtils;
    }

    public int getBadgeCount(String packageName, String className) {
        String fullKey = createFullKey(KEY_BADGE_COUNT, packageName, className);
        if (!mSharedPreferences.contains(fullKey)) {
            int badgeCount = mBadgeUtils.getBadgeCountFromLauncher(packageName, className);
            if (badgeCount != BadgeUtils.INVALID_BADGE_COUNT) {
                setBadgeCount(packageName, className, badgeCount, false);
            }
        }
        return mSharedPreferences.getInt(fullKey, DEFAULT_BADGE_COUNT);
    }

    public void setBadgeCount(String packageName, String className, int badgeCount) {
        setBadgeCount(packageName, className, badgeCount, true);
    }

    public void updateBadgesFromLauncher() {
        Map<String, ?> items = mSharedPreferences.getAll();
        for (String fullKey : items.keySet()) {
            String[] keyParts = splitFullKey(fullKey);
            String keyName = keyParts[0];
            if (KEY_BADGE_COUNT.equals(keyName)) {
                String packageName = keyParts[1];
                String className = keyParts[2];
                int badgeCount = mBadgeUtils.getBadgeCountFromLauncher(packageName, className);
                if (badgeCount != BadgeUtils.INVALID_BADGE_COUNT) {
                    Log.d(TAG, "Updating " + packageName + " " + className + " " + badgeCount);
                    setBadgeCount(packageName, className, badgeCount, false);
                }
            }
        }
        EventBus.getDefault().post(new BadgeEvent("", "", 0));
    }

    public void cleanup() {
        List<AppData> installedApps = AppUtils.getInstalledApps(mPackageManager);
        Map<String, ?> items = mSharedPreferences.getAll();
        for (String fullKey : items.keySet()) {
            String[] keyParts = splitFullKey(fullKey);
            String keyName = keyParts[0];
            if (KEY_BADGE_COUNT.equals(keyName)) {
                String packageName = keyParts[1];
                String className = keyParts[2];
                boolean isAppDeleted = true;
                for (AppData appData : installedApps) {
                    if (packageName.equals(appData.getPackageName())
                            && className.equals(appData.getComponentName())) {
                        isAppDeleted = false;
                        break;
                    }
                }
                if (isAppDeleted) {
                    mSharedPreferences.edit().remove(fullKey).apply();
                }
            }
        }
        EventBus.getDefault().post(new BadgeEvent("", "", 0));
    }

    private void setBadgeCount(String packageName, String className, int badgeCount,
                              boolean sendNotification) {
        String fullKey = createFullKey(KEY_BADGE_COUNT, packageName, className);
        mSharedPreferences.edit().putInt(fullKey, badgeCount).apply();
        if (sendNotification) {
            EventBus.getDefault().post(new BadgeEvent(packageName, className, badgeCount));
        }
    }

    private String createFullKey(String key, String packageName, String className) {
        return key + "::" + packageName + "::" + className;
    }

    private String[] splitFullKey(String fullKey) {
        String[] result = fullKey.split("::");
        return result;
    }

}