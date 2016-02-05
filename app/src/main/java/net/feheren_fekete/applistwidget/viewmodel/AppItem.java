package net.feheren_fekete.applistwidget.viewmodel;


public class AppItem extends BaseItem {
    private String mPackageName;
    private String mComponentName;
    private String mAppName;

    public AppItem(long id,
                   String packageName,
                   String componentName,
                   String appName) {
        super(id);
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

    @Override
    public String getName() {
        return mAppName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AppItem)) {
            return false;
        }
        AppItem other = (AppItem) o;
        return mPackageName.equals(other.getPackageName())
                && mComponentName.equals(other.getComponentName());
    }
}
