package net.feheren_fekete.applistwidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.model.PageData;
import net.feheren_fekete.applistwidget.viewmodel.AppItem;
import net.feheren_fekete.applistwidget.viewmodel.BaseItem;
import net.feheren_fekete.applistwidget.viewmodel.SectionItem;
import net.feheren_fekete.applistwidget.viewmodel.ViewModelUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class ApplistAdapter
        extends RecyclerView.Adapter<ApplistAdapter.ViewHolderBase>
        implements ApplistItemTouchHelperCallback.OnMoveListener {

    public static final int APP_ITEM_VIEW = 1;
    public static final int SECTION_ITEM_VIEW = 2;

    private Context mContext;
    private PackageManager mPackageManager;
    private DataModel mModel;
    private String mPageName;
    private List<BaseItem> mItems;
    private boolean mIsItemMoved;

    public static class ViewHolderBase extends RecyclerView.ViewHolder {
        public ViewHolderBase(View view) {
            super(view);
        }
    }

    public static class AppItemHolder extends ViewHolderBase {
        public final LinearLayout layout;
        public final ImageView appIcon;
        public final TextView appName;
        public AppItemHolder(View view) {
            super(view);
            this.layout = (LinearLayout) view.findViewById(R.id.layout);
            this.appIcon = (ImageView) view.findViewById(R.id.icon);
            this.appName = (TextView) view.findViewById(R.id.name);
        }
    }

    public static class SectionItemHolder extends ViewHolderBase {
        public final TextView sectionName;
        public SectionItemHolder(View view) {
            super(view);
            this.sectionName = (TextView) view.findViewById(R.id.name);
        }
    }

    public ApplistAdapter(Context context,
                          PackageManager packageManager,
                          DataModel model,
                          String pageName) {
        mContext = context;
        mPackageManager = packageManager;
        mModel = model;
        mPageName = pageName;
        mItems = Collections.emptyList();
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case APP_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.appitem, parent, false);
                return new AppItemHolder(itemView);
            }
            case SECTION_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sectionitem, parent, false);
                return new SectionItemHolder(itemView);
            }
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolderBase holder, int position) {
        if (holder instanceof AppItemHolder) {
            bindAppItemHolder((AppItemHolder) holder, position);
        } else if (holder instanceof SectionItemHolder) {
            bindSectionItemHolder((SectionItemHolder) holder, position);
        }
    }

    private void bindAppItemHolder(final AppItemHolder holder, final int position) {
        AppItem item = (AppItem) mItems.get(position);

        try {
            String packageName = item.getPackageName();
            Drawable appIcon = mPackageManager.getApplicationIcon(packageName);
            holder.appIcon.setImageDrawable(appIcon);

            holder.appName.setText(item.getAppName());

            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppItem item = (AppItem) mItems.get(position);
                    Intent intent = mPackageManager.getLaunchIntentForPackage(item.getPackageName());
                    mContext.startActivity(intent);
                    ((Activity) mContext).finish();
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            // TODO: set placeholder name and icon
        }
    }

    private void bindSectionItemHolder(SectionItemHolder holder, int position) {
        SectionItem item = (SectionItem) mItems.get(position);
        holder.sectionName.setText(item.getName());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        BaseItem item = mItems.get(position);
        if (item instanceof AppItem) {
            return APP_ITEM_VIEW;
        }
        if (item instanceof SectionItem) {
            return SECTION_ITEM_VIEW;
        }
        return super.getItemViewType(position);
    }

    public void loadAllItems() {
        Task.callInBackground(new Callable<List<BaseItem>>() {
            @Override
            public List<BaseItem> call() throws Exception {
                return ViewModelUtils.modelToView(mModel.getPage(mPageName));
            }
        }).continueWith(new Continuation<List<BaseItem>, Void>() {
            @Override
            public Void then(Task<List<BaseItem>> task) throws Exception {
                mItems = task.getResult();
                notifyDataSetChanged();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        mIsItemMoved = true;
    }

    @Override
    public void onItemMoveEnd() {
        if (mIsItemMoved) {
            mIsItemMoved = false;
            final PageData pageData = ViewModelUtils.viewToModel(mPageName, mItems);
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mModel.setPage(pageData);
                    return null;
                }
            });
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.SectionsChangedEvent event) {
        loadAllItems();
    }

}
