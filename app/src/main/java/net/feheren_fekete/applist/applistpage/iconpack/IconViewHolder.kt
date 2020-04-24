package net.feheren_fekete.applist.applistpage.iconpack

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.iconpack_icon_item.view.*
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.glide.GlideApp
import net.feheren_fekete.applist.utils.glide.GlideIconPackIcon
import net.feheren_fekete.applist.utils.glide.IconPackIconSignature

class IconViewHolder(
    view: View,
    private val onClickCallback: (position: Int) -> Unit,
    private val onLongClickCallback: (position: Int) -> Unit
): RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            onClickCallback(adapterPosition)
        }
        view.setOnLongClickListener {
            onLongClickCallback(adapterPosition)
            true
        }
    }

    fun bind(iconPackPackageName: String, item: String, isSelected: Boolean) {
        GlideApp.with(itemView)
                .load(GlideIconPackIcon(iconPackPackageName, item))
                .signature(IconPackIconSignature(System.currentTimeMillis()))
                .into(itemView.icon)
        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.iconpack_picker_item_highlighted_background)
        } else {
            itemView.background = null
        }
    }

}
