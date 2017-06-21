package net.feheren_fekete.applist.launcher.model;

public class PageData {

    public static final int TYPE_APPLIST_PAGE = 1;
    public static final int TYPE_WIDGET_PAGE = 2;

    private long mId;
    private int mType;
    private boolean mIsMainPage;

    public PageData(long id, int type, boolean isMainPage) {
        mId = id;
        mType = type;
        mIsMainPage = isMainPage;
    }

    public PageData(PageData other) {
        this(other.mId, other.mType, other.mIsMainPage);
    }

    public long getId() {
        return mId;
    }

    public int getType() {
        return mType;
    }

    public boolean isMainPage() {
        return mIsMainPage;
    }

    public void setMainPage(boolean mainPage) {
        mIsMainPage = mainPage;
    }

}
