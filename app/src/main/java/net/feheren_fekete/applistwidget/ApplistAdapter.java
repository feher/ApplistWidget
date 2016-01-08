package net.feheren_fekete.applistwidget;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class ApplistAdapter extends RecyclerView.Adapter<ApplistAdapter.ViewHolder>{

    private ApplistModel mModel;
    private List<String> mItems;
    private PackageManager mPackageManager;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView appIcon;
        public final TextView appName;
        public ViewHolder(View view) {
            super(view);
            this.appIcon = (ImageView) view.findViewById(R.id.icon);
            this.appName = (TextView) view.findViewById(R.id.name);
        }
    }

    public ApplistAdapter(ApplistModel model, PackageManager packageManager) {
        mModel = model;
        mItems = Collections.emptyList();
        mPackageManager = packageManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.appitem, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String packageName = mItems.get(position);

        try {
            Drawable appIcon = mPackageManager.getApplicationIcon(packageName);
            holder.appIcon.setImageDrawable(appIcon);

            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            holder.appName.setText(applicationInfo.name);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO: set placeholder name and icon
        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public void loadAllItems() {
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return mModel.loadAllData();
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                mItems = task.getResult();
                notifyDataSetChanged();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }
}
