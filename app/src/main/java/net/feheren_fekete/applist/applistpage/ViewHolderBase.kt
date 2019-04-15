package net.feheren_fekete.applist.applistpage

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

open class ViewHolderBase(view: View, @IdRes layoutId: Int) : RecyclerView.ViewHolder(view) {
    val layout: ViewGroup = view.findViewById<View>(layoutId) as ViewGroup
}
