package net.feheren_fekete.applist.utils.glide


import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.engine.cache.LruResourceCache



@GlideModule
class GlideConfiguration : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.isLowRamDevice) {
            builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_RGB_565))
        } else {
            builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
        }

//        val memoryCacheSizeBytes = 1024 * 1024 * 100 // 20mb
//        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(ComponentName::class.java, ComponentName::class.java, GlideAppIconFactory())
        registry.append(ComponentName::class.java, Drawable::class.java, GlideAppIconDecoder(context))

        registry.append(GlideIconPackIcon::class.java, GlideIconPackIcon::class.java, GlideIconPackIconFactory())
        registry.append(GlideIconPackIcon::class.java, Drawable::class.java, GlideIconPackIconDecoder(context))
    }
}
