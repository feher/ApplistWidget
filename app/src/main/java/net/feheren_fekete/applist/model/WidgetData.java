package net.feheren_fekete.applist.model;

public class WidgetData {

    private String mProviderPackage;
    private String mProviderClass;
    private int mPositionX; // dp
    private int mPositionY; // dp
    private int mWidth; // dp
    private int mHeight; // dp

    public WidgetData(String providerPackage, String providerClass, int positionX, int positionY, int width, int height) {
        mProviderPackage = providerPackage;
        mProviderClass = providerClass;
        mPositionX = positionX;
        mPositionY = positionY;
        mWidth = width;
        mHeight = height;
    }

    public WidgetData(WidgetData other) {
        mProviderPackage = other.mProviderPackage;
        mProviderClass = other.mProviderClass;
        mPositionX = other.mPositionX;
        mPositionY = other.mPositionY;
        mWidth = other.mWidth;
        mHeight = other.mHeight;
    }

    public String getProviderPackage() {
        return mProviderPackage;
    }

    public String getProviderClass() {
        return mProviderClass;
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
}
