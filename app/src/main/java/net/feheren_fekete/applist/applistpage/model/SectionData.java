package net.feheren_fekete.applist.applistpage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SectionData extends BaseData {
    private String mName;
    private List<StartableData> mStartables;
    private boolean mIsRemovable;
    private boolean mIsCollapsed;

    public SectionData(long id,
                       String name,
                       List<StartableData> startables,
                       boolean isRemovable,
                       boolean isCollapsed) {
        super(id);
        mName = name;
        mStartables = startables;
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
        return mStartables.isEmpty();
    }

    public List<StartableData> getStartables() {
        return mStartables;
    }

    public List<AppData> getApps() {
        ArrayList<AppData> result = new ArrayList<>();
        for (StartableData startableData : mStartables) {
            if (startableData instanceof AppData) {
                result.add((AppData) startableData);
            }
        }
        return result;
    }

    public void setStartables(List<StartableData> startables) {
        mStartables = startables;
    }

    public void addStartables(int index, List<StartableData> startableDatas) {
        mStartables.addAll(index, startableDatas);
    }

    public void addApps(int index, List<AppData> apps) {
        mStartables.addAll(index, apps);
    }

    public void addStartable(StartableData startableData) {
        mStartables.add(startableData);
    }

    public boolean hasStartable(StartableData startableData) {
        return mStartables.contains(startableData);
    }

    public boolean removeStartable(StartableData startableData) {
        return mStartables.remove(startableData);
    }

    public void sortStartablesAlphabetically() {
        Collections.sort(mStartables, new StartableData.NameComparator());
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
