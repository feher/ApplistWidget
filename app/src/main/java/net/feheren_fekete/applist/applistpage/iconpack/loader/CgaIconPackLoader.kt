package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class CgaIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
) : EffectIconPackLoader(context, packageManager, imageUtils) {

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(CgaFilter())
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    private class CgaFilter : GPUImageFilter(
        NO_FILTER_VERTEX_SHADER,
        """
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        void main()
        {
            highp vec2 sampleDivisor = vec2(1.0 / 200.0, 1.0 / 320.0);
            //highp vec4 colorDivisor = vec4(colorDepth);

            highp vec2 samplePos = textureCoordinate - mod(textureCoordinate, sampleDivisor);
            highp vec4 color = texture2D(inputImageTexture, samplePos );

            //gl_FragColor = texture2D(inputImageTexture, samplePos );
            mediump vec4 colorCyan = vec4(85.0 / 255.0, 1.0, 1.0, 1.0);
            mediump vec4 colorMagenta = vec4(1.0, 85.0 / 255.0, 1.0, 1.0);
            mediump vec4 colorWhite = vec4(1.0, 1.0, 1.0, 1.0);
            mediump vec4 colorBlack = vec4(0.0, 0.0, 0.0, 1.0);

            mediump vec4 endColor;
            highp float blackDistance = distance(color, colorBlack);
            highp float whiteDistance = distance(color, colorWhite);
            highp float magentaDistance = distance(color, colorMagenta);
            highp float cyanDistance = distance(color, colorCyan);

            mediump vec4 finalColor;

            highp float colorDistance = min(magentaDistance, cyanDistance);
            colorDistance = min(colorDistance, whiteDistance);
            colorDistance = min(colorDistance, blackDistance);

            if (colorDistance == blackDistance) {
                finalColor = colorBlack;
            } else if (colorDistance == whiteDistance) {
                finalColor = colorWhite;
            } else if (colorDistance == cyanDistance) {
                finalColor = colorCyan;
            } else {
                finalColor = colorMagenta;
            }
            finalColor.a = color.a;
            gl_FragColor = finalColor;
        }
        """
    )

    companion object {
        const val name = "cga"
        const val displayName = "CGA"
        const val displayIconId = R.drawable.icon_effect_cga
    }

}
