package net.feheren_fekete.applist.applistpage.iconpack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R

class IconsAdapter(private val itemClickCallback: (position: Int, isSelected: Boolean) -> Unit): RecyclerView.Adapter<IconViewHolder>() {

    var iconPackPackageName = ""
    var selectedItemPosition = RecyclerView.NO_POSITION
        private set

    private val items = arrayListOf<String>()

    fun clearItems() {
        this.items.clear()

        selectedItemPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()

        // Let the listener know that nothing is selected
        itemClickCallback(selectedItemPosition, false)
    }

    fun setItems(items: List<String>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun addItems(items: List<String>) {
        val oldSize = this.items.size
        this.items.clear()
        this.items.addAll(items)
        notifyItemRangeInserted(oldSize, items.size)
    }

    fun getItem(position: Int) = items.get(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.iconpack_icon_item, parent, false)
        return IconViewHolder(itemView) {
            selectItem(it)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(iconPackPackageName, items[position], position == selectedItemPosition)
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

}
