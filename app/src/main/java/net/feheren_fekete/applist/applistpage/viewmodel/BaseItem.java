package net.feheren_fekete.applist.applistpage.viewmodel;

public abstract class BaseItem {

    public static final int NONE = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private long mId;
    private String mName;
    private boolean mIsEnabled = true;
    private boolean mIsHighlighted = false;
    private boolean mIsSelected = false;

    public BaseItem(long id, String name) {
        mId = id;
        mName = name;
    }

    public long getId() {
        return mId;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }

    public boolean isHighlighted() {
        return mIsHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        mIsHighlighted = highlighted;
    }

    public String getName() {
        return mName;
    }
}
