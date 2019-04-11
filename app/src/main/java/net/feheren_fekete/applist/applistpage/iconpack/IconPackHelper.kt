package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.*
import android.graphics.Paint.FILTER_BITMAP_FLAG
import android.graphics.Bitmap

class IconPackHelper {

    private val uniformOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inScaled = false
        inDither = false
    }

    fun getSupportedApps(packageManager: PackageManager,
                         iconPackPackageName: String): List<ComponentName> {
        val result = mutableListOf<ComponentName>()
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        findInAppfilterXml(iconPackPackageName, iconPackResources) {
            if (it.name == "item"
                    && it.attributeCount >= 2
                    && it.getAttributeName(0) == "component") {
                // We assume the component to be in this format:
                // ComponentInfo{package-name/class-name}
                var componentString = it.getAttributeValue(0)
                if (componentString.startsWith("ComponentInfo{")
                        && componentString.indexOf('/') != -1) {
                    componentString = componentString.drop("ComponentInfo{".length)
                    componentString = componentString.dropLast(1)
                    val componentName = ComponentName.unflattenFromString(componentString)
                    if (componentName != null) {
                        result.add(componentName)
                        return@findInAppfilterXml true
                    }
                }
            }
            return@findInAppfilterXml false
        }
        return result
    }

    fun loadIcon(packageManager: PackageManager,
                 iconPackPackageName: String,
                 componentName: ComponentName): Bitmap? {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return getIconDrawableName(iconPackResources, iconPackPackageName, componentName)?.let {
            return@let loadBitmap(iconPackPackageName, iconPackResources, it)
        }
    }

    fun createFallbackIcon(packageManager: PackageManager,
                           iconPackPackageName: String,
                           width: Int,
                           height: Int,
                           originalIcon: Bitmap): Bitmap {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)

        val icon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val iconCanvas = Canvas(icon)
        val iconPaint = Paint(FILTER_BITMAP_FLAG)
        iconPaint.isAntiAlias = true

        // Draw the background
        getFallbackIconDrawableName(iconPackResources, iconPackPackageName, "iconback")?.let {
            loadBitmap(iconPackPackageName, iconPackResources, it)?.let { backBitmap ->
                iconCanvas.drawBitmap(backBitmap, getResizeMatrix(backBitmap, width, height), iconPaint)
            }
        }

        // Prepare a scaled-down version of the original icon
        val scaledOriginalIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scaledOriginalIconCanvas = Canvas(scaledOriginalIcon)
        val scale = getScaleFactor(iconPackResources, iconPackPackageName)
        val scaledWidth = (width * scale)
        val scaledHeight = (height * scale)
        val m = getResizeMatrix(originalIcon, scaledWidth.toInt(), scaledHeight.toInt())
        m.postTranslate((width - scaledWidth) / 2, (height - scaledHeight) / 2)
        val scaledOriginalPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        scaledOriginalPaint.isAntiAlias = true
        scaledOriginalIconCanvas.drawBitmap(originalIcon, m, scaledOriginalPaint)

        // Apply mask on top of the scaled original
        getFallbackIconDrawableName(iconPackResources, iconPackPackageName, "iconmask")?.let {
            loadBitmap(iconPackPackageName, iconPackResources, it)?.let { maskBitmap ->
                val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                maskPaint.isAntiAlias = true
                maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                scaledOriginalIconCanvas.drawBitmap(maskBitmap, getResizeMatrix(maskBitmap, width, height), maskPaint)
            }
        }

        // Draw the scaled original icon on top of the background
        iconCanvas.drawBitmap(scaledOriginalIcon, 0.0f, 0.0f, iconPaint)

        // Put the "upon" layer on top
        getFallbackIconDrawableName(iconPackResources, iconPackPackageName, "iconupon")?.let {
            loadBitmap(iconPackPackageName, iconPackResources, it)?.let { uponBitmap ->
                iconCanvas.drawBitmap(uponBitmap, getResizeMatrix(uponBitmap, width, height), iconPaint)
            }
        }

        return icon
    }

    private fun loadBitmap(iconPackPackageName: String,
                           iconPackResources: Resources,
                           resourceName: String): Bitmap? {
        val resourceId = iconPackResources.getIdentifier(resourceName, "drawable", iconPackPackageName)
        return BitmapFactory.decodeResource(iconPackResources, resourceId, uniformOptions)
    }

    private fun getIconDrawableName(resources: Resources,
                                    packageName: String,
                                    componentName: ComponentName): String? {
        var result: String? = null
        findInAppfilterXml(packageName, resources) {
            if (it.name == "item"
                    && it.attributeCount >= 2
                    && it.getAttributeName(0) == "component"
                    && it.getAttributeValue(0) == componentName.toString()
                    && it.getAttributeName(1) == "drawable") {
                result = it.getAttributeValue(1)
                return@findInAppfilterXml true
            } else {
                return@findInAppfilterXml false
            }
        }
        return result
    }

    private fun getFallbackIconDrawableName(resources: Resources,
                                            packageName: String,
                                            elementName: String): String? {
        var result: String? = null
        findInAppfilterXml(packageName, resources) {
            if (it.name == elementName
                    && it.attributeCount >= 1
                    && it.getAttributeName(0).startsWith("img")) {
                result = it.getAttributeValue(0)
                return@findInAppfilterXml true
            } else {
                return@findInAppfilterXml false
            }
        }
        return result
    }

    private fun getScaleFactor(resources: Resources,
                               packageName: String): Float {
        var result = 1.0f
        findInAppfilterXml(packageName, resources) {
            if (it.name == "scale"
                    && it.attributeCount >= 1
                    && it.getAttributeName(0) == "factor") {
                result = it.getAttributeValue(0).toFloat()
                return@findInAppfilterXml true
            } else {
                return@findInAppfilterXml false
            }
        }
        return result
    }

    private fun findInAppfilterXml(packageName: String,
                                   resources: Resources,
                                   finder: (XmlResourceParser) -> Boolean) {
        val resourceValue = resources.getIdentifier("appfilter", "xml", packageName)
        if (resourceValue != 0) {
            val xrp = resources.getXml(resourceValue)
            while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                if (xrp.eventType == XmlResourceParser.START_TAG) {
                    if (finder(xrp)) {
                        return
                    }
                }
                xrp.next()
            }
        }
    }

    private fun getResizeMatrix(bm: Bitmap,
                                newHeight: Int,
                                newWidth: Int): Matrix {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return matrix
    }

}
