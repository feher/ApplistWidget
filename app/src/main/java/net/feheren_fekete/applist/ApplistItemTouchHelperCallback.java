package net.feheren_fekete.applist;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ApplistItemTouchHelperCallback extends ItemTouchHelper.Callback {

    public interface OnMoveListener {
        void onItemMoveStart(RecyclerView.ViewHolder viewHolder);
        boolean onItemMove(RecyclerView.ViewHolder fromViewHolder, RecyclerView.ViewHolder targetViewHolder);
        void onItemMoveEnd(RecyclerView.ViewHolder viewHolder);
    }

    private final OnMoveListener mOnMoveListener;

    public ApplistItemTouchHelperCallback(OnMoveListener onMoveListener) {
        mOnMoveListener = onMoveListener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN
                | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            mOnMoveListener.onItemMoveStart(viewHolder);
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return mOnMoveListener.onItemMove(viewHolder, target);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mOnMoveListener.onItemMoveEnd(viewHolder);
    }
}
