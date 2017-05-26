package net.feheren_fekete.applist.applist;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.model.AppData;
import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.model.PageData;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.*;
import net.feheren_fekete.applist.viewmodel.AppItem;
import net.feheren_fekete.applist.viewmodel.BaseItem;
import net.feheren_fekete.applist.viewmodel.SectionItem;
import net.feheren_fekete.applist.viewmodel.ViewModelUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistPageFragment extends Fragment
        implements ApplistAdapter.ItemListener, ApplistItemDragHandler.Callback {

    private static final String TAG = ApplistPageFragment.class.getSimpleName();

    public interface Listener {
        void onItemMoveStart();
        void onItemMoveEnd();
    }

    private BadgeStore mBadgeStore;
    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private ViewGroup mTouchOverlay;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private MyGridLayoutManager mLayoutManager;
    private ApplistItemDragHandler mItemDragHandler;
    private View mDraggedView;
    private View mDraggedSectionView;
    private int mDraggedAppViewSize;
    private int mDraggedSectionViewSize;
    private @Nullable BaseItem mDraggedOverItem;
    private int[] mRecyclerViewLocation = new int[2];
    private int[] mDraggedOverViewLocation = new int[2];
    private @Nullable PopupMenu mItemMenu;
    private @Nullable BaseItem mItemMenuTarget;
    private float mItemMoveThreshold;
    private @Nullable Listener mListener;

    public static ApplistPageFragment newInstance(String pageName,
                                                  DataModel dataModel,
                                                  IconCache iconCache) {
        ApplistPageFragment fragment = new ApplistPageFragment();

        Bundle args = new Bundle();
        args.putString("pageName", pageName);
        fragment.setArguments(args);
        fragment.setDataModel(dataModel);
        fragment.setIconCache(iconCache);

        return fragment;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_page_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.applist_page_fragment_recycler_view);

        final int columnSize = Math.round(
                ScreenUtils.dpToPx(getContext(),
                        SettingsUtils.getColumnWidth(getContext())));
        final int screenWidth = ScreenUtils.getScreenWidth(getContext());
        final int columnCount = screenWidth / columnSize;
        mLayoutManager = new MyGridLayoutManager(getContext(), columnCount);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case ApplistAdapter.APP_ITEM_VIEW:
                        return 1;
                    case ApplistAdapter.SECTION_ITEM_VIEW:
                        return columnCount;
                    default:
                        return -1;
                }
            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);

        mTouchOverlay = (ViewGroup) view.findViewById(R.id.applist_page_fragment_touch_overlay);
        mItemDragHandler = new ApplistItemDragHandler(this, mTouchOverlay, mRecyclerView);
        mItemMoveThreshold = getResources().getDimension(R.dimen.applist_fragment_item_move_threshold);
        mDraggedAppViewSize = getResources().getDimensionPixelSize(R.dimen.appitem_icon_size);
        mDraggedSectionViewSize = getResources().getDimensionPixelSize(R.dimen.dragged_section_item_width);

        mAdapter = new ApplistAdapter(
                getContext(),
                this,
                getContext().getPackageManager(),
                mDataModel,
                new BadgeStore(
                        getContext(),
                        getContext().getPackageManager(),
                        new BadgeUtils(getContext())),
                getPageName(),
                this,
                mIconCache);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.loadAllItems();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBadgeStore = new BadgeStore(
                getContext(),
                getContext().getPackageManager(),
                new BadgeUtils(getContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter.isFilteredByName()) {
            deactivateNameFilter();
        }
    }

    public void update() {
        mAdapter.loadAllItems();
    }

    public String getPageName() {
        return getArguments().getString("pageName");
    }

    public boolean isFilteredByName() {
        return mAdapter.isFilteredByName();
    }

    public void activateNameFilter() {
        if (mAdapter.isFilteredByName()) {
            return;
        }

        setNameFilter("");
    }

    public void deactivateNameFilter() {
        if (!mAdapter.isFilteredByName()) {
            return;
        }

        setNameFilter(null);
    }

    public void setNameFilter(String filterText) {
        mAdapter.setNameFilter(filterText);
        mRecyclerView.scrollToPosition(0);
    }

    public boolean handleMenuItem(int itemId) {
        boolean isHandled = false;
        switch (itemId) {
            case R.id.action_create_section:
                createSection(null);
                isHandled = true;
                break;
        }
        return isHandled;
    }

    @Override
    public void onAppLongTapped(final AppItem appItem) {
        ApplistAdapter.AppItemHolder appItemHolder =
                (ApplistAdapter.AppItemHolder) mRecyclerView.findViewHolderForItemId(
                        appItem.getId());
        mItemMenuTarget = appItem;
        mItemMenu = new PopupMenu(getContext(), appItemHolder.layout);
        mItemMenu.setOnMenuItemClickListener(mItemMenuClickListener);
        mItemMenu.inflate(R.menu.app_item_menu);
        mItemMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                mItemMenu = null;
            }
        });
        mItemMenu.show();
    }

    @Override
    public void onAppTapped(AppItem appItem) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ComponentName appComponentName = new ComponentName(
                appItem.getPackageName(), appItem.getComponentName());
        intent.setComponent(appComponentName);
        getContext().startActivity(intent);

        ComponentName smsAppComponentName = AppUtils.getSmsApp(getContext());
        if (appComponentName.equals(smsAppComponentName)) {
            mBadgeStore.setBadgeCount(
                    smsAppComponentName.getPackageName(),
                    smsAppComponentName.getClassName(),
                    0);
        }
        ComponentName phoneAppComponentName = AppUtils.getPhoneApp(getContext().getApplicationContext());
        if (appComponentName.equals(phoneAppComponentName)) {
            mBadgeStore.setBadgeCount(
                    phoneAppComponentName.getPackageName(),
                    phoneAppComponentName.getClassName(),
                    0);
        }

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onAppTouched(final AppItem appItem) {
    }

    @Override
    public void onSectionLongTapped(final SectionItem sectionItem) {
        ApplistAdapter.SectionItemHolder sectionItemHolder =
                (ApplistAdapter.SectionItemHolder) mRecyclerView.findViewHolderForItemId(
                        sectionItem.getId());
        mItemMenuTarget = sectionItem;
        mItemMenu = new PopupMenu(getContext(), sectionItemHolder.layout);
        mItemMenu.setOnMenuItemClickListener(mItemMenuClickListener);
        mItemMenu.inflate(R.menu.section_item_menu);
        mItemMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                mItemMenu = null;
            }
        });
        if (!sectionItem.isRemovable()) {
            mItemMenu.getMenu().findItem(R.id.action_section_delete).setVisible(false);
        }
        if (SettingsUtils.isKeepAppsSortedAlphabetically(getContext().getApplicationContext())) {
            mItemMenu.getMenu().findItem(R.id.action_section_sort_apps).setVisible(false);
        }
        mItemMenu.show();
    }

    @Override
    public void onSectionTapped(final SectionItem sectionItem) {
        final String pageName = getPageName();
        final boolean wasSectionCollapsed = sectionItem.isCollapsed();
        if (!mAdapter.isFilteredByName()) {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mDataModel.setSectionCollapsed(
                            pageName,
                            sectionItem.getName(),
                            !wasSectionCollapsed);
                    return null;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (wasSectionCollapsed) {
                                int position = mAdapter.getItemPosition(sectionItem);
                                if (position != RecyclerView.NO_POSITION) {
                                    int firstPosition = mLayoutManager.findFirstVisibleItemPosition();
                                    int firstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                                    View firstVisibleView = mRecyclerView.getChildAt(firstVisiblePosition - firstPosition);
                                    int toY = firstVisibleView.getTop();
                                    View thisView = mRecyclerView.getChildAt(position - firstPosition);
                                    int fromY = thisView.getTop();
                                    mRecyclerView.smoothScrollBy(0, fromY - toY);
                                }
                            }
                        }
                    }, 200);
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    @Override
    public void onSectionTouched(final SectionItem sectionItem) {
    }

    @Override
    public boolean canDrag() {
        return !mAdapter.isFilteredByName()
                && mItemMenu != null
                && mItemMenuTarget != null;
    }

    @Override
    public boolean canStartDragging() {
        MotionEvent event = mItemDragHandler.getMotionEvent();
        PointF fingerDownPos = mItemDragHandler.getFingerDownPos();
        float a = event.getRawX() - fingerDownPos.x;
        float b = event.getRawY() - fingerDownPos.y;
        double distance = Math.sqrt(a * a + b * b);
        return (distance > mItemMoveThreshold);
    }

    @Override
    public void onStartDragging() {
        mItemMenu.dismiss();
        if (mListener != null) {
//            mListener.onItemMoveStart();
        }

        mAdapter.setEnabled(mItemMenuTarget, false);
        mAdapter.setSectionsHighlighted(true);

        addDraggedView();
    }

    @Override
    public void onDragging() {
        if (mDraggedView != null) {
            updateDraggedViewPosition();
            updateDraggedOverItem();
            scrollWhileDragging();
        }
    }

    @Override
    public void onDrop() {
        if (mDraggedOverItem != null) {
            int fromPosition = mAdapter.getItemPosition(mItemMenuTarget);
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

    @Override
    public void onStopDragging() {
        mAdapter.setEnabled(mItemMenuTarget, true);
        mAdapter.setSectionsHighlighted(false);

        if (mDraggedOverItem != null) {
            mDraggedOverItem.setDraggedOver(BaseItem.NONE);
            mAdapter.notifyItemChanged(mAdapter.getItemPosition(mDraggedOverItem));
        }

        removeDraggedView();

        if (mListener != null) {
            // This makes appbar move down
//            mListener.onItemMoveEnd();
        }
    }

    private PopupMenu.OnMenuItemClickListener mItemMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            boolean handled = false;
            switch (menuItem.getItemId()) {
                case R.id.action_app_info:
                    showAppInfo((AppItem) mItemMenuTarget);
                    handled = true;
                    break;
                case R.id.action_app_uninstall:
                    uninstallApp((AppItem) mItemMenuTarget);
                    handled = true;
                    break;
                case R.id.action_section_rename:
                    renameSection((SectionItem) mItemMenuTarget);
                    handled = true;
                    break;
                case R.id.action_section_delete:
                    deleteSection((SectionItem) mItemMenuTarget);
                    handled = true;
                    break;
                case R.id.action_section_sort_apps:
                    sortSection((SectionItem) mItemMenuTarget);
                    handled = true;
                    break;
            }
            mItemMenu = null;
            return handled;
        }
    };

    private void addDraggedView() {
        if (mItemMenuTarget instanceof AppItem) {
            ApplistAdapter.AppItemHolder appItemHolder =
                    (ApplistAdapter.AppItemHolder) mRecyclerView.findViewHolderForItemId(mItemMenuTarget.getId());
            ImageView imageView = new ImageView(getContext());
            imageView.setImageDrawable(appItemHolder.appIcon.getDrawable());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mDraggedAppViewSize, mDraggedAppViewSize);
            imageView.setLayoutParams(layoutParams);
            mDraggedView = imageView;
        } else {
            if (mDraggedSectionView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                mDraggedSectionView = inflater.inflate(R.layout.dragged_section_item, null, false);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        mDraggedSectionViewSize, ViewGroup.LayoutParams.WRAP_CONTENT);
                mDraggedSectionView.setLayoutParams(layoutParams);
            }
            SectionItem sectionItem = (SectionItem) mItemMenuTarget;
            TextView textView = (TextView) mDraggedSectionView.findViewById(R.id.dragged_section_item_name);
            textView.setText(sectionItem.getName());
            mDraggedView = mDraggedSectionView;
        }
        updateDraggedViewPosition();
        mTouchOverlay.addView(mDraggedView);
    }

    private void updateDraggedViewPosition() {
        final MotionEvent event = mItemDragHandler.getMotionEvent();
        final float fingerRawX = event.getRawX();
        final float fingerRawY = event.getRawY();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mDraggedView.getLayoutParams();
        mRecyclerView.getLocationOnScreen(mRecyclerViewLocation);
        layoutParams.leftMargin = Math.round(fingerRawX - mRecyclerViewLocation[0]);
        layoutParams.topMargin = Math.round(fingerRawY - mRecyclerViewLocation[1]);
        if (mItemMenuTarget instanceof AppItem) {
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

    private void updateDraggedOverItem() {
        final MotionEvent event = mItemDragHandler.getMotionEvent();
        final float fingerCurrentPosX = event.getRawX();
        final float fingerCurrentPosY = event.getRawY();
        float draggedViewPosX = fingerCurrentPosX;
        float draggedViewPosY = fingerCurrentPosY;
        if (mItemMenuTarget instanceof AppItem) {
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
                if (mItemMenuTarget instanceof AppItem
                        && item instanceof SectionItem) {
                    SectionItem sectionItem = (SectionItem) item;
                    if (!sectionItem.isCollapsed() && !mAdapter.isSectionEmpty(sectionItem)) {
                        // We don't allow dragging over open and not empty (i.e. normal) section
                        // headers.
                        considerItem = false;
                    }
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
                if (mDraggedOverItem == mItemMenuTarget) {
                    mDraggedOverItem = null;
                } else if (mItemMenuTarget instanceof AppItem) {
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
                        SectionItem sectionItem = (SectionItem) mDraggedOverItem;
                        if (sectionItem.isCollapsed() || mAdapter.isSectionEmpty(sectionItem)) {
                            mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                            mAdapter.notifyItemChanged(closestItemPosition);
                        } else {
                            mDraggedOverItem = null;
                        }
                    }
                } else if (mItemMenuTarget instanceof SectionItem) {
                    if (mDraggedOverItem instanceof AppItem) {
                        final boolean isLastItem = (closestItemPosition == mAdapter.getItemCount() - 1);
                        if (isLastItem) {
                            mDraggedOverItem.setDraggedOver(BaseItem.RIGHT);
                            mAdapter.notifyItemChanged(closestItemPosition);
                        } else {
                            mDraggedOverItem = null;
                        }
                    } else if (mDraggedOverItem instanceof SectionItem) {
                        if (closestItemPosition != mAdapter.getNextSectionPosition(mItemMenuTarget)) {
                            mDraggedOverItem.setDraggedOver(BaseItem.LEFT);
                            mAdapter.notifyItemChanged(closestItemPosition);
                        } else {
                            mDraggedOverItem = null;
                        }
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

    private void scrollWhileDragging() {
        final MotionEvent event = mItemDragHandler.getMotionEvent();
        final float fingerCurrentPosY = event.getRawY();

        final PointF fingerDownPos = mItemDragHandler.getFingerDownPos();
        final int direction = Math.signum(fingerCurrentPosY - fingerDownPos.y) > 0 ? 1 : -1;
        if (direction == -1) {
            final int firstItemPos = mLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (firstItemPos != 0) {
                mRecyclerView.getLocationOnScreen(mRecyclerViewLocation);
                final int recyclerViewTop = mRecyclerViewLocation[1];
                float scrollLimit = (fingerCurrentPosY - recyclerViewTop) / (fingerDownPos.y - recyclerViewTop);
                if (scrollLimit < 0.30f) {
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
                float scrollLimit = (recyclerViewBottom - fingerCurrentPosY) / (recyclerViewBottom - fingerDownPos.y);
                if (scrollLimit < 0.30f) {
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                } else {
                    mRecyclerView.stopScroll();
                }
            }
        }
    }

    private void setDataModel(DataModel dataModel) {
        mDataModel = dataModel;
    }

    private void setIconCache(IconCache iconCache) {
        mIconCache = iconCache;
    }

    private void showAppInfo(AppItem appItem) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", appItem.getPackageName(), null);
        intent.setData(uri);
        getContext().startActivity(intent);
    }

    private void uninstallApp(AppItem appItem) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        Uri uri = Uri.fromParts("package", appItem.getPackageName(), null);
        intent.setData(uri);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, false);
        getContext().startActivity(intent);
    }

    private void renameSection(SectionItem sectionItem) {
        final String pageName = getPageName();
        final String oldSectionName = sectionItem.getName();
        final List<String> sectionNames = mAdapter.getSectionNames();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, oldSectionName,
                new RunnableWithRetArg<String, String>() {
                     @Override
                    public String run(String sectionName) {
                        if (sectionNames.contains(sectionName)) {
                            return getResources().getString(R.string.dialog_error_section_exists);
                        }
                        return null;
                    }
                },
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String newSectionName) {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mDataModel.setSectionName(pageName, oldSectionName, newSectionName);
                                return null;
                            }
                        });
                    }
                });
    }

    private void deleteSection(SectionItem sectionItem) {
        final String sectionName = sectionItem.getName();
        final String pageName = getPageName();
        final String uncategorizedSectionName = mAdapter.getUncategorizedSectionName();
        ApplistDialogs.questionDialog(
                getActivity(),
                getResources().getString(R.string.remove_section_title),
                getResources().getString(R.string.remove_section_message, sectionName, uncategorizedSectionName),
                new Runnable() {
                    @Override
                    public void run() {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mDataModel.removeSection(pageName, sectionName);
                                return null;
                            }
                        });
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        // Nothing.
                    }
                });
    }

    private void sortSection(SectionItem sectionItem) {
        final String sectionName = sectionItem.getName();
        final String pageName = getPageName();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mDataModel.sortAppsInSection(pageName, sectionName);
                return null;
            }
        });
    }

    private void createSection(@Nullable final AppItem appToMove) {
        final String pageName = getPageName();
        final List<String> sectionNames = mAdapter.getSectionNames();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, "",
                new RunnableWithRetArg<String, String>() {
                    @Override
                    public String run(String sectionName) {
                        if (sectionNames.contains(sectionName)) {
                            return getResources().getString(R.string.dialog_error_section_exists);
                        }
                        return null;
                    }
                },
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String sectionName) {
                        if (!sectionName.isEmpty()) {
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    mDataModel.addNewSection(pageName, sectionName, true);
                                    if (appToMove != null) {
                                        AppData appData = new AppData(appToMove);
                                        mDataModel.moveAppToSection(pageName, sectionName, appData);
                                    }
                                    return null;
                                }
                            });
                        }
                    }
                });
    }

    private void savePageToModel() {
        final String pageName = getPageName();
        final List<BaseItem> items = mAdapter.getAllItems();
        final boolean keepAppsSorted = SettingsUtils.isKeepAppsSortedAlphabetically(getContext().getApplicationContext());
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
