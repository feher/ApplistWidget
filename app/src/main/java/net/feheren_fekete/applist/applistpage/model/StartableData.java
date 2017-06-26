package net.feheren_fekete.applist.applistpage.model;

import java.util.Comparator;

public class StartableData extends BaseData {

    private String mName;

    public StartableData(long id, String name) {
        super(id);
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public static final class NameComparator implements Comparator<StartableData> {
        @Override
        public int compare(StartableData lhs, StartableData rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

}
