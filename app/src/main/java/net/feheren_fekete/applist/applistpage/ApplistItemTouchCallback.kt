package net.feheren_fekete.applist.applistpage

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ApplistItemTouchCallback(
        private val canMoveHorizontally: (position: Int) -> Boolean,
        private val onItemDrag: (oldPosition: Int, newPosition: Int) -> Boolean): ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView,
                                  viewHolder: RecyclerView.ViewHolder): Int {
        var dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        if (canMoveHorizontally(viewHolder.adapterPosition)) {
            dragFlags = dragFlags or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        }
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        return onItemDrag(viewHolder.adapterPosition, target.adapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing.
    }

    override fun isLongPressDragEnabled() = false

    override fun isItemViewSwipeEnabled() = false

}