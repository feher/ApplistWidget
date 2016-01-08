package net.feheren_fekete.applistwidget;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


public class ApplistAdapter extends RecyclerView.Adapter<ApplistAdapter.ViewHolder>{

    private ApplistModel mModel;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    public ApplistAdapter(ApplistModel model) {
        mModel = model;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public void loadAllItems() {
    }
}
