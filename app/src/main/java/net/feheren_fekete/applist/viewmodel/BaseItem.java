package net.feheren_fekete.applist.viewmodel;

public abstract class BaseItem {

    public static final int NONE = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private long mId;
    private boolean mIsEnabled = true;
    private boolean mIsDraggedOverLeft = false;
    private boolean mIsDraggedOverRight = false;

    public BaseItem(long id) {
        mId = id;
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

    public boolean isDraggedOverLeft() {
        return mIsDraggedOverLeft;
    }

    public boolean isDraggedOverRight() {
        return mIsDraggedOverRight;
    }

    public void setDraggedOver(int side) {
        switch (side) {
            case LEFT:
                mIsDraggedOverLeft = true;
                mIsDraggedOverRight = false;
                break;
            case RIGHT:
                mIsDraggedOverLeft = false;
                mIsDraggedOverRight = true;
                break;
            case NONE:
                mIsDraggedOverLeft = false;
                mIsDraggedOverRight = false;
                break;
        }
    }

    public abstract String getName();
}
