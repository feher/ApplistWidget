package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.opengl.GLES20
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.ImageUtils

class ColorizeIconPackLoader(
    private val context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
) : EffectIconPackLoader(context, packageManager, imageUtils) {

    init {
        parameter = DEFAULT_PARAM // 0..1
    }

    override fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setFilter(ColorizeFilter().apply { setParameter(parameter) })
        return gpuImage.getBitmapWithFilterApplied(originalIcon)
    }

    override fun parameterToFloat(intParameter: Int): Float {
        return intParameter.toFloat() / 100.0f
    }

    override fun isEditable() = true

    override fun getEditableParameter(): Int {
        return (parameter * 100.0f).toInt()
    }

    private class ColorizeFilter : GPUImageFilter(
        NO_FILTER_VERTEX_SHADER,
        """
            precision mediump float;
            varying vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            uniform float parameter;

            vec3 rgb2hsv(vec3 rgb) {
                float Cmax = max(rgb.r, max(rgb.g, rgb.b));
                float Cmin = min(rgb.r, min(rgb.g, rgb.b));
                float delta = Cmax - Cmin;
            
                vec3 hsv = vec3(0.0, 0.0, Cmax);
            
                if (Cmax > Cmin) {
                    hsv.y = delta / Cmax;
            
                    if (rgb.r == Cmax)
                        hsv.x = (rgb.g - rgb.b) / delta;
                    else {
                        if (rgb.g == Cmax)
                            hsv.x = 2.0 + (rgb.b - rgb.r) / delta;
                        else
                            hsv.x = 4.0 + (rgb.r - rgb.g) / delta;
                    }
                    hsv.x = fract(hsv.x / 6.0);
                }
                return hsv;
            }
            
            vec3 hsv2rgb(vec3 c) {
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
            }

            void main() {
                vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
                vec3 hsv = rgb2hsv(textureColor.rgb);
                hsv.x = parameter;
                hsv.y = hsv.y; /* * 0.7; */
                hsv.z = hsv.z * 0.8;
                vec3 color = hsv2rgb(hsv);
                gl_FragColor = vec4(color, textureColor.a);
            }
        """
    ) {
        private var parameter = DEFAULT_PARAM
        private var parameterLocation = 0

        override fun onInit() {
            super.onInit()
            parameterLocation = GLES20.glGetUniformLocation(program, "parameter")
        }

        override fun onInitialized() {
            super.onInitialized()
            setParameter(parameter)
        }

        fun setParameter(param: Float) {
            parameter = param
            setFloat(parameterLocation, param)
        }
    }

    companion object {
        const val name = "colorize"
        const val displayName = "Mono Color"
        const val displayIconId = R.drawable.icon_effect_monocolor

        private const val MIN_PARAM = 0.0f
        private const val MAX_PARAM = 1.0f
        private const val DEFAULT_PARAM = 0.12f
    }

}
