package net.feheren_fekete.applist.applistpage.viewmodel;


public class AppItem extends StartableItem {
    private String mPackageName;
    private String mClassName;
    private String mAppName;

    public AppItem(long id,
                   String packageName,
                   String className,
                   String appName) {
        super(id);
        mPackageName = packageName;
        mClassName = className;
        mAppName = appName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getClassName() {
        return mClassName;
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
                && mClassName.equals(other.getClassName());
    }
}
