package net.feheren_fekete.applist.launcher;

import java.util.Map;

import androidx.collection.ArrayMap;

public class LauncherStateManager {

    private Map<Long, Boolean> mPageVisibility = new ArrayMap<>();

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
