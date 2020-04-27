package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.opengl.GLES20
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
        parameter = DEFAULT_PARAM
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
              float columnCount = floor(1.0f / dx);
              float dxExtra = (1.0f - columnCount * dx) / columnCount;
              dx += dxExtra;
              dy += dxExtra;
              float dxh = dx / 2.0f;
              float dxq = dx / 4.0f;
              float dx2q = 3.0f * dxq;

              vec2 cellCoord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
              // Middle
              vec4 cellColor = texture2D(inputImageTexture, vec2(cellCoord.x + dxh, cellCoord.y + dxh)).rgba;
              // Left-Top
              cellColor += texture2D(inputImageTexture, vec2(cellCoord.x + dxq, cellCoord.y + dxq)).rgba;
              // Right-Bottom
              cellColor += texture2D(inputImageTexture, vec2(cellCoord.x + dx2q, cellCoord.y + dx2q)).rgba;
              // Right-Top
              cellColor += texture2D(inputImageTexture, vec2(cellCoord.x + dx2q, cellCoord.y + dxq)).rgba;
              // Left-Bottom
              cellColor += texture2D(inputImageTexture, vec2(cellCoord.x + dxq, cellCoord.y + dx2q)).rgba;

              cellColor /= 5.0f;
              gl_FragColor = cellColor;
            }
            """
    ) {
        private var percent = DEFAULT_PARAM
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
        private const val MAX_PARAM = 0.10f
        private const val DEFAULT_PARAM = MIN_PARAM
    }

}
