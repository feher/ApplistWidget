package net.feheren_fekete.applistwidget.model;

import net.feheren_fekete.applistwidget.viewmodel.AppItem;

import java.util.Comparator;

public class AppData {
    private String mPackageName;
    private String mAppName;
    private String mComponentName;

    public AppData(String packageName, String componentName, String appName) {
        mPackageName = packageName;
        mComponentName = componentName;
        mAppName = appName;
    }

    public AppData(AppItem appItem) {
        mPackageName = appItem.getPackageName();
        mComponentName = appItem.getComponentName();
        mAppName = appItem.getAppName();
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getComponentName() {
        return mComponentName;
    }

    public String getAppName() {
        return mAppName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AppData)) {
            return false;
        }
        AppData other = (AppData) o;
        return mPackageName.equals(other.getPackageName())
                && mComponentName.equals(other.getComponentName());
    }

    public static final class NameComparator implements Comparator<AppData> {
        @Override
        public int compare(AppData lhs, AppData rhs) {
            return lhs.getAppName().compareTo(rhs.getAppName());
        }
    }
}
