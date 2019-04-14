package net.feheren_fekete.applist.utils.glide

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.util.Util
import net.feheren_fekete.applist.ApplistLog

internal class GlideAppIconDecoder(private val context: Context) : ResourceDecoder<ComponentName, Drawable> {

    override fun decode(source: ComponentName, width: Int, height: Int, options: Options): Resource<Drawable>? {
        val pm = context.packageManager
        return try {
            pm.getActivityInfo(source, 0)
                .applicationInfo
                .loadIcon(pm)
                .let(::IconResource)
        } catch (e: PackageManager.NameNotFoundException) {
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

}