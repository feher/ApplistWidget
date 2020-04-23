package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import net.feheren_fekete.applist.utils.ImageUtils

class DefaultIconPackLoader(
    context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): BuiltinIconPackLoader(context, packageManager, imageUtils) {

    override fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap? {
        val componentName = ComponentName.unflattenFromString(drawableName) ?: return null
        return loadOriginalAppIcon(componentName)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        return originalIcon
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        return loadOriginalAppIcon(componentName)
    }

    override fun showEditDialog() {
        // Nothing
    }

}
