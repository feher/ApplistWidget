package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class SepiaIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): EffectIconPackLoader(context, packageManager, imageUtils) {

    private var parameter = 0.5f // 0..2

    override fun applyEffect(originalIcon: Bitmap): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(GPUImageSepiaToneFilter(parameter))
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun isEditable() = true

    override fun setEditableParameter(parameterValue: Int) {
        val percentage = parameterValue.toFloat() / 100.0f
        val value = 2.0f * percentage
        parameter = value
    }

    override fun getEditableParameter(): Int {
        return ((parameter / 2.0f) * 100.0f).toInt()
    }

    companion object {
        const val name = "sepia"
        const val displayName = "Sepia"
        const val displayIconId = R.drawable.ic_apps
    }

}
