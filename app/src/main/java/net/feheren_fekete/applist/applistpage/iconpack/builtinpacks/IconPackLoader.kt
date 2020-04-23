package net.feheren_fekete.applist.applistpage.iconpack.builtinpacks

import android.content.ComponentName
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp

interface IconPackLoader {

    fun getSupportedApps(iconPackPackageName: String): Flow<IconPackApp>
    fun getIconDrawableNames(iconPackPackageName: String): Flow<String>

    fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap?

    fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int = DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap

    fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int = DEFAULT_FALLBACK_ICON_SIZE,
        fallbackIconHeightDp: Int = DEFAULT_FALLBACK_ICON_SIZE
    ): Bitmap

    companion object {
        public const val DEFAULT_FALLBACK_ICON_SIZE = 48
    }

}
