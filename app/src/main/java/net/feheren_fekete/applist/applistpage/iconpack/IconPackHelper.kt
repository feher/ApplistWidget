package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import net.feheren_fekete.applist.applistpage.iconpack.loader.*
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp
import net.feheren_fekete.applist.utils.ImageUtils
import net.feheren_fekete.applist.utils.ScreenUtils

class IconPackHelper(
    private val context: Context,
    private val packageManager: PackageManager,
    private val imageUtils: ImageUtils,
    private val screenUtils: ScreenUtils
) {

    private val loaders = HashMap<String, IconPackLoader>()

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

    fun showEditDailog(iconPackPackageName: String) {
        getLoader(iconPackPackageName).showEditDialog()
    }

    fun isEditable(iconPackPackageName: String) = getLoader(iconPackPackageName).isEditable()

    fun setEditableParameter(iconPackPackageName: String, parameterValue: Int) {
        getLoader(iconPackPackageName).setEditableParameter(parameterValue)
    }

    fun getEditableParameter(iconPackPackageName: String) =
        getLoader(iconPackPackageName).getEditableParameter()

    fun createFullPackageName(iconPackPackageName: String) =
        if (IconPackLoader.isBuiltinIconPack(iconPackPackageName)) {
            IconPackLoader.createBuiltinPackageNameWithParam(
                iconPackPackageName,
                getLoader(iconPackPackageName).getEditableParameter()
            )
        } else {
            iconPackPackageName
        }

    fun releaseLoaders() {
        loaders.clear()
    }

    private fun getLoader(iconPackPackageName: String): IconPackLoader {
        val loaderName = if (IconPackLoader.isBuiltinIconPack(iconPackPackageName)) {
            IconPackLoader.getBuiltinIconPackLoaderName(iconPackPackageName)
        } else {
            ApkIconPackLoader.name
        }
        var loader = loaders[loaderName]
        if (loader == null) {
            when (loaderName) {
                ApkIconPackLoader.name -> loader =
                    ApkIconPackLoader(context, packageManager, imageUtils, screenUtils)
                GrayscaleIconPackLoader.name -> loader =
                    GrayscaleIconPackLoader(context, packageManager, imageUtils)
                ColorizeIconPackLoader.name -> loader =
                    ColorizeIconPackLoader(context, packageManager, imageUtils)
                SketchIconPackLoader.name -> loader =
                    SketchIconPackLoader(context, packageManager, imageUtils)
                SepiaIconPackLoader.name -> loader =
                    SepiaIconPackLoader(context, packageManager, imageUtils)
                PixelIconPackLoader.name -> loader =
                    PixelIconPackLoader(context, packageManager, imageUtils)
                ToonIconPackLoader.name -> loader =
                    ToonIconPackLoader(context, packageManager, imageUtils)
                PosterizeIconPackLoader.name -> loader =
                    PosterizeIconPackLoader(context, packageManager, imageUtils)
                KuwaharaIconPackLoader.name -> loader =
                    KuwaharaIconPackLoader(context, packageManager, imageUtils)
                CgaIconPackLoader.name -> loader =
                    CgaIconPackLoader(context, packageManager, imageUtils)
                HueIconPackLoader.name -> loader =
                    HueIconPackLoader(context, packageManager, imageUtils)
                else -> loader = DefaultIconPackLoader(context, packageManager, imageUtils)
            }
            loaders[loaderName] = loader
        }
        return loader
    }

}
