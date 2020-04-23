package net.feheren_fekete.applist.applistpage.iconpack.builtinpacks

import android.content.ComponentName
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp

class ColorIconPackLoader: IconPackLoader {
    override fun getSupportedApps(iconPackPackageName: String): Flow<IconPackApp> {
        TODO("Not yet implemented")
    }

    override fun getIconDrawableNames(iconPackPackageName: String): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        TODO("Not yet implemented")
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        TODO("Not yet implemented")
    }

}
