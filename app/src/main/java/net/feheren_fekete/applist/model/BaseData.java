package net.feheren_fekete.applist.model;

public abstract class BaseData {

    private long mId;

    public BaseData(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }
}
