package net.feheren_fekete.applist.applistpage.iconpack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon

class IconsAdapter(private val itemClickCallback: (icon: IconPackIcon, isSelected: Boolean) -> Unit): RecyclerView.Adapter<IconViewHolder>() {

    private val invalidItem =
        IconPackIcon()

    var iconPackPackageName = ""
    var selectedItem = invalidItem
        private set

    private val items = arrayListOf<IconPackIcon>()
    private val filteredItems = arrayListOf<IconPackIcon>()
    private var filterText: String? = null

    fun clearItems() {
        this.items.clear()
        updateFilteredItems()

        selectedItem = invalidItem
        notifyDataSetChanged()

        // Let the listener know that nothing is selected
        itemClickCallback(selectedItem, false)
    }

    fun setItems(items: List<IconPackIcon>) {
        this.items.clear()
        this.items.addAll(items)
        updateFilteredItems()
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = filteredItems[position]

    fun setFilterText(filter: String?) {
        filterText = filter
        updateFilteredItems()
        notifyDataSetChanged()
    }

    fun isFiltered() = filterText != null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.iconpack_icon_item, parent, false)
        return IconViewHolder(itemView) {
            selectItem(it)
        }
    }

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val item = filteredItems[position]
        val isSelected = item.isSameAs(selectedItem)
        holder.bind(iconPackPackageName, item.drawableName, isSelected)
    }

    private fun selectItem(position: Int) {
        val item = filteredItems[position]
        if (item.isSameAs(selectedItem)) {
            // Unselect
            val selectedItemPosition = getSelectedItemPosition()
            selectedItem = invalidItem
            notifyItemChanged(selectedItemPosition)

            itemClickCallback(item, false)
        } else {
            // Select
            val previousSelectedItemPosition = getSelectedItemPosition()
            selectedItem = item
            notifyItemChanged(previousSelectedItemPosition)
            notifyItemChanged(position)

            itemClickCallback(item, true)
        }
    }

    private fun getSelectedItemPosition(): Int {
        for (i in 0 until filteredItems.size) {
            val item = filteredItems[i]
            if (item.isSameAs(selectedItem)) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun updateFilteredItems() {
        val ft = filterText
        if (ft == null) {
            filteredItems.clear()
            filteredItems.addAll(items)
        } else {
            filteredItems.clear()
            filteredItems.addAll(items.filter {
                if (it.drawableName.contains(ft)) {
                    return@filter true
                }
                for (component in it.componentNames) {
                    if (component.packageName.contains(ft)) {
                        return@filter true
                    }
                }
                return@filter false
            })
        }
    }

}
