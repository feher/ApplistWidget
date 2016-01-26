package net.feheren_fekete.applistwidget.viewmodel;


public class AppItem extends BaseItem {
    private String mPackageName;
    private String mAppName;

    public AppItem(String packageName, String appName) {
        mPackageName = packageName;
        mAppName = appName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getAppName() {
        return mAppName;
    }
}
