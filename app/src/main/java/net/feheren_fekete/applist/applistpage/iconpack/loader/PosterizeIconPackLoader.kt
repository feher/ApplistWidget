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

    init {
        parameter = 1.0f // 1..10
    }

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(GPUImagePosterizeFilter(parameter.toInt()))
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun parameterToFloat(intParameter: Int): Float {
        val percentage = intParameter.toFloat() / 100.0f
        return max(1.0f, 10.0f * percentage)
    }

    override fun isEditable() = true

    override fun getEditableParameter(): Int {
        return ((parameter / 10.0f) * 100.0f).toInt()
    }

    companion object {
        const val name = "poster"
        const val displayName = "Poster"
        const val displayIconId = R.drawable.icon_effect_poster
    }

}
