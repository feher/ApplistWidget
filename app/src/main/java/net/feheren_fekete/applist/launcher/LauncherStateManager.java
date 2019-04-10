package net.feheren_fekete.applist.launcher;

import java.util.Map;

import androidx.collection.ArrayMap;

public class LauncherStateManager {

    private Map<Long, Boolean> mPageVisibility = new ArrayMap<>();

    private static LauncherStateManager sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            sInstance = new LauncherStateManager();
        }
    }

    public static LauncherStateManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(LauncherStateManager.class.getSimpleName() + " singleton is not initialized yet");
        }
    }

    private LauncherStateManager() {
    }

    public void setPageVisibile(long pageId, boolean visible) {
        mPageVisibility.put(pageId, visible);
    }

    public boolean isPageVisible(long pageId) {
        Boolean result = mPageVisibility.get(pageId);
        return (result == null) ? false : result;
    }

    public void clearPageVisible(long pageId) {
        mPageVisibility.remove(pageId);
    }

}
