package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import net.feheren_fekete.applist.ApplistLog;

import java.util.Arrays;
import java.util.List;

public class BadgeUtils {

    private static final String TAG = BadgeUtils.class.getSimpleName();

    public static final int INVALID_BADGE_COUNT = -1;
    public static final int NOT_NUMBERED_BADGE_COUNT = Integer.MAX_VALUE;

    // Querying badges does not work for Sony.
    // We don't have permission to do so.
    private static final List<String> sSonyLaunchers = Arrays.asList(
//            "com.sonyericsson.home",
//            "com.sonymobile.home"
    );
    private static final String SONY_PROVIDER_CONTENT_URI = "content://com.sonymobile.home.resourceprovider/badge";
    private static final String SONY_COLUMN_PACKAGE_NAME = "package_name";
    private static final String SONY_COLUMN_ACTIVITY_NAME = "activity_name";
    private static final String SONY_COLUMN_BADGE_COUNT = "badge_count";

    private static final List<String> sHuaweiLaunchers = Arrays.asList(
            "com.huawei.android.launcher"
    );
    private static final String HUAWEI_PROVIDER_CONTENT_URI = "content://com.huawei.android.launcher.settings/badge";
    private static final String HUAWEI_COLUMN_PACKAGE = "package";
    private static final String HUAWEI_COLUMN_CLASS = "class";
    private static final String HUAWEI_COLUMN_BADGENUMBER = "badgenumber";

    private static final List<String> sSamsungLaunchers = Arrays.asList(
            "com.sec.android.app.launcher",
            "com.sec.android.app.twlauncher"
    );
    private static final String SAMSUNG_PROVIDER_CONTENT_URI = "content://com.sec.badge/apps?notify=true";
    private static final String SAMSUNG_COLUMN_PACKAGE = "package";
    private static final String SAMSUNG_COLUMN_CLASS = "class";
    private static final String SAMSUNG_COLUMN_BADGECOUNT = "badgecount";

    // Querying badges does not work for Nova (TeslaUnread).
    // We don't have permission to do so.
    private static final List<String> sNovaLaunchers = Arrays.asList(
//            "com.teslacoilsw.launcher"
    );
    private static final String NOVA_PROVIDER_CONTENT_URI = "content://com.teslacoilsw.notifier/unread_count";
    private static final String NOVA_COLUMN_TAG = "tag";
    private static final String NOVA_COLUMN_COUNT = "count";

    private Context mContext;

    public BadgeUtils(Context context) {
        mContext = context;
    }

    public int getBadgeCountFromLauncher(String packageName, String className) {
        String launcherPackageName = getLauncherPackageName();
        if (sSamsungLaunchers.contains(launcherPackageName)) {
            return getSamsungBadgeCount(packageName, className);
        } else if (sNovaLaunchers.contains(launcherPackageName)) {
            return getNovaBadgeCount(packageName, className);
        } else if (sSonyLaunchers.contains(launcherPackageName)) {
            return getSonyBadgeCount(packageName, className);
        } else if (sHuaweiLaunchers.contains(launcherPackageName)) {
            return getHuaweiBadgeCount(packageName, className);
        } else {
            return INVALID_BADGE_COUNT;
        }
    }

    private int getSamsungBadgeCount(String packageName, String className) {
        int result = INVALID_BADGE_COUNT;
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Uri.parse(SAMSUNG_PROVIDER_CONTENT_URI),
                    new String[]{SAMSUNG_COLUMN_BADGECOUNT},
                    SAMSUNG_COLUMN_PACKAGE + " = ? "
                            + " AND " + SAMSUNG_COLUMN_CLASS + " = ?",
                    new String[]{packageName, className},
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    int badgeCountColumnIndex = cursor.getColumnIndex(SAMSUNG_COLUMN_BADGECOUNT);
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex);
                    }
                } else {
                    result = 0;
                }
            }
        } catch (SecurityException | SQLException e) {
            ApplistLog.getInstance().log(new RuntimeException("Cannot get badge count", e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private int getNovaBadgeCount(String packageName, String className) {
        int result = INVALID_BADGE_COUNT;
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Uri.parse(NOVA_PROVIDER_CONTENT_URI),
                    new String[]{NOVA_COLUMN_COUNT},
                    NOVA_COLUMN_TAG + " = ? ",
                    new String[]{packageName + "/" + className},
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    int badgeCountColumnIndex = cursor.getColumnIndex(NOVA_COLUMN_COUNT);
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex);
                    }
                } else {
                    result = 0;
                }
            }
        } catch (SecurityException | SQLException e) {
            ApplistLog.getInstance().log(new RuntimeException("Cannot get badge count", e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private int getSonyBadgeCount(String packageName, String className) {
        int result = INVALID_BADGE_COUNT;
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Uri.parse(SONY_PROVIDER_CONTENT_URI),
                    new String[]{SONY_COLUMN_BADGE_COUNT},
                    SONY_COLUMN_PACKAGE_NAME + " = ? "
                            + " AND " + SONY_COLUMN_ACTIVITY_NAME + " = ?",
                    new String[]{packageName, className},
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    int badgeCountColumnIndex = cursor.getColumnIndex(SONY_COLUMN_BADGE_COUNT);
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex);
                    }
                } else {
                    result = 0;
                }
            }
        } catch (SecurityException | SQLException e) {
            ApplistLog.getInstance().log(new RuntimeException("Cannot get badge count", e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private int getHuaweiBadgeCount(String packageName, String className) {
        int result = INVALID_BADGE_COUNT;
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    Uri.parse(HUAWEI_PROVIDER_CONTENT_URI),
                    new String[]{HUAWEI_COLUMN_BADGENUMBER},
                    HUAWEI_COLUMN_PACKAGE + " = ? "
                            + " AND " + HUAWEI_COLUMN_CLASS + " = ?",
                    new String[]{packageName, className},
                    null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    int badgeCountColumnIndex = cursor.getColumnIndex(HUAWEI_COLUMN_BADGENUMBER);
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex);
                    }
                } else {
                    result = 0;
                }
            }
        } catch (SecurityException | SQLException e) {
            ApplistLog.getInstance().log(new RuntimeException("Cannot get badge count", e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private String getLauncherPackageName() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null ||
            resolveInfo.activityInfo.name.toLowerCase().contains("resolver")) {
            return "";
        }
        return resolveInfo.activityInfo.packageName;
    }

}
