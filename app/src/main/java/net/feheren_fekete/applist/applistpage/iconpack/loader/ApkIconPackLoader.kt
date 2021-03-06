package net.feheren_fekete.applist.applistpage.iconpack.loader

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackApp
import net.feheren_fekete.applist.utils.ImageUtils
import net.feheren_fekete.applist.utils.ScreenUtils
import kotlin.coroutines.coroutineContext

class ApkIconPackLoader(
    private val context: Context,
    private val packageManager: PackageManager,
    imageUtils: ImageUtils,
    private val screenUtils: ScreenUtils
) : IconPackLoader(packageManager, imageUtils) {

    private val uniformOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inScaled = false
        inDither = false
    }

    override fun getSupportedApps(iconPackPackageName: String): Flow<IconPackApp> {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return findFlowInXml(iconPackPackageName, iconPackResources, "appfilter") {
            if (it.name != "item" || it.attributeCount < 2) {
                return@findFlowInXml null
            }
            var componentName: ComponentName? = null
            var drawableName: String? = null
            for (i in 0 until it.attributeCount) {
                when (it.getAttributeName(i)) {
                    "component" -> {
                        // We assume the component to be in this format:
                        // ComponentInfo{package-name/class-name}
                        var componentString = it.getAttributeValue(i)
                        if (componentString.startsWith("ComponentInfo{")
                            && componentString.indexOf('/') != -1
                        ) {
                            componentString = componentString.drop("ComponentInfo{".length)
                            componentString = componentString.dropLast(1)
                            componentName = ComponentName.unflattenFromString(componentString)
                        }
                    }
                    "drawable" -> {
                        drawableName = it.getAttributeValue(i)
                        val resourceId =
                            getDrawableId(iconPackPackageName, iconPackResources, drawableName)
                        if (resourceId == 0) {
                            drawableName = null
                        }
                    }
                }
            }
            if (componentName != null && drawableName != null) {
                return@findFlowInXml IconPackApp(
                    componentName,
                    drawableName
                )
            }
            return@findFlowInXml null
        }
    }

    override fun getIconDrawableNames(iconPackPackageName: String): Flow<String> {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return findFlowInXml(iconPackPackageName, iconPackResources, "drawable") {
            if (it.name == "item") {
                for (i in 0 until it.attributeCount) {
                    if (it.getAttributeName(i) == "drawable") {
                        val drawableName = it.getAttributeValue(i)
                        val resourceId =
                            getDrawableId(iconPackPackageName, iconPackResources, drawableName)
                        if (resourceId != 0) {
                            return@findFlowInXml drawableName
                        }
                    }
                }
            }
            return@findFlowInXml null
        }
    }

    private fun loadIcon(
        iconPackPackageName: String,
        componentName: ComponentName
    ): Bitmap? {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return getIconDrawableName(iconPackResources, iconPackPackageName, componentName)?.let {
            return@let loadBitmap(iconPackPackageName, iconPackResources, it)
        }
    }

    override fun loadIcon(
        iconPackPackageName: String,
        drawableName: String
    ): Bitmap? {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)
        return loadBitmap(iconPackPackageName, iconPackResources, drawableName)
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        originalIcon: Bitmap,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        val icon = loadIcon(iconPackPackageName, componentName)
        if (icon != null) {
            return icon
        }
        val width = screenUtils.dpToPx(fallbackIconWidthDp.toFloat()).toInt()
        val height = screenUtils.dpToPx(fallbackIconHeightDp.toFloat()).toInt()
        val fallbackIcon = createFallbackIcon(
            packageManager, iconPackPackageName, width, height, originalIcon
        )
        return fallbackIcon
    }

    override fun loadIconWithFallback(
        iconPackPackageName: String,
        componentName: ComponentName,
        fallbackIconWidthDp: Int,
        fallbackIconHeightDp: Int
    ): Bitmap {
        val originalIcon = loadOriginalAppIcon(componentName)
        return loadIconWithFallback(
            iconPackPackageName, componentName, originalIcon,
            fallbackIconWidthDp, fallbackIconHeightDp
        )
    }

    private fun createFallbackIcon(
        packageManager: PackageManager,
        iconPackPackageName: String,
        width: Int,
        height: Int,
        originalIcon: Bitmap
    ): Bitmap {
        val iconPackResources = packageManager.getResourcesForApplication(iconPackPackageName)

        val icon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val iconCanvas = Canvas(icon)
        val iconPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        iconPaint.isAntiAlias = true

        // Draw the background
        getFallbackIconDrawableName(iconPackResources, iconPackPackageName, "iconback")?.let {
            loadBitmap(iconPackPackageName, iconPackResources, it)?.let { backBitmap ->
                iconCanvas.drawBitmap(
                    backBitmap,
                    getResizeMatrix(backBitmap, width, height),
                    iconPaint
                )
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
                scaledOriginalIconCanvas.drawBitmap(
                    maskBitmap,
                    getResizeMatrix(maskBitmap, width, height),
                    maskPaint
                )
            }
        }

        // Draw the scaled original icon on top of the background
        iconCanvas.drawBitmap(scaledOriginalIcon, 0.0f, 0.0f, iconPaint)

        // Put the "upon" layer on top
        getFallbackIconDrawableName(iconPackResources, iconPackPackageName, "iconupon")?.let {
            loadBitmap(iconPackPackageName, iconPackResources, it)?.let { uponBitmap ->
                iconCanvas.drawBitmap(
                    uponBitmap,
                    getResizeMatrix(uponBitmap, width, height),
                    iconPaint
                )
            }
        }

        return icon
    }

    private fun loadBitmap(
        iconPackPackageName: String,
        iconPackResources: Resources,
        resourceName: String
    ): Bitmap? {
        val resourceId = getDrawableId(iconPackPackageName, iconPackResources, resourceName)
        return if (resourceId != 0) {
            BitmapFactory.decodeResource(iconPackResources, resourceId, uniformOptions)
        } else {
            null
        }
    }

    private fun getDrawableId(
        iconPackPackageName: String,
        iconPackResources: Resources,
        resourceName: String
    ): Int {
        return iconPackResources.getIdentifier(resourceName, "drawable", iconPackPackageName)
    }

    private fun getIconDrawableName(
        resources: Resources,
        packageName: String,
        componentName: ComponentName
    ): String? {
        var result: String? = null
        findInXml(packageName, resources, "appfilter") {
            if (it.name == "item"
                && it.attributeCount >= 2
                && it.getAttributeName(0) == "component"
                && it.getAttributeValue(0) == componentName.toString()
                && it.getAttributeName(1) == "drawable"
            ) {
                result = it.getAttributeValue(1)
                return@findInXml true
            } else {
                return@findInXml false
            }
        }
        return result
    }

    private fun getFallbackIconDrawableName(
        resources: Resources,
        packageName: String,
        elementName: String
    ): String? {
        var result: String? = null
        findInXml(packageName, resources, "appfilter") {
            if (it.name == elementName
                && it.attributeCount >= 1
                && it.getAttributeName(0).startsWith("img")
            ) {
                result = it.getAttributeValue(0)
                return@findInXml true
            } else {
                return@findInXml false
            }
        }
        return result
    }

    private fun getScaleFactor(
        resources: Resources,
        packageName: String
    ): Float {
        var result = 1.0f
        findInXml(packageName, resources, "appfilter") {
            if (it.name == "scale"
                && it.attributeCount >= 1
                && it.getAttributeName(0) == "factor"
            ) {
                result = it.getAttributeValue(0).toFloat()
                return@findInXml true
            } else {
                return@findInXml false
            }
        }
        return result
    }

    private fun findInXml(
        packageName: String,
        resources: Resources,
        xmlName: String,
        finder: (XmlResourceParser) -> Boolean
    ) {
        val resourceValue = resources.getIdentifier(xmlName, "xml", packageName)
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

    private inline fun <T> findFlowInXml(
        packageName: String,
        resources: Resources,
        xmlName: String,
        crossinline finder: (XmlResourceParser) -> T?
    ) = flow {
        val resourceValue = resources.getIdentifier(xmlName, "xml", packageName)
        if (resourceValue != 0) {
            val xrp = resources.getXml(resourceValue)
            while (xrp.eventType != XmlResourceParser.END_DOCUMENT) {
                if (!coroutineContext.isActive) {
                    return@flow
                }
                if (xrp.eventType == XmlResourceParser.START_TAG) {
                    val v = finder(xrp)
                    if (v != null) {
                        emit(v!!)
                    }
                }
                xrp.next()
            }
        }
    }

    private fun getResizeMatrix(
        bm: Bitmap,
        newHeight: Int,
        newWidth: Int
    ): Matrix {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return matrix
    }

    companion object {
        const val name = "apk"
    }

}