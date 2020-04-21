package net.feheren_fekete.applist.applistpage.iconpack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R

class IconsAdapter(private val itemClickCallback: (position: Int, isSelected: Boolean) -> Unit): RecyclerView.Adapter<IconViewHolder>() {

    var iconPackPackageName = ""
    var selectedItemPosition = RecyclerView.NO_POSITION
        private set

    private val items = arrayListOf<IconPackIcon>()
    private val filteredItems = arrayListOf<IconPackIcon>()
    private var filterText: String? = null

    fun clearItems() {
        this.items.clear()
        updateFilteredItems()

        selectedItemPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()

        // Let the listener know that nothing is selected
        itemClickCallback(selectedItemPosition, false)
    }

    fun setItems(items: List<IconPackIcon>) {
        this.items.clear()
        this.items.addAll(items)
        updateFilteredItems()
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = filteredItems.get(position)

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
        holder.bind(iconPackPackageName, filteredItems[position].drawableName, position == selectedItemPosition)
    }

    private fun selectItem(position: Int) {
        if (position == selectedItemPosition) {
            // Unselect
            val previousSelectedItem = selectedItemPosition
            selectedItemPosition = RecyclerView.NO_POSITION
            notifyItemChanged(previousSelectedItem)

            itemClickCallback(position, false)
        } else {
            // Select
            val previousSelectedItem = selectedItemPosition
            selectedItemPosition = position
            notifyItemChanged(previousSelectedItem)
            notifyItemChanged(selectedItemPosition)

            itemClickCallback(position, true)
        }
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
