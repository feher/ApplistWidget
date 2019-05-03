package net.feheren_fekete.applist.applistpage

import android.content.ComponentName
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.FixedPreloadSizeProvider
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem
import net.feheren_fekete.applist.applistpage.viewmodel.AppShortcutItem
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem
import net.feheren_fekete.applist.applistpage.viewmodel.ShortcutItem
import net.feheren_fekete.applist.utils.glide.GlideApp
import java.io.File

class IconPreloadHelper {

    companion object {
        const val PRELOAD_ICON_SIZE = 100
    }

    fun setupPreloader(context: Context,
                       recyclerView: RecyclerView,
                       adapter: ApplistAdapter, preloadCount: Int) {
        val sizeProvider = FixedPreloadSizeProvider<BaseItem>(
                PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
        val preloadModelProvider = ApplistPreloadModelProvider(context, adapter)
        val preloader = RecyclerViewPreloader(
                GlideApp.with(context), preloadModelProvider,
                sizeProvider, preloadCount)
        recyclerView.addOnScrollListener(preloader)
    }

    inner class ApplistPreloadModelProvider(private val context: Context,
                                            private val adapter: ApplistAdapter): ListPreloader.PreloadModelProvider<BaseItem> {
        override fun getPreloadItems(position: Int): MutableList<BaseItem> {
            val item = adapter.getItemAt(position)
            return if (item is AppItem
                    || item is ShortcutItem
                    || item is AppShortcutItem) {
                mutableListOf(item)
            } else {
                mutableListOf()
            }
        }

        override fun getPreloadRequestBuilder(item: BaseItem): RequestBuilder<*>? {
            return if (item is AppItem) {
                GlideApp.with(context)
                        .load(ComponentName(item.packageName, item.className))
                        .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
            } else if (item is ShortcutItem) {
                GlideApp.with(context)
                        .load(File(item.iconPath))
                        .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
            } else if (item is AppShortcutItem) {
                GlideApp.with(context)
                        .load(File(item.iconPath))
                        .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
            } else {
                null
            }
        }
    }

}
