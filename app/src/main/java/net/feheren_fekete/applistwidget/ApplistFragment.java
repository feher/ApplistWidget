package net.feheren_fekete.applistwidget;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applistwidget.model.AppData;
import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.utils.RunnableWithArg;
import net.feheren_fekete.applistwidget.viewmodel.AppItem;
import net.feheren_fekete.applistwidget.viewmodel.SectionItem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import de.greenrobot.event.EventBus;

public class ApplistFragment extends Fragment implements ApplistAdapter.ItemListener {

    private static final String TAG = ApplistFragment.class.getSimpleName();

    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ItemTouchHelper mTouchHelper;
    private Map<String, Boolean> mSectionCollapsedStates;

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

        mLayoutManager = new GridLayoutManager(getContext(), 4);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch(mAdapter.getItemViewType(position)){
                    case ApplistAdapter.APP_ITEM_VIEW:
                        return 1;
                    case ApplistAdapter.SECTION_ITEM_VIEW:
                        return 4;
                    default:
                        return -1;
                }
            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ApplistAdapter(
                getContext(),
                getContext().getPackageManager(),
                mDataModel,
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "ZIZI FRAG START");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ZIZI FRAG RESUME");
        EventBus.getDefault().register(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "ZIZI FRAG PAUSE");
        EventBus.getDefault().unregister(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "ZIZI FRAG STOP");
        if (mAdapter.isChangingOrder()) {
            cancelChangingOrder();
        }
        if (mAdapter.isFilteredByName()) {
            deactivateFilter();
        }
    }

    public void update() {
        mAdapter.loadAllItems();
    }

    public String getPageName() {
        return getArguments().getString("pageName");
    }

    public void activateFilter() {
        Log.d(TAG, "ZIZI FILTER ACTIVATE");
        if (mAdapter.isFilteredByName()) {
            return;
        }

        setFilter("");
    }

    public void deactivateFilter() {
        Log.d(TAG, "ZIZI FILTER DEACTIVATE");
        if (!mAdapter.isFilteredByName()) {
            return;
        }

        Log.d(TAG, "ZIZI FILTER DEACTIVATE REALLY");
        setFilter(null);
    }

    public void setFilter(String filterText) {
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
                createSection();
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
        intent.setComponent(new ComponentName(appItem.getPackageName(), appItem.getComponentName()));
        getContext().startActivity(intent);
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
        if (!mAdapter.isFilteredByName()) {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mDataModel.setSectionCollapsed(
                            pageName,
                            sectionItem.getName(),
                            !sectionItem.isCollapsed());
                    return null;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    int position = mAdapter.getItemPosition(sectionItem);
                    if (position != RecyclerView.NO_POSITION) {
                        mRecyclerView.scrollToPosition(position);
                    }
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        String[] sections = new String[sectionNames.size()];
        sectionNames.toArray(sections);
        alertDialogBuilder.setTitle(R.string.dialog_move_to_section_title);
        alertDialogBuilder.setItems(sections, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                final String pageName = getPageName();
                final String sectionName = sectionNames.get(which);
                Task.callInBackground(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        AppData appData = new AppData(appItem);
                        mDataModel.moveAppToSection(pageName, sectionName, appData);
                        return null;
                    }
                });
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
        final String oldSectionName = sectionItem.getName();
        final String pageName = getPageName();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, oldSectionName,
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

    private void createSection() {
        final String pageName = getPageName();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, "",
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String sectionName) {
                        if (!sectionName.isEmpty()) {
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    mDataModel.addNewSection(pageName, sectionName, true);
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
    }

    private void cancelChangingOrder() {
        Log.d(TAG, "ZIZI CANCEL CHANGNIG ORDER");
        if (!mAdapter.isChangingOrder()) {
            return;
        }

        mAdapter.setTypeFilter(null);
        mAdapter.setChangingOrder(false);
        getActivity().invalidateOptionsMenu();
    }

}
