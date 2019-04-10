package net.feheren_fekete.applist.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Telephony;

import net.feheren_fekete.applist.applistpage.model.AppData;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class AppUtils {

    private static final String APPLIST_PREFERENCES = "ApplistPreferences";
    private static final String PREFERENCE_PHONE_APP = "phoneApp";

    public static List<AppData> getInstalledApps(PackageManager packageManager) {
        List<AppData> installedApps = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppInfos = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : installedAppInfos) {
            installedApps.add(new AppData(
                    createAppId(
                            resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name),
                    resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name,
                    resolveInfo.loadLabel(packageManager).toString()));
        }
        return installedApps;
    }

    @Nullable
    public static ComponentName getSmsApp(Context context) {
        String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(smsPackage);
        if (smsAppIntent != null) {
            return smsAppIntent.getComponent();
        } else {
            return null;
        }
    }

    @Nullable
    public static ComponentName getPhoneApp(Context appContext) {
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(
                APPLIST_PREFERENCES, Context.MODE_PRIVATE);

        Intent phoneIntent = getPhoneIntent();
        PackageManager packageManager = appContext.getPackageManager();
        ResolveInfo candidateResolveInfo = packageManager.resolveActivity(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
        List<ResolveInfo> availableResolveInfos = packageManager.queryIntentActivities(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean hasDefaultPhoneApp = false;
        for (ResolveInfo resolveInfo : availableResolveInfos) {
            if (resolveInfo.activityInfo.name.equals(candidateResolveInfo.activityInfo.name)
                    && resolveInfo.activityInfo.packageName.equals(candidateResolveInfo.activityInfo.packageName)) {
                hasDefaultPhoneApp = true;
                break;
            }
        }

        if (hasDefaultPhoneApp) {
            sharedPreferences.edit().remove(PREFERENCE_PHONE_APP).apply();
            return new ComponentName(
                    candidateResolveInfo.activityInfo.packageName,
                    candidateResolveInfo.activityInfo.name);
        } else {
            String phoneAppComponentString = sharedPreferences.getString(PREFERENCE_PHONE_APP, "");
            if (!phoneAppComponentString.isEmpty()) {
                return ComponentName.unflattenFromString(phoneAppComponentString);
            } else {
                return null;
            }
        }
    }

    public static void savePhoneApp(Context appContext, ComponentName componentName) {
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(
                APPLIST_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(PREFERENCE_PHONE_APP, componentName.flattenToString()).apply();
    }

    public static List<ResolveInfo> getAvailableAppsForIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    public static Intent getPhoneIntent() {
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:+1234567890"));
        return phoneIntent;
    }

    private static long createAppId(String packageName, String className) {
        return (packageName + className).hashCode();
    }

}
