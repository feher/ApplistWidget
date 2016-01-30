package net.feheren_fekete.applistwidget.viewmodel;


public class SectionItem extends BaseItem {
    private String mName;
    private boolean mIsRemovable;

    public SectionItem(String name, boolean isRemovable) {
        mName = name;
        mIsRemovable = isRemovable;
    }

    @Override
    public String getName() {
        return mName;
    }

    public boolean isRemovable() {
        return mIsRemovable;
    }
}
