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
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.lang.Exception

class IconLoaderTask(private val appItem: AppItem,
                     startableItemHolder: ApplistAdapter.StartableItemHolder,
                     private val packageManager: PackageManager,
                     iconCache: IconCache,
                     iconCacheDirPath: String) : AsyncTask<Void, Void, Bitmap>(), KoinComponent {

    private val iconPackHelper: IconPackHelper by inject()

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
            val originalIcon = loadIconFromApp(componentName)
            val iconPackPackageName = "com.natewren.radpackfree"
            //val iconPackPackageName = "com.natewren.linesfree"
            return loadIconFromIconPack(componentName, iconPackPackageName, originalIcon)

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

    private fun loadIconFromApp(componentName: ComponentName): Bitmap {
        val iconDrawable = packageManager.getActivityIcon(componentName)
        return ImageUtils.drawableToBitmap(iconDrawable)
    }

    private fun loadIconFromIconPack(componentName: ComponentName,
                                     iconPackPackageName: String,
                                     originalIcon: Bitmap): Bitmap {
        return try {
            iconPackHelper.loadIcon(packageManager, iconPackPackageName, componentName)
                    ?: iconPackHelper.createFallbackIcon(packageManager, iconPackPackageName, 100, 100, originalIcon)
        } catch (e: Exception) {
            // Do not log. It may generate too many logs (for every single icon failing) in case of
            // a broken (or missing) icon pack.
            originalIcon
        }
    }
}
