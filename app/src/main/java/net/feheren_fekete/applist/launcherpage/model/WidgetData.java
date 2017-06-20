package net.feheren_fekete.applist.launcherpage.model;

public class WidgetData {

    private long mId;
    private int mAppWidgetId;
    private String mProviderPackage;
    private String mProviderClass;
    private long mPageId;
    private int mPositionX; // dp
    private int mPositionY; // dp
    private int mWidth; // dp
    private int mHeight; // dp

    public WidgetData(long id,
                      int appWidgetId,
                      String providerPackage, String providerClass,
                      long pageId,
                      int positionX, int positionY,
                      int width, int height) {
        mId = id;
        mAppWidgetId = appWidgetId;
        mProviderPackage = providerPackage;
        mProviderClass = providerClass;
        mPageId = pageId;
        mPositionX = positionX;
        mPositionY = positionY;
        mWidth = width;
        mHeight = height;
    }

    public WidgetData(WidgetData other) {
        updateFrom(other);
    }

    public void updateFrom(WidgetData other) {
        mId = other.mId;
        mAppWidgetId = other.mAppWidgetId;
        mProviderPackage = other.mProviderPackage;
        mProviderClass = other.mProviderClass;
        mPageId = other.mPageId;
        mPositionX = other.mPositionX;
        mPositionY = other.mPositionY;
        mWidth = other.mWidth;
        mHeight = other.mHeight;
    }

    public long getId() {
        return mId;
    }

    public int getAppWidgetId() {
        return mAppWidgetId;
    }

    public String getProviderPackage() {
        return mProviderPackage;
    }

    public String getProviderClass() {
        return mProviderClass;
    }

    public long getPageId() {
        return mPageId;
    }

    public void setPageId(int pageId) {
        mPageId = pageId;
    }

    public int getPositionX() {
        return mPositionX;
    }

    public void setPositionX(int positionX) {
        mPositionX = positionX;
    }

    public int getPositionY() {
        return mPositionY;
    }

    public void setPositionY(int positionY) {
        mPositionY = positionY;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    @Override
    public String toString() {
        return "id " + mId
                + ", appWidgetId " + mAppWidgetId
                + ", package " + mProviderPackage
                + ", class " + mProviderClass
                + ", pageId " + mPageId
                + ", posX " + mPositionX
                + ", posY " + mPositionY
                + ", width " + mWidth
                + ", height " + mHeight;
    }
}
