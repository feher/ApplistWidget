package net.feheren_fekete.applistwidget;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applistwidget.model.AppData;
import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.viewmodel.AppItem;

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

    public static ApplistFragment newInstance(String pageName) {
        ApplistFragment fragment = new ApplistFragment();

        Bundle args = new Bundle();
        args.putString("pageName", pageName);
        fragment.setArguments(args);

        return fragment;
    }

    public void setDataModel(DataModel dataModel) {
        mDataModel = dataModel;
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

        return view;
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

    public String getPageName() {
        return getArguments().getString("pageName");
    }

    private static final int APP_ITEM_MENU_MOVE_TO_SECTION = 0;
    private static final int APP_ITEM_MENU_SHOW_INFO = 1;
    private static final int APP_ITEM_MENU_UNINSTALL = 2;

    @Override
    public void onAppLongTapped(int position) {
        final AppItem appItem = (AppItem) mAdapter.getItem(position);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(appItem.getAppName());
        alertDialogBuilder.setItems(R.array.app_item_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case APP_ITEM_MENU_MOVE_TO_SECTION:
                        moveAppToSection(appItem);
                        break;
                    case APP_ITEM_MENU_SHOW_INFO:
                        break;
                    case APP_ITEM_MENU_UNINSTALL:
                        break;
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
                        mDataModel.storeData(null);
                        return null;
                    }
                });
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onAppTapped(int position) {
        AppItem appItem = (AppItem) mAdapter.getItem(position);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName(appItem.getPackageName(), appItem.getComponentName()));

        getContext().startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onSectionLongTapped(int position) {

    }
}
