package net.feheren_fekete.applist.utils.glide

import android.content.ComponentName
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey

internal class GlideAppIconLoader : ModelLoader<ComponentName, ComponentName> {

    override fun buildLoadData(model: ComponentName, width: Int, height: Int, options: Options)
            : ModelLoader.LoadData<ComponentName>? {
        return ModelLoader.LoadData(ObjectKey(model), IconDataFetcher(model))
    }

    override fun handles(model: ComponentName): Boolean = true

    private class IconDataFetcher(private val model: ComponentName) : DataFetcher<ComponentName> {

        override fun getDataSource(): DataSource = DataSource.LOCAL

        override fun getDataClass(): Class<ComponentName> = ComponentName::class.java

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ComponentName>) {
            callback.onDataReady(model)
        }

        override fun cleanup() {
        }

        override fun cancel() {
        }

    }

}