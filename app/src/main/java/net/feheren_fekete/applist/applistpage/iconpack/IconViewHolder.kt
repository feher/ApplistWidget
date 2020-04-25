package net.feheren_fekete.applist.applistpage.iconpack

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
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

    fun bind(iconPackHelper: IconPackHelper,
            iconPackPackageName: String,
             item: IconsAdapter.Item,
             isSelected: Boolean) {
        Log.d("ZIZI", "bind prev ${item.previousTimestamp} curr ${item.directUpdate} ${item.icon.drawableName}")
        val prevSig = ObjectKey("$iconPackPackageName.${item.icon.drawableName}.${item.previousTimestamp}")
        val curSig = ObjectKey("$iconPackPackageName.${item.icon.drawableName}.${item.currentTimestamp}")

//        GlideApp.with(itemView)
//            .asDrawable()
//            .signature(prevSig)
//            .load(GlideIconPackIcon(iconPackPackageName, item))
//            .into(object: CustomTarget<Drawable>() {
//                override fun onLoadCleared(placeholder: Drawable?) {
//                    Log.d("ZIZI", "cleared $item")
//                    GlideApp.with(itemView)
//                        .load(GlideIconPackIcon(iconPackPackageName, item))
//                        .signature(curSig)
//                        .into(itemView.icon)
//                }
//
//                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
//                    Log.d("ZIZI", "ready $item")
//                    GlideApp.with(itemView)
//                        .load(GlideIconPackIcon(iconPackPackageName, item))
//                        .signature(curSig)
//                        .placeholder(resource)
//                        .error(resource)
//                        .fallback(resource)
//                        .into(itemView.icon)
//                }
//
//                override fun onLoadFailed(errorDrawable: Drawable?) {
//                    Log.d("ZIZI", "failed $item")
//                    GlideApp.with(itemView)
//                        .load(GlideIconPackIcon(iconPackPackageName, item))
//                        .signature(curSig)
//                        .into(itemView.icon)
//                }
//            })


        if (item.directUpdate) {
            itemView.icon.setImageBitmap(
                iconPackHelper.loadIcon(
                    iconPackPackageName, item.icon.drawableName))
        } else {
            GlideApp.with(itemView)
                .load(GlideIconPackIcon(iconPackPackageName, item.icon.drawableName))
                .signature(curSig)
                .into(itemView.icon)

        }

//        val thumbnail = if (previousTimestamp != currentTimestamp) {
//            GlideApp.with(itemView)
//                .load(GlideIconPackIcon(iconPackPackageName, item))
//                .signature(prevSig)
//        } else {
//            null
//        }
//        GlideApp.with(itemView)
//                .load(GlideIconPackIcon(iconPackPackageName, item))
//                .signature(curSig)
//                .thumbnail(thumbnail)
//                .into(itemView.icon)

        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.iconpack_picker_item_highlighted_background)
        } else {
            itemView.background = null
        }
    }

}
