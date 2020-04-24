package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp
import net.feheren_fekete.applist.utils.AppUtils
import net.feheren_fekete.applist.utils.ImageUtils

abstract class BuiltinIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): IconPackLoader(packageManager, imageUtils) {

    override fun getSupportedApps(iconPackPackageName: String) = flow {
        AppUtils.getInstalledApps(context).forEach {
            val componentName = ComponentName(it.packageName, it.className)
            emit(IconPackApp(componentName, drawableName = componentName.flattenToString()))
        }
    }

    override fun getIconDrawableNames(iconPackPackageName: String) = emptyFlow<String>()

}
