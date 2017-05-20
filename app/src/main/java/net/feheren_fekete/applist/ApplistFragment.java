package net.feheren_fekete.applist;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.model.AppData;
import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.model.PageData;
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

public class ApplistFragment extends Fragment
        implements ApplistAdapter.ItemListener, ApplistItemTouchHelperCallback.OnMoveListener {

    private static final String TAG = ApplistFragment.class.getSimpleName();

    public interface Listener {
        void onItemMoveStart();
        void onItemMoveEnd();
    }

    private BadgeStore mBadgeStore;
    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private View mTouchOverlay;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ItemTouchHelper mTouchHelper;
    private Handler mHandler = new Handler();
    private @Nullable PopupMenu mItemMenu;
    private @Nullable BaseItem mItemMenuTarget;
    private float mItemMoveThreshold;
    private @Nullable Listener mListener;

    public static ApplistFragment newInstance(String pageName,
                                              DataModel dataModel,
                                              IconCache iconCache) {
        ApplistFragment fragment = new ApplistFragment();

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
        View view = inflater.inflate(R.layout.applist_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.applist_fragment_recycler_view);

        final int columnSize = getResources().getDimensionPixelSize(R.dimen.appitem_width);
        final int screenWidth = ScreenUtils.getScreenWidth(getContext());
        final int columnCount = screenWidth / columnSize;
        mLayoutManager = new GridLayoutManager(getContext(), columnCount);
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

        mTouchOverlay = view.findViewById(R.id.applist_fragment_touch_overlay);
        mTouchOverlay.setOnTouchListener(mTouchOverlayListener);

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

        ItemTouchHelper.Callback callback = new ApplistItemTouchHelperCallback(this);
        mTouchHelper = new ItemTouchHelper(callback);
        mTouchHelper.attachToRecyclerView(mRecyclerView);

        mItemMoveThreshold = getResources().getDimension(R.dimen.applist_fragment_item_move_threshold);

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
            MenuItem deleteMenuItem = mItemMenu.getMenu().findItem(R.id.action_section_delete);
            deleteMenuItem.setVisible(false);
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
    public void onItemMoveStart(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ApplistAdapter.AppItemHolder) {
            ApplistAdapter.AppItemHolder appItemHolder = (ApplistAdapter.AppItemHolder) viewHolder;
            appItemHolder.appIcon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start();
            appItemHolder.appName.setVisibility(View.INVISIBLE);
        } else if (viewHolder instanceof ApplistAdapter.SectionItemHolder) {
            ApplistAdapter.SectionItemHolder sectionItemHolder = (ApplistAdapter.SectionItemHolder) viewHolder;
            sectionItemHolder.sectionName.setTypeface(null, Typeface.BOLD);
        }
    }

    @Override
    public boolean onItemMove(RecyclerView.ViewHolder fromViewHolder, RecyclerView.ViewHolder targetViewHolder) {
        return mAdapter.moveItem(fromViewHolder.getAdapterPosition(), targetViewHolder.getAdapterPosition());
    }

    @Override
    public void onItemMoveEnd(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ApplistAdapter.AppItemHolder) {
            ApplistAdapter.AppItemHolder appItemHolder = (ApplistAdapter.AppItemHolder) viewHolder;
            appItemHolder.appIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            appItemHolder.appName.setVisibility(View.VISIBLE);
        } else if (viewHolder instanceof ApplistAdapter.SectionItemHolder) {
            ApplistAdapter.SectionItemHolder sectionItemHolder = (ApplistAdapter.SectionItemHolder) viewHolder;
            sectionItemHolder.sectionName.setTypeface(null, Typeface.NORMAL);
            mAdapter.setTypeFilter(null);
        }
        finishItemMove();
        savePageToModel();
    }

    private View.OnTouchListener mTouchOverlayListener = new View.OnTouchListener() {
        private boolean isFingerDown;
        private PointF fingerDownPos = new PointF();
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    fingerDownPos.set(event.getRawX(), event.getRawY());
                    isFingerDown = true;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (isFingerDown) {
                        final boolean canMoveItem = !mAdapter.isFilteredByName()
                                && mItemMenu != null
                                && mItemMenuTarget != null;
                        if (canMoveItem) {
                            final float a = event.getRawX() - fingerDownPos.x;
                            final float b = event.getRawY() - fingerDownPos.y;
                            if (Math.sqrt(a * a + b * b) > mItemMoveThreshold) {
                                mItemMenu.dismiss();
                                if (mItemMenuTarget instanceof SectionItem) {
                                    mAdapter.setTypeFilter(SectionItem.class);
                                }
                                initiateItemMove(mItemMenuTarget.getId());
                            }
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    isFingerDown = false;
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    isFingerDown = false;
                    break;
                }
            }
            mRecyclerView.dispatchTouchEvent(event);
            return true;
        }
    };

    private void initiateItemMove(final long itemId) {
        if (mListener != null) {
            mListener.onItemMoveStart();
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForItemId(itemId);
                if (viewHolder != null) {
                    mTouchHelper.startDrag(viewHolder);
                }
            }
        });
    }

    private void finishItemMove() {
        if (mListener != null) {
            mListener.onItemMoveEnd();
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
            }
            mItemMenu = null;
            return handled;
        }
    };

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
                                mDataModel.storePages();
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
                                mDataModel.storePages();
                                return null;
                            }
                        });
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
                                    mDataModel.storePages();
                                    return null;
                                }
                            });
                        }
                    }
                });
    }

    private void savePageToModel() {
        final String pageName = getPageName();
        final List<BaseItem> items = mAdapter.getItems();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PageData pageData = ViewModelUtils.viewToModel(DataModel.INVALID_ID, pageName, items);
                mDataModel.setPage(pageName, pageData);
                return null;
            }
        });
    }

}
