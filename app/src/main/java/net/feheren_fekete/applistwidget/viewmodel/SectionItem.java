package net.feheren_fekete.applistwidget.viewmodel;


public class SectionItem extends BaseItem {
    private String mName;
    private boolean mIsRemovable;
    private boolean mIsCollapsed;

    public SectionItem(String name, boolean isRemovable, boolean isCollapsed) {
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
}
