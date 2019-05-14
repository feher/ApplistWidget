package net.feheren_fekete.applist.utils.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey

internal class GlideIconPackIconLoader : ModelLoader<GlideIconPackIcon, GlideIconPackIcon> {

    override fun buildLoadData(model: GlideIconPackIcon, width: Int, height: Int, options: Options)
            : ModelLoader.LoadData<GlideIconPackIcon>? {
        return ModelLoader.LoadData(ObjectKey(model), IconDataFetcher(model))
    }

    override fun handles(model: GlideIconPackIcon): Boolean = true

    private class IconDataFetcher(private val model: GlideIconPackIcon) : DataFetcher<GlideIconPackIcon> {

        override fun getDataSource(): DataSource = DataSource.LOCAL

        override fun getDataClass(): Class<GlideIconPackIcon> = GlideIconPackIcon::class.java

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in GlideIconPackIcon>) {
            callback.onDataReady(model)
        }

        override fun cleanup() {
        }

        override fun cancel() {
        }

    }

}