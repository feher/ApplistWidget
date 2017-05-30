package net.feheren_fekete.applist.applist;

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
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.model.PageData;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.viewmodel.AppItem;
import net.feheren_fekete.applist.viewmodel.BaseItem;
import net.feheren_fekete.applist.viewmodel.SectionItem;
import net.feheren_fekete.applist.viewmodel.ViewModelUtils;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public class ApplistItemDragHandler implements DragGestureRecognizer.Callback {

    public interface Listener {
        void onItemDragStart();
        void onItemDragEnd();
    }

    private Context mContext;
    private ApplistPageFragment mApplistPageFragment;
    private DataModel mDataModel;
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
                                  ApplistPageFragment applistPageFragment,
                                  DataModel dataModel,
                                  ViewGroup touchOverlay,
                                  RecyclerView recyclerView,
                                  MyGridLayoutManager layoutManager,
                                  ApplistAdapter adapter,
                                  Listener listener) {
        mContext = context;
        mApplistPageFragment = applistPageFragment;
        mDataModel = dataModel;
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
                && mApplistPageFragment.isItemMenuOpen();
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
        mApplistPageFragment.closeItemMenu();

        // The RecyclerView is under the AppBarLayout. But the AppBarLayout is
        // behind the transparent status bar. So, if we hide the AppBarLayout the
        // RecyclerView would get behind the status bar.
        // We don't want that. So we add a top padding.
        mRecyclerView.setPadding(0, ScreenUtils.getStatusBarHeight(mContext), 0, 0);
        mListener.onItemDragStart();

        final BaseItem draggedItem = mApplistPageFragment.getItemMenuTarget();
        if (draggedItem instanceof AppItem) {
            mAdapter.setEnabled(draggedItem, false);
        } else if (draggedItem instanceof SectionItem) {
            mAdapter.setAllAppsEnabled(false);
            mAdapter.setSectionsHighlighted(true);
        }

        addDraggedView(gestureRecognizer, draggedItem);
    }

    @Override
    public void onDragging(DragGestureRecognizer gestureRecognizer) {
        if (mDraggedView != null) {
            final BaseItem draggedItem = mApplistPageFragment.getItemMenuTarget();
            updateDraggedViewPosition(gestureRecognizer, draggedItem);
            updateDraggedOverItem(gestureRecognizer, draggedItem);
            scrollWhileDragging(gestureRecognizer);
        }
    }

    @Override
    public void onDrop(DragGestureRecognizer gestureRecognizer) {
        if (mDraggedOverItem != null) {
            boolean canDrop = true;
            final BaseItem draggedItem = mApplistPageFragment.getItemMenuTarget();
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
        final BaseItem draggedItem = mApplistPageFragment.getItemMenuTarget();
        if (draggedItem instanceof AppItem) {
            mAdapter.setEnabled(draggedItem, true);
        } else if (draggedItem instanceof SectionItem) {
            mAdapter.setAllAppsEnabled(true);
            mAdapter.setSectionsHighlighted(false);
        }

        if (mDraggedOverItem != null) {
            mDraggedOverItem.setDraggedOver(BaseItem.NONE);
            mAdapter.notifyItemChanged(mAdapter.getItemPosition(mDraggedOverItem));
        }

        removeDraggedView();

        // Remove the top padding we added in onStartDragging().
        mRecyclerView.setPadding(0, 0, 0, 0);
        mListener.onItemDragEnd();
    }

    private void addDraggedView(DragGestureRecognizer gestureRecognizer, BaseItem draggedItem) {
        if (draggedItem instanceof AppItem) {
            ApplistAdapter.AppItemHolder appItemHolder =
                    (ApplistAdapter.AppItemHolder) mRecyclerView.findViewHolderForItemId(draggedItem.getId());
            ImageView imageView = new ImageView(mContext);
            imageView.setImageDrawable(appItemHolder.appIcon.getDrawable());
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
        if (draggedItem instanceof AppItem) {
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
        if (draggedItem instanceof AppItem) {
            draggedViewPosX -= mDraggedAppViewSize / 2;
            draggedViewPosY -= mDraggedAppViewSize;
        } else {
            draggedViewPosX -= mDraggedSectionViewSize / 2;
            draggedViewPosY -= mDraggedSectionViewSize / 3;
        }

        final int firstItemPos = mLayoutManager.findFirstVisibleItemPosition();
        final int lastItemPos = mLayoutManager.findLastVisibleItemPosition();

        if (mDraggedOverItem != null) {
            mDraggedOverItem.setDraggedOver(BaseItem.NONE);
            mAdapter.notifyItemChanged(mAdapter.getItemPosition(mDraggedOverItem));
            mDraggedOverItem = null;
        }

        if (mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {

            int closestItemPosition = RecyclerView.NO_POSITION;
            BaseItem closestItem = null;
            double distanceToClosestItem = Double.MAX_VALUE;
            int closestViewLeft = 0;
            int closestViewTop = 0;
            int closestViewRight = 0;
            int closestViewBottom = 0;
            for (int i = firstItemPos; i <= lastItemPos; ++i) {
                boolean considerItem = true;
                BaseItem item = mAdapter.getItem(i);
                if (draggedItem instanceof AppItem
                        && item instanceof SectionItem) {
                    SectionItem sectionItem = (SectionItem) item;
                    if (!sectionItem.isCollapsed() && !mAdapter.isSectionEmpty(sectionItem)) {
                        // We don't allow dragging over open and not empty (i.e. normal) section
                        // headers.
                        considerItem = false;
                    }
                }
                if (draggedItem instanceof SectionItem
                        && item instanceof AppItem) {
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
                        if (distanceToView < distanceToClosestItem) {
                            closestItemPosition = i;
                            closestItem = mAdapter.getItem(i);
                            distanceToClosestItem = distanceToView;
                            closestViewLeft = viewLeft;
                            closestViewRight = viewRight;
                            closestViewTop = viewTop;
                            closestViewBottom = viewBottom;
                        }
                    }
                }
            }

            if (closestItem != null) {
                mDraggedOverItem = mAdapter.getItem(closestItemPosition);
                if (draggedItem instanceof AppItem) {
                    if (mDraggedOverItem instanceof AppItem) {
                        if (mAdapter.isAppLastInSection((AppItem) mDraggedOverItem)) {
                            final int viewLeftSideCenterX = closestViewLeft;
                            final int viewLeftSideCenterY = closestViewTop + (closestViewBottom - closestViewTop) / 2;
                            final double distanceToLeftSideCenter = distanceOfPoints(
                                    viewLeftSideCenterX, viewLeftSideCenterY, draggedViewPosX, draggedViewPosY);
                            final int viewRightSideCenterX = closestViewRight;
                            final int viewRightSideCenterY = closestViewTop + (closestViewBottom - closestViewTop) / 2;
                            final double distanceToRightSideCenter = distanceOfPoints(
                                    viewRightSideCenterX, viewRightSideCenterY, draggedViewPosX, draggedViewPosY);
                            mDraggedOverItem.setDraggedOver(
                                    distanceToLeftSideCenter < distanceToRightSideCenter ? BaseItem.LEFT : BaseItem.RIGHT);
                        } else {
                            mDraggedOverItem.setDraggedOver(BaseItem.LEFT);
                        }
                        mAdapter.notifyItemChanged(closestItemPosition);
                    } else if (mDraggedOverItem instanceof SectionItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                        mAdapter.notifyItemChanged(closestItemPosition);
                    }
                } else if (draggedItem instanceof SectionItem) {
                    if (mDraggedOverItem instanceof AppItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                        mAdapter.notifyItemChanged(closestItemPosition);
                    } else if (mDraggedOverItem instanceof SectionItem) {
                        mDraggedOverItem.setDraggedOver(BaseItem.LEFT);
                        mAdapter.notifyItemChanged(closestItemPosition);
                    }
                }
            }
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
        final String pageName = mApplistPageFragment.getPageName();
        final List<BaseItem> items = mAdapter.getAllItems();
        final boolean keepAppsSorted = SettingsUtils.isKeepAppsSortedAlphabetically(mContext.getApplicationContext());
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PageData pageData = ViewModelUtils.viewToModel(DataModel.INVALID_ID, pageName, items);
                mDataModel.setPage(pageName, pageData);
                if (keepAppsSorted) {
                    mDataModel.sortAppsInPage(pageName);
                }
                return null;
            }
        });
    }

}


