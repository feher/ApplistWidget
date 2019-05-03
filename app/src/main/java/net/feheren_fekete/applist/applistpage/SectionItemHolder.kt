package net.feheren_fekete.applist.applistpage

import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.MotionEventCompat
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem
import java.lang.ref.WeakReference

class SectionItemHolder(view: View, itemListener: ApplistAdapter.ItemListener) : ViewHolderBase(view, R.id.applist_section_item_layout) {
    private val draggedOverIndicatorLeft: View = view.findViewById(R.id.applist_section_item_dragged_over_indicator_left)
    private val draggedOverIndicatorRight: View = view.findViewById(R.id.applist_section_item_dragged_over_indicator_right)
    private val sectionName: TextView = view.findViewById<View>(R.id.applist_section_item_section_name) as TextView

    private val itemListenerRef: WeakReference<ApplistAdapter.ItemListener> = WeakReference(itemListener)
    private val typedValue = TypedValue()

    init {
        layout.setOnLongClickListener {
            itemListenerRef.get()?.onSectionLongTapped(adapterPosition)
            true
        }
        layout.setOnClickListener {
            itemListenerRef.get()?.onSectionTapped(adapterPosition)
        }
        layout.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                itemListenerRef.get()?.onSectionTouched(adapterPosition)
            }
            false
        }
    }

    fun bind(sectionItem: SectionItem) {

        sectionName.text = if (sectionItem.isCollapsed)
            sectionItem.name + " ..."
        else
            sectionItem.name

        val theme = sectionName.context.theme
        if (sectionItem.isHighlighted) {
            theme.resolveAttribute(R.attr.sectionTextHighlightColor, typedValue, true)
        } else {
            theme.resolveAttribute(R.attr.sectionTextColor, typedValue, true)
        }
        sectionName.setTextColor(typedValue.data)

        val alpha = if (sectionItem.isEnabled) 1.0f else 0.3f
        sectionName.alpha = alpha

        draggedOverIndicatorLeft.visibility = if (sectionItem.isDraggedOverLeft) View.VISIBLE else View.INVISIBLE
        draggedOverIndicatorRight.visibility = if (sectionItem.isDraggedOverRight) View.VISIBLE else View.INVISIBLE
    }

}
