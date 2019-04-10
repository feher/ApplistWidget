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

    fun loadIcon(packageManager: PackageManager,
                 iconPackPackageName: String,
                 componentName: ComponentName): Bitmap? {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return loadBitmap(iconPackPackageName, iconPackResources, null, componentName.toString())
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
        val backBitmap = loadBitmap(iconPackPackageName, iconPackResources, "iconback", null)
        if (backBitmap != null) {
            iconCanvas.drawBitmap(backBitmap, getResizeMatrix(backBitmap, width, height), iconPaint)
        }

        // Prepare a scaled-down version of the original icon
        val scaledOriginalIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scaledOriginalIconCanvas = Canvas(scaledOriginalIcon)
        val scaleResourceName = getResourceName(iconPackResources, iconPackPackageName, "scale", null)
        val scale = scaleResourceName?.toFloat() ?: 1.0f
        val scaledWidth = (width * scale)
        val scaledHeight = (height * scale)
        val m = getResizeMatrix(originalIcon, scaledWidth.toInt(), scaledHeight.toInt())
        m.postTranslate((width - scaledWidth) / 2, (height - scaledHeight) / 2)
        val scaledOriginalPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        scaledOriginalPaint.isAntiAlias = true
        scaledOriginalIconCanvas.drawBitmap(originalIcon, m, scaledOriginalPaint)

        // Apply mask on top of the scaled original
        val maskBitmap = loadBitmap(iconPackPackageName, iconPackResources, "iconmask", null)
        if (maskBitmap != null) {
            val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            maskPaint.isAntiAlias = true
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            scaledOriginalIconCanvas.drawBitmap(maskBitmap, getResizeMatrix(maskBitmap, width, height), maskPaint)
        }

        // Draw the scaled original icon on top of the background
        iconCanvas.drawBitmap(scaledOriginalIcon, 0.0f, 0.0f, iconPaint)

        // Put the "upon" layer on top
        val uponBitmap = loadBitmap(iconPackPackageName, iconPackResources, "iconupon", null)
        if (uponBitmap != null) {
            iconCanvas.drawBitmap(uponBitmap, getResizeMatrix(uponBitmap, width, height), iconPaint)
        }

        return icon
    }

    private fun loadBitmap(iconPackPackageName: String,
                           iconPackResources: Resources,
                           resourceName: String?,
                           componentName: String?): Bitmap? {
        val resourceNameInIconPack = getResourceName(
                iconPackResources, iconPackPackageName, resourceName, componentName) ?: return null
        val resourceId = iconPackResources.getIdentifier(resourceNameInIconPack, "drawable", iconPackPackageName)
        return BitmapFactory.decodeResource(iconPackResources, resourceId, uniformOptions)
    }

    private fun getResourceName(resources: Resources,
                                packageName: String,
                                resourceName: String?,
                                componentName: String?): String? {
        val xrp: XmlResourceParser
        var resource: String? = null
        try {
            val resourceValue = resources.getIdentifier("appfilter", "xml", packageName)
            if (resourceValue != 0) {
                xrp = resources.getXml(resourceValue)
                while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                    if (xrp.eventType == 2) {
                        try {
                            val string = xrp.name
                            if (componentName != null) {
                                if (xrp.getAttributeValue(0).compareTo(componentName) == 0) {
                                    resource = xrp.getAttributeValue(1)
                                }
                            } else if (string == resourceName) {
                                resource = xrp.getAttributeValue(0)
                            }
                        } catch (e: Exception) {
                            println(e)
                        }

                    }
                    xrp.next()
                }
            }
        } catch (e: Exception) {
            println(e)
        }

        return resource
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
