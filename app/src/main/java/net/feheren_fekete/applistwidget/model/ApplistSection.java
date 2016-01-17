package net.feheren_fekete.applistwidget.model;

import java.util.List;

public class ApplistSection {
    private String mName;
    private List<ApplistApp> mApps;
    public ApplistSection(String name,
                          List<ApplistApp> apps) {
        mName = name;
        mApps = apps;
    }
    public String getName() {
        return mName;
    }
}
