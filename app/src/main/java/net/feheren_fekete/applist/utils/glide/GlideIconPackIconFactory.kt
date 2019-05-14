package net.feheren_fekete.applist.utils.glide


import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory

internal class GlideIconPackIconFactory : ModelLoaderFactory<GlideIconPackIcon, GlideIconPackIcon> {

    override fun build(multiFactory: MultiModelLoaderFactory)
            : ModelLoader<GlideIconPackIcon, GlideIconPackIcon> = GlideIconPackIconLoader()

    override fun teardown() {
    }

}
