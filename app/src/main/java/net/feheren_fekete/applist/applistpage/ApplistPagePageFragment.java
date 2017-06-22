package net.feheren_fekete.applist.applistpage;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.AppData;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.applistpage.model.PageData;
import net.feheren_fekete.applist.applistpage.model.SectionData;
import net.feheren_fekete.applist.applistpage.viewmodel.ViewModelUtils;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.*;
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistPagePageFragment extends Fragment implements ApplistAdapter.ItemListener {

    private static final String TAG = ApplistPagePageFragment.class.getSimpleName();

    // TODO: Inject these singletons.
    private ApplistModel mApplistModel = ApplistModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();
    private ScreenUtils mScreenUtils = ScreenUtils.getInstance();

    private Handler mHandler = new Handler();
    private BadgeStore mBadgeStore;
    private ApplistPreferences mApplistPreferences;
    private RecyclerView mRecyclerView;
    private ViewGroup mTouchOverlay;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private MyGridLayoutManager mLayoutManager;
    private DragGestureRecognizer mItemDragGestureRecognizer;
    private ApplistItemDragHandler mItemDragCallback;
    private @Nullable PopupMenu mItemMenu;
    private @Nullable BaseItem mItemMenuTarget;
    private ApplistItemDragHandler.Listener mListener;

    public static ApplistPagePageFragment newInstance(String pageName,
                                                      long launcherPageId,
                                                      IconCache iconCache,
                                                      ApplistItemDragHandler.Listener listener) {
        ApplistPagePageFragment fragment = new ApplistPagePageFragment();

        Bundle args = new Bundle();
        args.putString("pageName", pageName);
        args.putLong("launcherPageId", launcherPageId);
        fragment.setArguments(args);

        fragment.mIconCache = iconCache;
        fragment.mListener = listener;

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBadgeStore = new BadgeStore(
                getContext(),
                getContext().getPackageManager(),
                new BadgeUtils(getContext()));
        mApplistPreferences = new ApplistPreferences(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_page_page_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.applist_page_page_fragment_recycler_view);

        final int columnSize = Math.round(
                mScreenUtils.dpToPx(getContext(),
                        mSettingsUtils.getColumnWidth()));
        final int screenWidth = mScreenUtils.getScreenWidth(getContext());
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

        mAdapter = new ApplistAdapter(
                getContext(),
                this,
                getContext().getPackageManager(),
                mSettingsUtils,
                new FileUtils(),
                new BadgeStore(
                        getContext(),
                        getContext().getPackageManager(),
                        new BadgeUtils(getContext())),
                this,
                mIconCache);
        mRecyclerView.setAdapter(mAdapter);

        loadAllItems();

        mTouchOverlay = (ViewGroup) view.findViewById(R.id.applist_page_page_fragment_touch_overlay);
        mItemDragCallback = new ApplistItemDragHandler(
                getContext(), mScreenUtils, mSettingsUtils, this,
                mApplistModel, mTouchOverlay, mRecyclerView, mLayoutManager, mAdapter, mListener);
        mItemDragGestureRecognizer = new DragGestureRecognizer(mItemDragCallback, mTouchOverlay, mRecyclerView);

        return view;
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
        EventBus.getDefault().register(this);
        if (mApplistPreferences.getShowRearrangeItemsHelp()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.rearrange_items_help)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mApplistPreferences.setShowRearrangeItemsHelp(false);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter.isFilteredByName()) {
            deactivateNameFilter();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSectionsChangedEvent(ApplistModel.SectionsChangedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBadgeEvent(BadgeStore.BadgeEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    private long getLauncherPageId() {
        return getArguments().getLong("launcherPageId");
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

    public boolean isItemMenuOpen() {
        return mItemMenu != null;
    }

    public void closeItemMenu() {
        mItemMenu.dismiss();
    }

    @Nullable
    public BaseItem getItemMenuTarget() {
        return mItemMenuTarget;
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
        if (mSettingsUtils.isKeepAppsSortedAlphabetically()) {
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
                    mApplistModel.setSectionCollapsed(
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

    private void loadAllItems() {
        PageData pageData = mApplistModel.getPage(getPageName());
        if (pageData == null) {
            pageData = new PageData(ApplistModel.INVALID_ID, getPageName(), new ArrayList<SectionData>());
        }
        mAdapter.setItems(ViewModelUtils.modelToView(pageData));
        mScreenshotUtils.scheduleScreenshot(getActivity(), getLauncherPageId(), 500);
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
                                mApplistModel.setSectionName(pageName, oldSectionName, newSectionName);
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
                                mApplistModel.removeSection(pageName, sectionName);
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
                mApplistModel.sortAppsInSection(pageName, sectionName);
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
                                    mApplistModel.addNewSection(pageName, sectionName, true);
                                    if (appToMove != null) {
                                        AppData appData = new AppData(appToMove);
                                        mApplistModel.moveAppToSection(pageName, sectionName, appData);
                                    }
                                    return null;
                                }
                            });
                        }
                    }
                });
    }

}
