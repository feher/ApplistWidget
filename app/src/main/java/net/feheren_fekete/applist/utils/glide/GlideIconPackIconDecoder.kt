package net.feheren_fekete.applist.utils.glide

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.util.Util
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import org.koin.core.KoinComponent
import org.koin.core.inject

internal class GlideIconPackIconDecoder(private val context: Context) : ResourceDecoder<GlideIconPackIcon, Drawable>, KoinComponent {

    private val iconPackHelper: IconPackHelper by inject()

    override fun decode(source: GlideIconPackIcon, width: Int, height: Int, options: Options): Resource<Drawable>? {
        return try {
            val icon = iconPackHelper.loadIcon(
                    context.packageManager,
                    source.iconPackPackageName,
                    source.drawableName)
            if (icon != null) {
                IconResource(BitmapDrawable(context.resources, icon))
            } else {
                null
            }
        } catch (e: Exception) {
            ApplistLog.getInstance().log(e)
            null
        }
    }

    override fun handles(source: GlideIconPackIcon, options: Options): Boolean = true

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
