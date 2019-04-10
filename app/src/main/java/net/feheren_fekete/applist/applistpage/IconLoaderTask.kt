package net.feheren_fekete.applist.applistpage

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask

import net.feheren_fekete.applist.utils.ImageUtils
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem

import java.io.File
import java.lang.ref.WeakReference

import bolts.Task

class IconLoaderTask(private val appItem: AppItem,
                     startableItemHolder: ApplistAdapter.StartableItemHolder,
                     private val packageManager: PackageManager,
                     iconCache: IconCache,
                     iconCacheDirPath: String) : AsyncTask<Void, Void, Bitmap>() {

    private val appItemHolderRef = WeakReference(startableItemHolder)
    private val iconCacheRef = WeakReference(iconCache)
    private val cachedIconPath = "$iconCacheDirPath${File.separator}${appItem.packageName}_${appItem.className}.png"

    fun isLoadingFor(item: AppItem): Boolean {
        return appItem == item
    }

    override fun doInBackground(vararg params: Void): Bitmap? {
        try {
            if (isCancelled) {
                return null
            }

            val cachedIconBitmap = ImageUtils.loadBitmap(cachedIconPath)
            if (cachedIconBitmap != null) {
                return cachedIconBitmap
            }

            val componentName = ComponentName(appItem.packageName, appItem.className)
            val iconDrawable = packageManager.getActivityIcon(componentName)
            if (isCancelled) {
                return null
            }

            val iconBitmap = ImageUtils.drawableToBitmap(iconDrawable)
            return iconBitmap
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

    }

    override fun onPostExecute(iconBitmap: Bitmap?) {
        if (iconBitmap == null) {
            return
        }

        val startableItemHolder = appItemHolderRef.get() ?: return
        val iconCache = iconCacheRef.get() ?: return

        if (!isStillValid()) {
            return
        }

        val key = iconCache.createKey(appItem)
        iconCache.addIcon(key, iconBitmap)

        Task.callInBackground {
            ImageUtils.saveBitmap(iconBitmap, cachedIconPath)
            null
        }

        startableItemHolder.appIcon.setBackgroundColor(Color.TRANSPARENT)
        startableItemHolder.appIcon.setImageBitmap(iconBitmap)
        startableItemHolder.iconLoader = null
    }

    private fun isStillValid(): Boolean {
        val startableItemHolder = appItemHolderRef.get()
        return (!isCancelled
                && startableItemHolder != null
                && startableItemHolder.iconLoader === this)
    }
}
