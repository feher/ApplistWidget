package net.feheren_fekete.applistwidget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applistwidget.model.DataModel;

import de.greenrobot.event.EventBus;

public class ApplistFragment extends Fragment {

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
                getPageName());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.loadAllItems();

        ItemTouchHelper.Callback callback =
                new ApplistItemTouchHelperCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

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

}
