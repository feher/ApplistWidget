package net.feheren_fekete.applist.applistpage.iconpack

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.iconpack_item.view.*
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo
import net.feheren_fekete.applist.utils.glide.GlideApp

class IconPackViewHolder(view: View,
                         val onClickCallback: (position: Int) -> Unit): RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            onClickCallback(adapterPosition)
        }
    }

    fun bind(iconPack: IconPackInfo, isSelected: Boolean) {
        GlideApp.with(itemView)
                .load(iconPack.componentName)
                .into(itemView.icon)
        itemView.name.text = iconPack.name
        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.iconpack_picker_item_highlighted_background)
        } else {
            itemView.background = null
        }
    }

}
