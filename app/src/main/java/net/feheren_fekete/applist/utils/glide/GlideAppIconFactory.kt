package net.feheren_fekete.applist.utils.glide


import android.content.ComponentName
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory

internal class GlideAppIconFactory : ModelLoaderFactory<ComponentName, ComponentName> {

    override fun build(multiFactory: MultiModelLoaderFactory)
            : ModelLoader<ComponentName, ComponentName> = GlideAppIconLoader()

    override fun teardown() {
    }

}