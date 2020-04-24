package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo
import net.feheren_fekete.applist.utils.ImageUtils

abstract class IconPackLoader(
    private val packageManager: PackageManager,
    private val imageUtils: ImageUtils
) {

    private var parameterValue = 0

    abstract fun getSupportedApps(iconPackPackageName: String): Flow<IconPackApp>
    abstract fun getIconDrawableNames(iconPackPackageName: String): Flow<String>

    abstract fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap?

    abstract fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int = DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap

    abstract fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int = DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap

    open fun isEditable() = false

    open fun showEditDialog() {
        // Nothing
    }

    /**
     * @param parameterValue Between 0 and 100
     */
    open fun setEditableParameter(parameterValue: Int) {
        // Nothing
    }

    /**
     * @return Value between 0 and 100
     */
    open fun getEditableParameter() = 0

    protected fun loadOriginalAppIcon(componentName: ComponentName): Bitmap {
        return imageUtils.drawableToBitmap(loadOriginalAppIconDrawable(componentName))
    }

    private fun loadOriginalAppIconDrawable(componentName: ComponentName) =
        packageManager.getActivityInfo(componentName, 0)
            .applicationInfo
            .loadIcon(packageManager)

    companion object {
        const val DEFAULT_FALLBACK_ICON_SIZE = 48
        private const val ICONPACK_LOADER_SCHEMA = "applisticonpack"

        fun createBuiltinPackageNameWithParam(iconPackPackageName: String, parameter: Int) =
            "$iconPackPackageName?param=$parameter"

        fun getBuiltinIconPackParameter(iconPackPackageName: String) =
            Uri.parse(iconPackPackageName).getQueryParameter("param") ?: ""

        fun createIconPackInfo(displayName: String, displayIconId: Int, iconPackLoaderName: String) =
            IconPackInfo(
                displayName,
                ComponentName("$ICONPACK_LOADER_SCHEMA://$iconPackLoaderName", "$displayIconId"))

        fun isBuiltinIconPack(iconPackPackageName: String): Boolean {
            return iconPackPackageName.startsWith("$ICONPACK_LOADER_SCHEMA://")
        }

        fun getBuiltinIconPackLoaderName(iconPackPackageName: String) =
            Uri.parse(iconPackPackageName).authority!!

    }

}
