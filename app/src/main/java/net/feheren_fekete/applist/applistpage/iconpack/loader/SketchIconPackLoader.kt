package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImage3x3TextureSamplingFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class SketchIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): EffectIconPackLoader(context, packageManager, imageUtils) {

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(SketchFilter())
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    private class SketchFilter : GPUImageFilterGroup() {
        companion object {
            const val SKETCH_FRAGMENT_SHADER = """
                precision mediump float;
                varying vec2 textureCoordinate;
                varying vec2 leftTextureCoordinate;
                varying vec2 rightTextureCoordinate;
                varying vec2 topTextureCoordinate;
                varying vec2 topLeftTextureCoordinate;
                varying vec2 topRightTextureCoordinate;
                varying vec2 bottomTextureCoordinate;
                varying vec2 bottomLeftTextureCoordinate;
                varying vec2 bottomRightTextureCoordinate;
                uniform sampler2D inputImageTexture;
                void main()
                {
                    vec4 color = texture2D(inputImageTexture, textureCoordinate);
                    float bottomLeftIntensity = texture2D(inputImageTexture, bottomLeftTextureCoordinate).r;
                    float topRightIntensity = texture2D(inputImageTexture, topRightTextureCoordinate).r;
                    float topLeftIntensity = texture2D(inputImageTexture, topLeftTextureCoordinate).r;
                    float bottomRightIntensity = texture2D(inputImageTexture, bottomRightTextureCoordinate).r;
                    float leftIntensity = texture2D(inputImageTexture, leftTextureCoordinate).r;
                    float rightIntensity = texture2D(inputImageTexture, rightTextureCoordinate).r;
                    float bottomIntensity = texture2D(inputImageTexture, bottomTextureCoordinate).r;
                    float topIntensity = texture2D(inputImageTexture, topTextureCoordinate).r;
                    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;
                    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;
                    float mag = 1.0 - length(vec2(h, v));
                    gl_FragColor = vec4(vec3(mag), color.a);
                }
                """
        }

        init {
            addFilter(GPUImageGrayscaleFilter())
            addFilter(GPUImage3x3TextureSamplingFilter(SKETCH_FRAGMENT_SHADER))
        }
    }

    companion object {
        const val name = "sketch"
        const val displayName = "Sketch"
        const val displayIconId = R.drawable.icon_effect_sketch
    }

}
