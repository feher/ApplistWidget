package net.feheren_fekete.applist.viewmodel;


public class SectionItem extends BaseItem {
    private String mName;
    private boolean mIsRemovable;
    private boolean mIsCollapsed;

    public SectionItem(long id, String name, boolean isRemovable, boolean isCollapsed) {
        super(id);
        mName = name;
        mIsRemovable = isRemovable;
        mIsCollapsed = isCollapsed;
    }

    @Override
    public String getName() {
        return mName;
    }

    public boolean isRemovable() {
        return mIsRemovable;
    }

    public boolean isCollapsed() {
        return mIsCollapsed;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof SectionItem)) {
            return false;
        }
        SectionItem other = (SectionItem) o;
        return mName.equals(other.getName());
    }
}
