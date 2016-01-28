package net.feheren_fekete.applistwidget.viewmodel;


public class AppItem extends BaseItem {
    private String mPackageName;
    private String mComponentName;
    private String mAppName;

    public AppItem(String packageName,
                   String componentName,
                   String appName) {
        mPackageName = packageName;
        mComponentName = componentName;
        mAppName = appName;
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
}
