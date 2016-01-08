package net.feheren_fekete.applistwidget;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplistModel {
    public static final String APPS_ALL = "All";

    private PackageManager mPackageManager;
    private String mApplistName;

    public ApplistModel(PackageManager packageManager, String applistName) {
        mPackageManager = packageManager;
        mApplistName = applistName;
    }

    public List<String> loadAllData() {
        switch (mApplistName) {
            case APPS_ALL:
                return getAllApps();
            default:
                return Collections.emptyList();
        }
    }

    public List<String> getAllApps() {
        List<String> packageNames = new ArrayList<>();
        List<ApplicationInfo> installedApps = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : installedApps) {
            packageNames.add(appInfo.packageName);
        }
        return packageNames;
    }

}
