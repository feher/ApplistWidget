package net.feheren_fekete.applist.applistpage.model;

import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;

import java.util.Comparator;

public class AppData extends StartableData {
    private String mPackageName;
    private String mClassName;

    public AppData(long id, String packageName, String className, String appName) {
        super(id, appName);
        mPackageName = packageName;
        mClassName = className;
    }

    public AppData(AppItem appItem) {
        super(appItem.getId(), appItem.getName());
        mPackageName = appItem.getPackageName();
        mClassName = appItem.getClassName();
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getClassName() {
        return mClassName;
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

}
