package net.feheren_fekete.applist.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Telephony;
import android.support.annotation.Nullable;

import net.feheren_fekete.applist.model.AppData;

import java.util.ArrayList;
import java.util.List;

public class AppUtils {

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
    public static ComponentName getPhoneApp(Context context) {
        Intent phoneIntent = getPhoneIntent();
        PackageManager packageManager = context.getPackageManager();
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
            return new ComponentName(
                    candidateResolveInfo.activityInfo.packageName,
                    candidateResolveInfo.activityInfo.name);
        } else {
            return null;
        }
    }

    public static Intent getPhoneIntent() {
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:+1234567890"));
        return phoneIntent;
    }

    private static long createAppId(String packageName, String componentName) {
        return (packageName + componentName).hashCode();
    }

}
