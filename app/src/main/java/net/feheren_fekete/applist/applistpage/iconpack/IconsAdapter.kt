package net.feheren_fekete.applist.applistpage.iconpack

import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon

class IconsAdapter(
    private val iconPackHelper: IconPackHelper,
    private val itemClickCallback: (icon: IconPackIcon, isSelected: Boolean) -> Unit,
    private val itemLongTapCallback: (icon: IconPackIcon) -> Unit
): RecyclerView.Adapter<IconViewHolder>() {

    class Item(
        val icon: IconPackIcon = IconPackIcon(),
        var directUpdate: Boolean = false,
        var previousTimestamp: Long = 0,
        var currentTimestamp: Long = 0)

    private val h = Handler()

    private val invalidItem = Item()

    var iconPackPackageName = ""
    var selectedIcon = invalidItem.icon
        private set

    private val items = arrayListOf<Item>()
    private val filteredItems = arrayListOf<Item>()
    private var filterText: String? = null

//    private var previousDataTimeStamp = 0L
//    private var currentDataTimeStamp = 0L

    fun clearItems() {
        this.items.clear()
        updateFilteredItems()

        selectedIcon = invalidItem.icon
//        updateViews()
        notifyDataSetChanged()

        // Let the listener know that nothing is selected
        itemClickCallback(selectedIcon, false)
    }

    fun setItems(items: List<IconPackIcon>) {
        this.items.clear()
        this.items.addAll(items.map { Item(it) })
        updateFilteredItems()
//        updateViews()
        notifyDataSetChanged()
    }

    fun updateViews(from: Int, to: Int) {
        Log.d("ZIZI", "update")
        for (item in items) {
            item.currentTimestamp = System.currentTimeMillis()
        }
        for (i in from..to) {
            val item = filteredItems[i]
            item.directUpdate = true
        }
        //notifyItemRangeChanged(from, to - from + 1)
//        h.post {
//            notifyItemRangeChanged(0, 5)
//            h.post {
//                notifyItemRangeChanged(5, 5)
//                h.post {
//                    notifyItemRangeChanged(10, 5)
//                }
//            }
//        }
        //notifyItemRangeChanged(0, filteredItems.size - 5)
        //(notifyItemRangeChanged(0, filteredItems.size)
        notifyDataSetChanged()
//        h.postDelayed({
//            previousDataTimeStamp = currentDataTimeStamp
//            currentDataTimeStamp = System.currentTimeMillis()
//        }, 2000)
    }

    fun getItem(position: Int) = filteredItems[position].icon

    fun setFilterText(filter: String?) {
        filterText = filter
        updateFilteredItems()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.iconpack_icon_item, parent, false)
        return IconViewHolder(itemView,
            onClickCallback = {
                selectItem(it)
            },
            onLongClickCallback = {
                itemLongTapCallback(filteredItems[it].icon)
            }
        )
    }

    override fun getItemCount() = filteredItems.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val item = filteredItems[position]
        val isSelected = item.icon.isSameAs(selectedIcon)
        holder.bind(iconPackHelper, iconPackPackageName, item, isSelected)
        item.previousTimestamp = item.currentTimestamp
        item.directUpdate = false
    }

    private fun selectItem(position: Int) {
        val item = filteredItems[position]
        if (item.icon.isSameAs(selectedIcon)) {
            // Unselect
            val selectedItemPosition = getSelectedItemPosition()
            selectedIcon = invalidItem.icon
            notifyItemChanged(selectedItemPosition)

            itemClickCallback(item.icon, false)
        } else {
            // Select
            val previousSelectedItemPosition = getSelectedItemPosition()
            selectedIcon = item.icon
            notifyItemChanged(previousSelectedItemPosition)
            notifyItemChanged(position)

            itemClickCallback(item.icon, true)
        }
    }

    private fun getSelectedItemPosition(): Int {
        for (i in 0 until filteredItems.size) {
            val item = filteredItems[i]
            if (item.icon.isSameAs(selectedIcon)) {
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
                if (it.icon.drawableName.contains(ft)) {
                    return@filter true
                }
                for (component in it.icon.componentNames) {
                    if (component.packageName.contains(ft)) {
                        return@filter true
                    }
                }
                return@filter false
            })
        }
    }

}
