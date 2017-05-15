package net.feheren_fekete.applist;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
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
        void onChangeItemOrderStart();
        void onChangeItemOrderEnd();
    }

    private BadgeStore mBadgeStore;
    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ItemTouchHelper mTouchHelper;
    private @Nullable Listener mListener;
    private Handler mHandler = new Handler();

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

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
        if (mAdapter.isChangingOrder()) {
            finishChangingOrder();
        }
        if (mAdapter.isFilteredByName()) {
            deactivateNameFilter();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
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

    public boolean isChangingOrder() {
        return mAdapter.isChangingOrder();
    }

    public boolean handleMenuItem(int itemId) {
        boolean isHandled = false;
        switch (itemId) {
            case R.id.action_edit_page:
                changeItemOrder();
                isHandled = true;
                break;
            case R.id.action_create_section:
                createSection(null);
                isHandled = true;
                break;
        }
        return isHandled;
    }

    @Override
    public void onAppLongTapped(final AppItem appItem) {
        if (mAdapter.isChangingOrder()) {
            ApplistAdapter.AppItemHolder appItemHolder =
                    (ApplistAdapter.AppItemHolder) mRecyclerView.findViewHolderForItemId(
                            appItem.getId());
            mTouchHelper.startDrag(appItemHolder);
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setTitle(appItem.getName());
            alertDialogBuilder.setItems(R.array.app_item_menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            showAppInfo(appItem);
                            break;
                        case 1:
                            uninstallApp(appItem);
                            break;
                    }
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    @Override
    public void onAppTapped(AppItem appItem) {
        if (mAdapter.isChangingOrder()) {
            return;
        }

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
        if (mAdapter.isChangingOrder()) {
            mAdapter.setTypeFilter(SectionItem.class);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ApplistAdapter.SectionItemHolder sectionItemHolder =
                            (ApplistAdapter.SectionItemHolder) mRecyclerView.findViewHolderForItemId(
                                    sectionItem.getId());
                    mTouchHelper.startDrag(sectionItemHolder);
                }
            });
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
            alertDialogBuilder.setTitle(sectionItem.getName());
            if (sectionItem.isRemovable()) {
                alertDialogBuilder.setItems(
                        R.array.section_item_menu,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        renameSection(sectionItem);
                                        break;
                                    case 1:
                                        deleteSection(sectionItem);
                                        break;
                                }
                            }
                        });
            } else {
                alertDialogBuilder.setItems(
                        R.array.section_item_menu_readonly,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        renameSection(sectionItem);
                                        break;
                                }
                            }
                        });
            }
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
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
            appItemHolder.appName.setVisibility(View.GONE);
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
        setPageToModel();
    }

    public void finishChangingOrder() {
        if (!mAdapter.isChangingOrder()) {
            return;
        }

        mAdapter.setChangingOrder(false);
        mAdapter.setTypeFilter(null);
        getActivity().invalidateOptionsMenu();

        if (mListener != null) {
            mListener.onChangeItemOrderEnd();
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

    private void changeItemOrder() {
        if (mAdapter.isChangingOrder() || mAdapter.isFilteredByName()) {
            return;
        }

        mAdapter.setChangingOrder(true);
        getActivity().invalidateOptionsMenu();
        if (mListener != null) {
            mListener.onChangeItemOrderStart();
        }
    }

    private void setPageToModel() {
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
