package net.feheren_fekete.applist.applistpage

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ApplistItemTouchCallback(
        private val canMoveHorizontally: (position: Int) -> Boolean,
        private val onItemDrag: (oldPosition: Int, newPosition: Int) -> Boolean,
        private val onItemDropped: (position: Int) -> Unit): ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView,
                                  viewHolder: RecyclerView.ViewHolder): Int {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) {
            return makeMovementFlags(0, 0)
        }
        var dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        if (canMoveHorizontally(position)) {
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

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onItemDropped(viewHolder.adapterPosition)
    }

    override fun isLongPressDragEnabled() = false

    override fun isItemViewSwipeEnabled() = false

}