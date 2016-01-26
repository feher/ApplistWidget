package net.feheren_fekete.applistwidget.model;

public class AppData {
    private String mPackageName;
    private String mAppName;

    public AppData(String packageName, String appName) {
        mPackageName = packageName;
        mAppName = appName;
    }

    public String getPackageName() {
        return mPackageName;
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
        return mPackageName.equals(other.getPackageName());
    }
}
