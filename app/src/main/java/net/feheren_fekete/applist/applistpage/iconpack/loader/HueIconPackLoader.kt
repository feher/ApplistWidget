package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageCGAColorspaceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class HueIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): EffectIconPackLoader(context, packageManager, imageUtils) {

    override fun applyEffect(originalIcon: Bitmap): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(GPUImageHueFilter(90.0f))
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun showEditDialog() {
        // Nothing
    }

    companion object {
        const val name = "hue"
        const val displayName = "Hue"
        const val displayIconId = R.drawable.ic_apps
    }

}
