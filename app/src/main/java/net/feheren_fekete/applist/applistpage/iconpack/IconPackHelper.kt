package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import net.feheren_fekete.applist.applistpage.iconpack.builtinpacks.IconPackLoader
import net.feheren_fekete.applist.applistpage.iconpack.builtinpacks.NormalIconPackLoader
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp
import net.feheren_fekete.applist.utils.ImageUtils
import net.feheren_fekete.applist.utils.ScreenUtils

class IconPackHelper(
    context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils,
    screenUtils: ScreenUtils
) {

    private val normalLoader =
        NormalIconPackLoader(context, packageManager, imageUtils, screenUtils)

    fun getSupportedApps(iconPackPackageName: String): Flow<IconPackApp> {
        val loader = getLoader(iconPackPackageName)
        return loader.getSupportedApps(iconPackPackageName)
    }

    fun getIconDrawableNames(iconPackPackageName: String): Flow<String> {
        val loader = getLoader(iconPackPackageName)
        return loader.getIconDrawableNames(iconPackPackageName)
    }

    fun loadIcon(
        iconPackPackageName: String,
        drawableName: String
    ): Bitmap? {
        val loader = getLoader(iconPackPackageName)
        return loader.loadIcon(iconPackPackageName, drawableName)
    }

    fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int = IconPackLoader.DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = IconPackLoader.DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap {
        val loader = getLoader(iconPackPackageName)
        return loader.loadIconWithFallback(
            iconPackPackageName, componentName,
            originalIcon,
            fallbackIconWidthDp, fallbackIconHeightDp
        )
    }

    fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int = IconPackLoader.DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = IconPackLoader.DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap {
        val loader = getLoader(iconPackPackageName)
        return loader.loadIconWithFallback(
            iconPackPackageName, componentName,
            fallbackIconWidthDp, fallbackIconHeightDp
        )
    }

    private fun getLoader(iconPackPackageName: String): IconPackLoader {
        return normalLoader
    }

}
