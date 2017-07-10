package net.feheren_fekete.applist.applistpage;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.PageData;
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;
import net.feheren_fekete.applist.applistpage.viewmodel.ViewModelUtils;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public class ApplistItemDragHandler implements DragGestureRecognizer.Callback {

    public interface Listener {
        void onItemDragStart();
        void onItemDragEnd();
    }

    // TODO: Inject these
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();
    private ApplistModel mApplistModel = ApplistModel.getInstance();

    private Context mContext;
    private ApplistPagePageFragment mApplistPagePageFragment;
    private ViewGroup mTouchOverlay;
    private RecyclerView mRecyclerView;
    private MyGridLayoutManager mLayoutManager;
    private ApplistAdapter mAdapter;
    private Listener mListener;
    private View mDraggedView;
    private View mDraggedSectionView;
    private int mDraggedAppViewSize;
    private int mDraggedSectionViewSize;
    private @Nullable BaseItem mDraggedOverItem;
    private int[] mRecyclerViewLocation = new int[2];
    private int[] mDraggedOverViewLocation = new int[2];
    private float mItemMoveThreshold;

    public ApplistItemDragHandler(Context context,
                                  ApplistPagePageFragment applistPagePageFragment,
                                  ViewGroup touchOverlay,
                                  RecyclerView recyclerView,
                                  MyGridLayoutManager layoutManager,
                                  ApplistAdapter adapter,
                                  Listener listener) {
        mContext = context;
        mApplistPagePageFragment = applistPagePageFragment;
        mTouchOverlay = touchOverlay;
        mRecyclerView = recyclerView;
        mLayoutManager = layoutManager;
        mAdapter = adapter;
        mListener = listener;

        mItemMoveThreshold = mContext.getResources().getDimension(R.dimen.applist_fragment_item_move_threshold);
        mDraggedAppViewSize = mContext.getResources().getDimensionPixelSize(R.dimen.appitem_icon_size);
        mDraggedSectionViewSize = mContext.getResources().getDimensionPixelSize(R.dimen.dragged_section_item_width);
    }

    @Override
    public boolean canDrag(DragGestureRecognizer gestureRecognizer) {
        return !mAdapter.isFilteredByName()
                && mApplistPagePageFragment.isItemMenuOpen();
    }

    @Override
    public boolean canStartDragging(DragGestureRecognizer gestureRecognizer) {
        MotionEvent event = gestureRecognizer.getMotionEvent();
        PointF fingerDownPos = gestureRecognizer.getFingerDownPos();
        float a = event.getRawX() - fingerDownPos.x;
        float b = event.getRawY() - fingerDownPos.y;
        double distance = Math.sqrt(a * a + b * b);
        return (distance > mItemMoveThreshold);
    }

    @Override
    public void onStartDragging(DragGestureRecognizer gestureRecognizer) {
        mApplistPagePageFragment.closeItemMenu();

        mListener.onItemDragStart();

        final BaseItem draggedItem = mApplistPagePageFragment.getItemMenuTarget();
        if (draggedItem instanceof StartableItem) {
            mAdapter.setEnabled(draggedItem, false);
        } else if (draggedItem instanceof SectionItem) {
            mAdapter.setAllStartablesEnabled(false);
            mAdapter.setSectionsHighlighted(true);
        }

        addDraggedView(gestureRecognizer, draggedItem);
    }

    @Override
    public void onDragging(DragGestureRecognizer gestureRecognizer) {
        if (mDraggedView != null) {
            final BaseItem draggedItem = mApplistPagePageFragment.getItemMenuTarget();
            updateDraggedViewPosition(gestureRecognizer, draggedItem);
            updateDraggedOverItem(gestureRecognizer, draggedItem);
            scrollWhileDragging(gestureRecognizer);
        }
    }

    @Override
    public void onDrop(DragGestureRecognizer gestureRecognizer) {
        if (mDraggedOverItem != null) {
            boolean canDrop = true;
            final BaseItem draggedItem = mApplistPagePageFragment.getItemMenuTarget();
            if (draggedItem == mDraggedOverItem) {
                canDrop = false;
            }
            if (draggedItem instanceof SectionItem) {
                if (mDraggedOverItem instanceof SectionItem) {
                    final int draggedOverItemPos = mAdapter.getItemPosition(mDraggedOverItem);
                    if (draggedOverItemPos == mAdapter.getNextSectionPosition(draggedItem)) {
                        canDrop = false;
                    }
                }
            }

            if (canDrop) {
                int fromPosition = mAdapter.getItemPosition(draggedItem);
                int toPosition = mAdapter.getItemPosition(mDraggedOverItem);
                if (fromPosition < toPosition && mDraggedOverItem.isDraggedOverLeft()) {
                    --toPosition;
                }
                if (toPosition < fromPosition && mDraggedOverItem.isDraggedOverRight()) {
                    ++toPosition;
                }
                mAdapter.moveItem(fromPosition, toPosition);
                savePageToModel();
            }
        }
    }

    @Override
    public void onStopDragging(DragGestureRecognizer gestureRecognizer) {
        final BaseItem draggedItem = mApplistPagePageFragment.getItemMenuTarget();
        if (draggedItem instanceof StartableItem) {
            mAdapter.setEnabled(draggedItem, true);
        } else if (draggedItem instanceof SectionItem) {
            mAdapter.setAllStartablesEnabled(true);
            mAdapter.setSectionsHighlighted(false);
        }

        if (mDraggedOverItem != null) {
            mDraggedOverItem.setDraggedOver(BaseItem.NONE);
            mAdapter.notifyItemChanged(mAdapter.getItemPosition(mDraggedOverItem));
        }

        removeDraggedView();

        mListener.onItemDragEnd();
    }

    private void addDraggedView(DragGestureRecognizer gestureRecognizer, BaseItem draggedItem) {
        if (draggedItem instanceof StartableItem) {
            ApplistAdapter.StartableItemHolder startableItemHolder =
                    (ApplistAdapter.StartableItemHolder) mRecyclerView.findViewHolderForItemId(draggedItem.getId());
            ImageView imageView = new ImageView(mContext);
            imageView.setImageDrawable(startableItemHolder.appIcon.getDrawable());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mDraggedAppViewSize, mDraggedAppViewSize);
            imageView.setLayoutParams(layoutParams);
            mDraggedView = imageView;
        } else {
            if (mDraggedSectionView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                mDraggedSectionView = inflater.inflate(R.layout.dragged_section_item, null, false);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        mDraggedSectionViewSize, ViewGroup.LayoutParams.WRAP_CONTENT);
                mDraggedSectionView.setLayoutParams(layoutParams);
            }
            SectionItem sectionItem = (SectionItem) draggedItem;
            TextView textView = (TextView) mDraggedSectionView.findViewById(R.id.dragged_section_item_name);
            textView.setText(sectionItem.getName());
            mDraggedView = mDraggedSectionView;
        }
        updateDraggedViewPosition(gestureRecognizer, draggedItem);
        mTouchOverlay.addView(mDraggedView);
    }

    private void updateDraggedViewPosition(DragGestureRecognizer gestureRecognizer, BaseItem draggedItem) {
        final MotionEvent event = gestureRecognizer.getMotionEvent();
        final float fingerRawX = event.getRawX();
        final float fingerRawY = event.getRawY();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mDraggedView.getLayoutParams();
        mRecyclerView.getLocationOnScreen(mRecyclerViewLocation);
        layoutParams.leftMargin = Math.round(fingerRawX - mRecyclerViewLocation[0]);
        layoutParams.topMargin = Math.round(fingerRawY - mRecyclerViewLocation[1]);
        if (draggedItem instanceof StartableItem) {
            layoutParams.leftMargin -= mDraggedAppViewSize / 2;
            layoutParams.topMargin -= mDraggedAppViewSize * 1.5f;
        } else {
            layoutParams.leftMargin -= mDraggedSectionViewSize / 2;
            layoutParams.topMargin -= mDraggedSectionViewSize;
        }
        mDraggedView.setLayoutParams(layoutParams);
    }

    private void removeDraggedView() {
        mTouchOverlay.removeView(mDraggedView);
        mDraggedView = null;
    }

    private void updateDraggedOverItem(DragGestureRecognizer gestureRecognizer, BaseItem draggedItem) {
        final MotionEvent event = gestureRecognizer.getMotionEvent();
        final float fingerCurrentPosX = event.getRawX();
        final float fingerCurrentPosY = event.getRawY();
        float draggedViewPosX = fingerCurrentPosX;
        float draggedViewPosY = fingerCurrentPosY;
        if (draggedItem instanceof StartableItem) {
            draggedViewPosX -= mDraggedAppViewSize / 2;
            draggedViewPosY -= mDraggedAppViewSize;
        } else {
            draggedViewPosX -= mDraggedSectionViewSize / 2;
            draggedViewPosY -= mDraggedSectionViewSize / 3;
        }

        final int firstItemPos = mLayoutManager.findFirstVisibleItemPosition();
        final int lastItemPos = mLayoutManager.findLastVisibleItemPosition();

        if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {

            int candidateItemPosition = RecyclerView.NO_POSITION;
            BaseItem candidateItem = null;
            double distanceToCandidateItem = Double.MAX_VALUE;
            int candidateViewLeft = 0;
            int candidateViewTop = 0;
            int candidateViewRight = 0;
            int candidateViewBottom = 0;
            for (int i = firstItemPos; i <= lastItemPos; ++i) {
                boolean considerItem = true;
                BaseItem item = mAdapter.getItem(i);
                if (draggedItem instanceof StartableItem
                        && item instanceof SectionItem) {
                    SectionItem sectionItem = (SectionItem) item;
                    if (!sectionItem.isCollapsed() && !mAdapter.isSectionEmpty(sectionItem)) {
                        // We don't allow dragging over open and not empty (i.e. normal) section
                        // headers.
                        considerItem = false;
                    }
                }
                if (draggedItem instanceof SectionItem
                        && item instanceof StartableItem) {
                    // We don't allow dragging sections over app items, unless it's the very last
                    // app item.
                    considerItem = (i == mAdapter.getItemCount() - 1);
                }
                if (considerItem) {
                    ApplistAdapter.ViewHolderBase viewHolder =
                            (ApplistAdapter.ViewHolderBase) mRecyclerView.findViewHolderForAdapterPosition(i);
                    if (viewHolder != null) {
                        viewHolder.layout.getLocationOnScreen(mDraggedOverViewLocation);
                        final int viewLeft = mDraggedOverViewLocation[0];
                        final int viewTop = mDraggedOverViewLocation[1];
                        final int viewRight = mDraggedOverViewLocation[0] + viewHolder.layout.getWidth();
                        final int viewBottom = mDraggedOverViewLocation[1] + viewHolder.layout.getHeight();
                        final float viewCenterX = viewLeft + (viewHolder.layout.getWidth() / 2.0f);
                        final float viewCenterY = viewTop + (viewHolder.layout.getHeight() / 2.0f);
                        final double distanceToView = distanceOfPoints(
                                viewCenterX, viewCenterY, draggedViewPosX, draggedViewPosY);
                        if (distanceToView < distanceToCandidateItem) {
                            candidateItemPosition = i;
                            candidateItem = mAdapter.getItem(i);
                            distanceToCandidateItem = distanceToView;
                            candidateViewLeft = viewLeft;
                            candidateViewRight = viewRight;
                            candidateViewTop = viewTop;
                            candidateViewBottom = viewBottom;
                        }
                    }
                }
            }

            if (candidateItem == null) {
                cleanDraggedOverItem();
            } else {
                cleanDraggedOverItem();
                mDraggedOverItem = candidateItem;
                if (draggedItem instanceof StartableItem) {
                    if (mDraggedOverItem instanceof StartableItem) {
                        if (mAdapter.isStartableLastInSection((StartableItem) mDraggedOverItem)) {
                            final int viewLeftSideCenterX = candidateViewLeft;
                            final int viewLeftSideCenterY = candidateViewTop + (candidateViewBottom - candidateViewTop) / 2;
                            final double distanceToLeftSideCenter = distanceOfPoints(
                                    viewLeftSideCenterX, viewLeftSideCenterY, draggedViewPosX, draggedViewPosY);
                            final int viewRightSideCenterX = candidateViewRight;
                            final int viewRightSideCenterY = candidateViewTop + (candidateViewBottom - candidateViewTop) / 2;
                            final double distanceToRightSideCenter = distanceOfPoints(
                                    viewRightSideCenterX, viewRightSideCenterY, draggedViewPosX, draggedViewPosY);
                            mDraggedOverItem.setDraggedOver(
                                    distanceToLeftSideCenter < distanceToRightSideCenter ? BaseItem.LEFT : BaseItem.RIGHT);
                        } else {
                            mDraggedOverItem.setDraggedOver(BaseItem.LEFT);
                        }
                        mAdapter.notifyItemChanged(candidateItemPosition);
                    } else if (mDraggedOverItem instanceof SectionItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                        mAdapter.notifyItemChanged(candidateItemPosition);
                    }
                } else if (draggedItem instanceof SectionItem) {
                    if (mDraggedOverItem instanceof StartableItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                        mAdapter.notifyItemChanged(candidateItemPosition);
                    } else if (mDraggedOverItem instanceof SectionItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.LEFT);
                        mAdapter.notifyItemChanged(candidateItemPosition);
                    }
                }
            }
        } else {
            cleanDraggedOverItem();
        }
    }

    private void cleanDraggedOverItem() {
        if (mDraggedOverItem != null) {
            mDraggedOverItem.setDraggedOver(BaseItem.NONE);
            mAdapter.notifyItemChanged(mAdapter.getItemPosition(mDraggedOverItem));
            mDraggedOverItem = null;
        }
    }

    private double distanceOfPoints(float p1x, float p1y, float p2x, float p2y) {
        final float a = p1x - p2x;
        final float b = p1y - p2y;
        return Math.sqrt(a * a + b * b);
    }

    private void scrollWhileDragging(DragGestureRecognizer gestureRecognizer) {
        final MotionEvent event = gestureRecognizer.getMotionEvent();
        final float fingerCurrentPosY = event.getRawY();

        final PointF fingerDownPos = gestureRecognizer.getFingerDownPos();
        final int direction = Math.signum(fingerCurrentPosY - fingerDownPos.y) > 0 ? 1 : -1;
        if (direction == -1) {
            final int firstItemPos = mLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (firstItemPos != 0) {
                mRecyclerView.getLocationOnScreen(mRecyclerViewLocation);
                final int recyclerViewTop = mRecyclerViewLocation[1];
                float scrollLimit = (fingerCurrentPosY - recyclerViewTop) / mRecyclerView.getHeight();
                if (scrollLimit < 0.20f) {
                    mRecyclerView.smoothScrollToPosition(0);
                } else {
                    mRecyclerView.stopScroll();
                }
            }
        } else {
            final int lastItemPos = mLayoutManager.findLastCompletelyVisibleItemPosition();
            if (lastItemPos != mAdapter.getItemCount() - 1) {
                mRecyclerView.getLocationOnScreen(mRecyclerViewLocation);
                final int recyclerViewBottom = mRecyclerViewLocation[1] + mRecyclerView.getHeight();
                float scrollLimit = (recyclerViewBottom - fingerCurrentPosY) / mRecyclerView.getHeight();
                if (scrollLimit < 0.20f) {
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                } else {
                    mRecyclerView.stopScroll();
                }
            }
        }
    }

    private void savePageToModel() {
        final String pageName = mApplistPagePageFragment.getPageName();
        final List<BaseItem> items = mAdapter.getAllItems();
        final boolean keepAppsSorted = mSettingsUtils.isKeepAppsSortedAlphabetically();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PageData pageData = ViewModelUtils.viewToModel(ApplistModel.INVALID_ID, pageName, items);
                mApplistModel.setPage(pageName, pageData);
                if (keepAppsSorted) {
                    mApplistModel.sortStartablesInPage(pageName);
                }
                return null;
            }
        });
    }

}


