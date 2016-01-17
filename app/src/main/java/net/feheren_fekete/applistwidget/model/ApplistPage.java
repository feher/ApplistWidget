package net.feheren_fekete.applistwidget.model;

import java.util.List;

public class ApplistPage {
    private String mName;
    private List<ApplistSection> mSections;
    public ApplistPage(String name, List<ApplistSection> sections) {
        mName = name;
        mSections = sections;
    }
    public String getName() {
        return mName;
    }
}
