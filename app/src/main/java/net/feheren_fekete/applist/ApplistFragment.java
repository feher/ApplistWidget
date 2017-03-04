package net.feheren_fekete.applist;

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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.model.AppData;
import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.*;
import net.feheren_fekete.applist.viewmodel.AppItem;
import net.feheren_fekete.applist.viewmodel.SectionItem;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistFragment extends Fragment implements ApplistAdapter.ItemListener {

    private static final String TAG = ApplistFragment.class.getSimpleName();

    public interface Listener {
        void onChangeSectionOrderStart();
        void onChangeSectionOrderEnd();
    }

    private BadgeStore mBadgeStore;
    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ItemTouchHelper mTouchHelper;
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

        ItemTouchHelper.Callback callback =
                new ApplistItemTouchHelperCallback(mAdapter);
        mTouchHelper = new ItemTouchHelper(callback);
        mTouchHelper.attachToRecyclerView(mRecyclerView);
        mAdapter.setItemTouchHelper(mTouchHelper);

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
            cancelChangingOrder();
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
            case R.id.action_done:
                finishChangingOrder();
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(appItem.getName());
        alertDialogBuilder.setItems(R.array.app_item_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        moveAppToSection(appItem);
                        break;
                    case 1:
                        showAppInfo(appItem);
                        break;
                    case 2:
                        uninstallApp(appItem);
                        break;
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
        ComponentName phoneAppComponentName = AppUtils.getPhoneApp(getContext());
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
    public void onSectionLongTapped(final SectionItem sectionItem) {
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
                                    changeSectionOrder();
                                    break;
                                case 1:
                                    renameSection(sectionItem);
                                    break;
                                case 2:
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
                                    changeSectionOrder();
                                    break;
                                case 1:
                                    renameSection(sectionItem);
                                    break;
                            }
                        }
                    });
        }
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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

    private void setDataModel(DataModel dataModel) {
        mDataModel = dataModel;
    }

    private void setIconCache(IconCache iconCache) {
        mIconCache = iconCache;
    }

    private void moveAppToSection(final AppItem appItem) {
        final String pageName = getPageName();
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return mDataModel.getSectionNames(pageName);
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                chooseSectionAndMoveApp(task.getResult(), appItem);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void chooseSectionAndMoveApp(final List<String> sectionNames,
                                         final AppItem appItem) {
        final int newSectionIndex = sectionNames.size();
        sectionNames.add(getResources().getString(R.string.menu_item_move_to_new_new_section));

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        String[] sections = new String[sectionNames.size()];
        sectionNames.toArray(sections);
        alertDialogBuilder.setTitle(R.string.dialog_move_to_section_title);
        alertDialogBuilder.setItems(sections, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int index) {
                if (index == newSectionIndex) {
                    createSection(appItem);
                } else {
                    final String pageName = getPageName();
                    final String sectionName = sectionNames.get(index);
                    Task.callInBackground(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            AppData appData = new AppData(appItem);
                            mDataModel.moveAppToSection(pageName, sectionName, appData);
                            return null;
                        }
                    });
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
        ApplistDialogs.questionDialog(
                getActivity(), R.string.remove_section,
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
                            mAdapter.getSectionNames();
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

    private void changeSectionOrder() {
        if (mAdapter.isChangingOrder() || mAdapter.isFilteredByName()) {
            return;
        }

        mAdapter.setTypeFilter(SectionItem.class);
        mAdapter.setChangingOrder(true);
        getActivity().invalidateOptionsMenu();
        if (mListener != null) {
            mListener.onChangeSectionOrderStart();
        }
    }

    private void finishChangingOrder() {
        if (!mAdapter.isChangingOrder()) {
            return;
        }

        final String pageName = getPageName();
        final List<String> sectionNames = mAdapter.getSectionNames();
        mAdapter.setChangingOrder(false);
        mAdapter.setTypeFilter(null);
        getActivity().invalidateOptionsMenu();

        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mDataModel.setSectionOrder(pageName, sectionNames, true);
                return null;
            }
        });

        if (mListener != null) {
            mListener.onChangeSectionOrderEnd();
        }
    }

    public void cancelChangingOrder() {
        if (!mAdapter.isChangingOrder()) {
            return;
        }

        mAdapter.setTypeFilter(null);
        mAdapter.setChangingOrder(false);
        getActivity().invalidateOptionsMenu();

        if (mListener != null) {
            mListener.onChangeSectionOrderEnd();
        }
    }

}
