package net.feheren_fekete.applist.utils.glide

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.util.Util
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.utils.ImageUtils
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

internal class GlideAppIconDecoder(private val context: Context)
    : ResourceDecoder<ComponentName, Drawable>, KoinComponent {

    private val imageUtils: ImageUtils by inject()

    override fun decode(source: ComponentName, width: Int, height: Int, options: Options): Resource<Drawable>? {
        return try {
            val customIcon = loadCustomAppIcon(source)
            if (customIcon != null) {
                IconResource(customIcon)
            } else {
                IconResource(loadDefaultAppIcon(source))
            }
        } catch (e: Exception) {
            ApplistLog.getInstance().log(e)
            null
        }
    }

    override fun handles(source: ComponentName, options: Options): Boolean = true

    private class IconResource(icon: Drawable) : DrawableResource<Drawable>(icon) {

        override fun getResourceClass(): Class<Drawable> = Drawable::class.java

        override fun getSize(): Int {
            return if (drawable is BitmapDrawable) {
                Util.getBitmapByteSize(drawable.bitmap)
            } else {
                1
            }
        }

        override fun recycle() {
        }

    }

    private fun loadDefaultAppIcon(componentName: ComponentName) =
            context.packageManager.getActivityInfo(componentName, 0)
                    .applicationInfo
                    .loadIcon(context.packageManager)

    private fun loadCustomAppIcon(componentName: ComponentName): Drawable? {
        val customIconFile = getCustomIconFile(componentName)
        if (!customIconFile.exists()) {
            return null
        }
        return BitmapDrawable(context.resources, imageUtils.loadBitmap(customIconFile.absolutePath))
    }

    private fun getCustomIconFile(componentName: ComponentName): File {
        return File(context.getFilesDir(), "icons/${componentName.packageName}::${componentName.className}")
    }

}
