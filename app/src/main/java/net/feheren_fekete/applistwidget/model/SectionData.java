package net.feheren_fekete.applistwidget.model;

import java.util.List;

public class SectionData {
    private String mName;
    private List<AppData> mApps;

    public SectionData(String name,
                       List<AppData> apps) {
        mName = name;
        mApps = apps;
    }

    public String getName() {
        return mName;
    }

    public List<AppData> getApps() {
        return mApps;
    }

    public void setApps(List<AppData> apps) {
        mApps = apps;
    }

    public void addApps(List<AppData> apps) {
        mApps.addAll(apps);
    }

    public boolean hasApp(AppData app) {
        for (AppData sectionApp : mApps) {
            if (sectionApp.getPackageName().equals(app.getPackageName())) {
                return true;
            }
        }
        return false;
    }

}
