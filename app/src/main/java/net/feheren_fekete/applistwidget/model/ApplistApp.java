package net.feheren_fekete.applistwidget.model;

public class ApplistApp {
    private String mPackageName;
    public ApplistApp(String packageName) {
        mPackageName = packageName;
    }
    public String getPackageName() {
        return mPackageName;
    }
}
