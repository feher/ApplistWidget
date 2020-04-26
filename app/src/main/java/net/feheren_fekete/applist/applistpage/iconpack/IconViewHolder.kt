package net.feheren_fekete.applist.applistpage.iconpack

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.ObjectKey
import kotlinx.android.synthetic.main.iconpack_icon_item.view.*
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.utils.glide.GlideApp
import net.feheren_fekete.applist.utils.glide.GlideIconPackIcon

class IconViewHolder(
    view: View,
    private val onClickCallback: (position: Int) -> Unit,
    private val onLongClickCallback: (position: Int) -> Unit
) : RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            onClickCallback(adapterPosition)
        }
        view.setOnLongClickListener {
            onLongClickCallback(adapterPosition)
            true
        }
    }

    fun bind(
        iconPackPackageName: String,
        item: IconsAdapter.Item,
        isSelected: Boolean
    ) {
        val curSig =
            ObjectKey("$iconPackPackageName.${item.icon.drawableName}.${item.currentTimestamp}")
        GlideApp.with(itemView)
            .load(GlideIconPackIcon(iconPackPackageName, item.icon.drawableName))
            .signature(curSig)
            .placeholder(R.drawable.ic_processing)
            .error(R.drawable.ic_error)
            .fallback(R.drawable.ic_error)
            .into(itemView.icon)

        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.iconpack_picker_item_highlighted_background)
        } else {
            itemView.background = null
        }
    }

}
