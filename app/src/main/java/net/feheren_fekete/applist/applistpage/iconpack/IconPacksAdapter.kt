package net.feheren_fekete.applist.applistpage.iconpack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.loader.IconPackLoader
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPacksRepository

class IconPacksAdapter(private val itemClickCallback: (position: Int) -> Unit) :
    RecyclerView.Adapter<IconPackViewHolder>() {

    enum class Filter {
        All,
        IconPack,
        IconEffect
    }

    private var filter = Filter.IconEffect
    private val items = arrayListOf<IconPackInfo>()
    private val filteredItems = arrayListOf<IconPackInfo>()
    private var selectedItemPosition = RecyclerView.NO_POSITION

    fun setItems(items: List<IconPackInfo>) {
        this.items.clear()
        this.items.addAll(items)
        this.selectedItemPosition = 0
        updateFilteredItems()
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = filteredItems[position]

    fun setFilter(filter: Filter) {
        this.selectedItemPosition = RecyclerView.NO_POSITION
        this.filter = filter
        updateFilteredItems()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconPackViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.iconpack_item, parent, false)
        return IconPackViewHolder(itemView) {
            selectItem(it)
            itemClickCallback(it)
        }
    }

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: IconPackViewHolder, position: Int) {
        holder.bind(filteredItems[position], position == selectedItemPosition)
    }

    private fun selectItem(position: Int) {
        val previousSelectedItem = selectedItemPosition
        selectedItemPosition = position
        notifyItemChanged(previousSelectedItem)
        notifyItemChanged(selectedItemPosition)
    }

    private fun updateFilteredItems() {
        filteredItems.clear()
        for (item in items) {
            when (filter) {
                Filter.All -> filteredItems.add(item)
                Filter.IconPack ->
                    if (!IconPackLoader.isBuiltinIconPack(item.componentName.packageName)
                        || IconPacksRepository.isInstallerIconPack(item)
                    ) {
                        filteredItems.add(item)
                    }
                Filter.IconEffect ->
                    if (IconPackLoader.isBuiltinIconPack(item.componentName.packageName)) {
                        filteredItems.add(item)
                    }
            }
        }
    }

}
