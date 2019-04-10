package net.feheren_fekete.applist.applistpage

import android.graphics.Bitmap

import net.feheren_fekete.applist.applistpage.viewmodel.AppItem

import androidx.collection.LruCache

class IconCache {
    private val cache: LruCache<String, Bitmap>

    init {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

        // Use 1/8th of the available memory for this memory cache.
        val cacheSize = maxMemory / 8

        cache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.byteCount / 1024
            }
        }
    }

    fun addIcon(key: String, bitmap: Bitmap) {
        if (getIcon(key) == null) {
            cache.put(key, bitmap)
        }
    }

    fun getIcon(key: String): Bitmap? {
        return cache.get(key)
    }

    fun createKey(appItem: AppItem): String {
        return appItem.packageName + "::" + appItem.className
    }
}
