package net.feheren_fekete.applist.applistpage.iconpack

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.iconpack_item.view.*
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.loader.IconPackLoader
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo
import net.feheren_fekete.applist.utils.glide.GlideApp
import java.lang.NumberFormatException

class IconPackViewHolder(view: View,
                         val onClickCallback: (position: Int) -> Unit): RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            onClickCallback(adapterPosition)
        }
    }

    fun bind(iconPack: IconPackInfo, isSelected: Boolean) {
        if (IconPackLoader.isEffectIconPack(iconPack.componentName.packageName)) {
            try {
                val iconDrawableId = iconPack.componentName.className.toInt()
                itemView.icon.setImageResource(iconDrawableId)
            } catch (e: NumberFormatException) {
                itemView.icon.setImageBitmap(null)
            }
        } else {
            GlideApp.with(itemView)
                .load(iconPack.componentName)
                .into(itemView.icon)
        }
        itemView.name.text = iconPack.name
        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.iconpack_picker_item_highlighted_background)
        } else {
            itemView.background = null
        }
    }

}
