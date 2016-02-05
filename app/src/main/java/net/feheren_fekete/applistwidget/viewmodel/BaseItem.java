package net.feheren_fekete.applistwidget.viewmodel;

public abstract class BaseItem {
    private long mId;

    public BaseItem(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public abstract String getName();
}
