package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import net.feheren_fekete.applist.utils.ImageUtils
import java.lang.NumberFormatException

abstract class EffectIconPackLoader(
    context: Context,
    packageManager: PackageManager,
    imageUtils: ImageUtils
): BuiltinIconPackLoader(context, packageManager, imageUtils) {

    protected var parameter = 0.0f

    override fun loadIcon(iconPackPackageName: String, drawableName: String): Bitmap? {
        val parameter = getParameter(iconPackPackageName)
        val componentName = ComponentName.unflattenFromString(drawableName) ?: return null
        val originalIcon = loadOriginalAppIcon(componentName)
        return applyEffect(originalIcon, parameter)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        val parameter = getParameter(iconPackPackageName)
        return applyEffect(originalIcon, parameter)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        val parameter = getParameter(iconPackPackageName)
        val originalIcon = loadOriginalAppIcon(componentName)
        return applyEffect(originalIcon, parameter)
    }

    override fun setEditableParameter(parameterValue: Int) {
        parameter = parameterToFloat(parameterValue)
    }

    protected abstract fun applyEffect(originalIcon: Bitmap, parameter: Float): Bitmap

    protected open fun parameterToFloat(intParameter: Int) = intParameter.toFloat()

    private fun getParameter(iconPackPackageName: String): Float {
        val paramString = getBuiltinIconPackParameter(iconPackPackageName)
        return if (paramString.isNotEmpty()) {
            try {
                parameterToFloat(paramString.toInt())
            } catch (e: NumberFormatException) {
                parameter
            }
        } else {
            parameter
        }
    }

}
