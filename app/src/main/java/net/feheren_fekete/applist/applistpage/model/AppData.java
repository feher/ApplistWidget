package net.feheren_fekete.applist.applistpage.model;

import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;

import java.util.Comparator;

public class AppData extends BaseData {
    private String mPackageName;
    private String mClassName;
    private String mAppName;

    public AppData(long id, String packageName, String className, String appName) {
        super(id);
        mPackageName = packageName;
        mClassName = className;
        mAppName = appName;
    }

    public AppData(AppItem appItem) {
        super(appItem.getId());
        mPackageName = appItem.getPackageName();
        mClassName = appItem.getClassName();
        mAppName = appItem.getName();
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getClassName() {
        return mClassName;
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
                && mClassName.equals(other.getClassName());
    }

    public static final class NameComparator implements Comparator<AppData> {
        @Override
        public int compare(AppData lhs, AppData rhs) {
            return lhs.getAppName().compareTo(rhs.getAppName());
        }
    }
}
