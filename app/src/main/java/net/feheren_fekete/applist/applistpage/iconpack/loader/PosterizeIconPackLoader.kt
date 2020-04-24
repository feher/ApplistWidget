package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils
import kotlin.math.max

class PosterizeIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): EffectIconPackLoader(context, packageManager, imageUtils) {

    private var parameter = 1.0f // 1..50

    override fun applyEffect(originalIcon: Bitmap): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(GPUImagePosterizeFilter(parameter.toInt()))
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun isEditable() = true

    override fun setEditableParameter(parameterValue: Int) {
        val percentage = parameterValue.toFloat() / 100.0f
        val value = max(1.0f, 50.0f * percentage)
        parameter = value
    }

    override fun getEditableParameter(): Int {
        return ((parameter / 50.0f) * 100.0f).toInt()
    }

    companion object {
        const val name = "poster"
        const val displayName = "Poster"
        const val displayIconId = R.drawable.ic_apps
    }

}
