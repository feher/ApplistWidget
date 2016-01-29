package net.feheren_fekete.applistwidget.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SectionData {
    private String mName;
    private List<AppData> mApps;
    private boolean mIsRemovable;

    public SectionData(String name,
                       List<AppData> apps,
                       boolean isRemovable) {
        mName = name;
        mApps = apps;
        mIsRemovable = isRemovable;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isRemovable() {
        return mIsRemovable;
    }

    public List<AppData> getApps() {
        return mApps;
    }

    public void setApps(List<AppData> apps) {
        mApps = apps;
        Collections.sort(mApps, new AppData.NameComparator());
    }

    public void addApps(List<AppData> apps) {
        mApps.addAll(apps);
        Collections.sort(mApps, new AppData.NameComparator());
    }

    public void addApp(AppData app) {
        mApps.add(app);
        Collections.sort(mApps, new AppData.NameComparator());
    }

    public boolean hasApp(AppData app) {
        return mApps.contains(app);
    }

    public boolean removeApp(AppData app) {
        return mApps.remove(app);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof SectionData)) {
            return false;
        }
        SectionData other = (SectionData) o;
        return mName.equals(other.getName());
    }

    public static final class NameComparator implements Comparator<SectionData> {
        @Override
        public int compare(SectionData lhs, SectionData rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

}
