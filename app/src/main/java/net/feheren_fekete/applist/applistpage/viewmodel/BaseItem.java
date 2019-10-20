package net.feheren_fekete.applist.applistpage.viewmodel;

public abstract class BaseItem {

    private long mId;
    private String mName;
    private boolean mIsEnabled = true;
    private boolean mIsHighlighted = false;
    private boolean mIsSelected = false;
    private boolean mIsDragged = false;

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

    public boolean isDragged() {
        return mIsDragged;
    }

    public void setDragged(boolean dragged) {
        mIsDragged = dragged;
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
