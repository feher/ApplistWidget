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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applistwidget.model.AppData;
import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.utils.RunnableWithArg;
import net.feheren_fekete.applistwidget.viewmodel.AppItem;
import net.feheren_fekete.applistwidget.viewmodel.SectionItem;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import de.greenrobot.event.EventBus;

public class ApplistFragment extends Fragment implements ApplistAdapter.ItemListener {

    private DataModel mDataModel;
    private RecyclerView mRecyclerView;
    private ApplistAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private boolean mIsChangingSectionOrder;

    public static ApplistFragment newInstance(String pageName, DataModel dataModel) {
        ApplistFragment fragment = new ApplistFragment();

        Bundle args = new Bundle();
        args.putString("pageName", pageName);
        fragment.setArguments(args);
        fragment.setDataModel(dataModel);

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
                this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.loadAllItems();

        ItemTouchHelper.Callback callback =
                new ApplistItemTouchHelperCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

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
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(mAdapter);
    }

    public void update() {
        mAdapter.loadAllItems();
    }

    public String getPageName() {
        return getArguments().getString("pageName");
    }

    public void setFilter(String filterText) {
        mAdapter.setFilter(filterText);
        mRecyclerView.scrollToPosition(0);
    }

    private static final int SECTION_ITEM_MENU_RENAME = 0;
    private static final int SECTION_ITEM_MENU_DELETE = 1;
    private static final int SECTION_ITEM_MENU_CREATE = 2;
    private static final int SECTION_ITEM_MENU_CHANGE_ORDER = 3;

    private static final int APP_ITEM_MENU_MOVE_TO_SECTION = 0;
    private static final int APP_ITEM_MENU_SHOW_INFO = 1;
    private static final int APP_ITEM_MENU_UNINSTALL = 2;

    @Override
    public void onAppLongTapped(final AppItem appItem) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(appItem.getName());
        alertDialogBuilder.setItems(R.array.app_item_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case APP_ITEM_MENU_MOVE_TO_SECTION:
                        moveAppToSection(appItem);
                        break;
                    case APP_ITEM_MENU_SHOW_INFO:
                        showAppInfo(appItem);
                        break;
                    case APP_ITEM_MENU_UNINSTALL:
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
        getActivity().finish();
    }

    @Override
    public void onSectionLongTapped(final SectionItem sectionItem) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(sectionItem.getName());
        alertDialogBuilder.setItems(R.array.section_item_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case SECTION_ITEM_MENU_RENAME:
                        renameSection(sectionItem);
                        break;
                    case SECTION_ITEM_MENU_DELETE:
                        deleteSection(sectionItem);
                        break;
                    case SECTION_ITEM_MENU_CREATE:
                        createSection();
                        break;
                    case SECTION_ITEM_MENU_CHANGE_ORDER:
                        changeSectionOrder();
                        break;
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onSectionTapped(final SectionItem sectionItem) {
        final String pageName = getPageName();
        if (mIsChangingSectionOrder) {
        } else {
            if (!mAdapter.isFiltered()) {
                Task.callInBackground(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mDataModel.setSectionCollapsed(
                                pageName,
                                sectionItem.getName(),
                                !sectionItem.isCollapsed());
                        return null;
                    }
                });
            }
        }
    }

    private void setDataModel(DataModel dataModel) {
        mDataModel = dataModel;
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
                        mDataModel.storePages();
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
        final String pageName = getPageName();
        mIsChangingSectionOrder = true;
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                List<String> sectionNames = mDataModel.getSectionNames(pageName);
                for (String sectionName : sectionNames) {
                    mDataModel.setSectionCollapsed(pageName, sectionName, true);
                }
                return null;
            }
        });
    }

}
