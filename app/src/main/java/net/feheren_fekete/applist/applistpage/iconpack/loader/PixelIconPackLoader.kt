package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class PixelIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): EffectIconPackLoader(context, packageManager, imageUtils) {

    init {
        parameter = MIN_PARAM
    }

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(PixelFilter().apply { setPercent(parameter) })
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun parameterToFloat(intParameter: Int): Float {
        val percentage = intParameter.toFloat() / 100.0f
        return MIN_PARAM + (percentage * (MAX_PARAM - MIN_PARAM))
    }

    override fun isEditable() = true

    override fun getEditableParameter(): Int {
        return ((parameter - MIN_PARAM) / (MAX_PARAM - MIN_PARAM) * 100.0f).toInt()
    }

    private class PixelFilter: GPUImageFilter(
        NO_FILTER_VERTEX_SHADER,
        """
            precision highp float;
            varying vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            uniform float percent;
            void main()
            {
              vec2 uv  = textureCoordinate.xy;
              float dx = percent;
              float dy = percent;
              vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
              vec4 tc = texture2D(inputImageTexture, coord).rgba;
              gl_FragColor = tc;
            }
            """
    ) {
        private var imageWidthFactorLocation = 0
        private var imageHeightFactorLocation = 0
        private var percent = 0.1f
        private var percentLocation = 0

        override fun onInit() {
            super.onInit()
            percentLocation = GLES20.glGetUniformLocation(program, "percent")
        }

        override fun onInitialized() {
            super.onInitialized()
            setPercent(percent)
        }

        fun setPercent(pixel: Float) {
            this.percent = pixel
            setFloat(percentLocation, this.percent)
        }
    }

    companion object {
        const val name = "pixel"
        const val displayName = "Pixel"
        const val displayIconId = R.drawable.icon_effect_pixel

        private const val MIN_PARAM = 0.05f
        private const val MAX_PARAM = 0.14f
    }

}
