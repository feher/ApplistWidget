package net.feheren_fekete.applist;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ApplistItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private static final float DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS = 2000;

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

    @Override
    public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
        int scrollBy;
        final int maxScroll = 50;
        final int direction = (int) Math.signum(viewSizeOutOfBounds);
        final int cappedScroll = direction * maxScroll;
        final float timeRatio;
        if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
            timeRatio = 1f;
        } else {
            timeRatio = (float) msSinceStartScroll / DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS;
        }
        scrollBy = (int) (cappedScroll * Math.pow(timeRatio, 5));
        if (scrollBy == 0) {
            scrollBy = viewSizeOutOfBounds > 0 ? 1 : -1;
        }
        return scrollBy;
    }

}
