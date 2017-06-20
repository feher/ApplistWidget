package net.feheren_fekete.applist.launcherpage.model;

public class WidgetData {

    private long mId;
    private int mAppWidgetId;
    private String mProviderPackage;
    private String mProviderClass;
    private int mPageNumber;
    private int mPositionX; // dp
    private int mPositionY; // dp
    private int mWidth; // dp
    private int mHeight; // dp

    public WidgetData(long id,
                      int appWidgetId,
                      String providerPackage, String providerClass,
                      int pageNumber,
                      int positionX, int positionY,
                      int width, int height) {
        mId = id;
        mAppWidgetId = appWidgetId;
        mProviderPackage = providerPackage;
        mProviderClass = providerClass;
        mPageNumber = pageNumber;
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
        mPageNumber = other.mPageNumber;
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

    public int getPageNumber() {
        return mPageNumber;
    }

    public void setPageNumber(int pageNumber) {
        mPageNumber = pageNumber;
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
                + ", page " + mPageNumber
                + ", posX " + mPositionX
                + ", posY " + mPositionY
                + ", width " + mWidth
                + ", height " + mHeight;
    }
}
