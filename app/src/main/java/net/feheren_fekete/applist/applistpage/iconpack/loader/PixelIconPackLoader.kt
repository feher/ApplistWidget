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
        parameter = 15.0f
    }

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(PixelFilter().apply { setPixel(parameter) })
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun parameterToFloat(intParameter: Int): Float {
        val percentage = intParameter.toFloat() / 100.0f
        return 30.0f * percentage
    }

    override fun isEditable() = true

    override fun getEditableParameter(): Int {
        return ((parameter / 30.0f) * 100.0f).toInt()
    }

    private class PixelFilter: GPUImageFilter(
        NO_FILTER_VERTEX_SHADER,
        """
            precision highp float;
            varying vec2 textureCoordinate;
            uniform float imageWidthFactor;
            uniform float imageHeightFactor;
            uniform sampler2D inputImageTexture;
            uniform float pixel;
            void main()
            {
              vec2 uv  = textureCoordinate.xy;
              float dx = pixel * imageWidthFactor;
              float dy = pixel * imageHeightFactor;
              vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
              vec4 tc = texture2D(inputImageTexture, coord).rgba;
              gl_FragColor = tc;
            }
            """
    ) {
        private var imageWidthFactorLocation = 0
        private var imageHeightFactorLocation = 0
        private var pixel = 1.0f
        private var pixelLocation = 0

        override fun onInit() {
            super.onInit()
            imageWidthFactorLocation = GLES20.glGetUniformLocation(program, "imageWidthFactor")
            imageHeightFactorLocation =
                GLES20.glGetUniformLocation(program, "imageHeightFactor")
            pixelLocation = GLES20.glGetUniformLocation(program, "pixel")
        }

        override fun onInitialized() {
            super.onInitialized()
            setPixel(pixel)
        }

        override fun onOutputSizeChanged(width: Int, height: Int) {
            super.onOutputSizeChanged(width, height)
            setFloat(imageWidthFactorLocation, 1.0f / width)
            setFloat(imageHeightFactorLocation, 1.0f / height)
        }

        fun setPixel(pixel: Float) {
            this.pixel = pixel
            setFloat(pixelLocation, this.pixel)
        }
    }

    companion object {
        const val name = "pixel"
        const val displayName = "Pixel"
        const val displayIconId = R.drawable.ic_apps
    }

}
