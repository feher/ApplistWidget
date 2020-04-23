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

abstract class EffectIconPackLoader(
    context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): BuiltinIconPackLoader(context, packageManager, imageUtils) {

    override fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap? {
        val param = getEffectParameter(iconPackPackageName)
        val componentName = ComponentName.unflattenFromString(drawableName) ?: return null
        val originalIcon = loadOriginalAppIcon(componentName)
        return applyEffect(originalIcon)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        return applyEffect(originalIcon)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        val originalIcon = loadOriginalAppIcon(componentName)
        return applyEffect(originalIcon)
    }

    protected abstract fun applyEffect(originalIcon: Bitmap): Bitmap

    protected fun getEffectParameter(iconPackPackageName: String) =
        Uri.parse(iconPackPackageName).getQueryParameter("param") ?: ""

}
