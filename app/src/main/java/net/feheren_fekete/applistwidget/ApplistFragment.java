package net.feheren_fekete.applistwidget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ApplistFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private ApplistAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String mApplistName;

    public ApplistFragment(String applistName) {
        super();
        mApplistName = applistName;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ApplistAdapter(new ApplistModel(getContext().getPackageManager(), mApplistName));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.loadAllItems();

        return view;
    }
}
