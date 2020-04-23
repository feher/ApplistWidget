package net.feheren_fekete.applist.applistpage.iconpack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPack

class IconPacksAdapter(private val itemClickCallback: (position: Int) -> Unit): RecyclerView.Adapter<IconPackViewHolder>() {

    private val items = arrayListOf<IconPack>()
    private var selectedItemPosition = RecyclerView.NO_POSITION

    fun setItems(items: List<IconPack>) {
        this.items.clear()
        this.items.addAll(items)
        this.selectedItemPosition = 0
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconPackViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.iconpack_item, parent, false)
        return IconPackViewHolder(itemView) {
            selectItem(it)
            itemClickCallback(it)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: IconPackViewHolder, position: Int) {
        holder.bind(items[position], position == selectedItemPosition)
    }

    private fun selectItem(position: Int) {
        val previousSelectedItem = selectedItemPosition
        selectedItemPosition = position
        notifyItemChanged(previousSelectedItem)
        notifyItemChanged(selectedItemPosition)
    }

}
