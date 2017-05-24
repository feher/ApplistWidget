package net.feheren_fekete.applist.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SectionData extends BaseData {
    private String mName;
    private List<AppData> mApps;
    private boolean mIsRemovable;
    private boolean mIsCollapsed;

    public SectionData(long id,
                       String name,
                       List<AppData> apps,
                       boolean isRemovable,
                       boolean isCollapsed) {
        super(id);
        mName = name;
        mApps = apps;
        mIsRemovable = isRemovable;
        mIsCollapsed = isCollapsed;
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

    public void setCollapsed(boolean collapsed) {
        mIsCollapsed = collapsed;
    }

    public boolean isCollapsed() {
        return mIsCollapsed;
    }

    public boolean isEmpty() {
        return mApps.isEmpty();
    }

    public List<AppData> getApps() {
        return mApps;
    }

    public void setApps(List<AppData> apps) {
        mApps = apps;
    }

    public void addApps(int index, List<AppData> apps) {
        mApps.addAll(index, apps);
    }

    public void addApp(AppData app) {
        mApps.add(app);
    }

    public boolean hasApp(AppData app) {
        return mApps.contains(app);
    }

    public boolean removeApp(AppData app) {
        return mApps.remove(app);
    }

    public void sortAppsAlphabetically() {
        Collections.sort(mApps, new AppData.NameComparator());
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
