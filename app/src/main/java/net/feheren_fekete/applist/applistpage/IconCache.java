package net.feheren_fekete.applist.applistpage;

import android.graphics.Bitmap;

import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;

import androidx.collection.LruCache;

public class IconCache {
    private LruCache<String, Bitmap> mCache;

    public IconCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addIcon(String key, Bitmap bitmap) {
        if (getIcon(key) == null) {
            mCache.put(key, bitmap);
        }
    }

    public Bitmap getIcon(String key) {
        return mCache.get(key);
    }

    public String createKey(AppItem appItem) {
        return appItem.getPackageName() + "::" + appItem.getClassName();
    }
}
