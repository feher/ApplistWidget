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

    private var parameter = 90.0f // 0..360

    override fun applyEffect(originalIcon: Bitmap): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(GPUImageHueFilter(parameter))
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun isEditable() = true

    override fun setEditableParameter(parameterValue: Int) {
        val percentage = parameterValue.toFloat() / 100.0f
        val value = 360.0f * percentage
        parameter = value
    }

    override fun getEditableParameter(): Int {
        return ((parameter / 360.0f) * 100.0f).toInt()
    }

    companion object {
        const val name = "hue"
        const val displayName = "Hue"
        const val displayIconId = R.drawable.ic_apps
    }

}
