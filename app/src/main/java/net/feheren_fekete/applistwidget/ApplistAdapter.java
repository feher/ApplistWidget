package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.model.PageData;
import net.feheren_fekete.applistwidget.model.SectionData;
import net.feheren_fekete.applistwidget.viewmodel.AppItem;
import net.feheren_fekete.applistwidget.viewmodel.BaseItem;
import net.feheren_fekete.applistwidget.viewmodel.SectionItem;
import net.feheren_fekete.applistwidget.viewmodel.ViewModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class ApplistAdapter
        extends RecyclerView.Adapter<ApplistAdapter.ViewHolderBase>
        implements ApplistItemTouchHelperCallback.OnMoveListener {

    private static final String TAG = ApplistAdapter.class.getSimpleName();

    public static final int APP_ITEM_VIEW = 1;
    public static final int SECTION_ITEM_VIEW = 2;

    private Context mContext;
    private Handler mHandler;
    private PackageManager mPackageManager;
    private DataModel mModel;
    private String mPageName;
    private List<BaseItem> mItems;
    private @Nullable String mFilterText;
    private @Nullable List<BaseItem> mFilteredItems;
    private boolean mIsChangingOrder;
    private boolean mIsItemMoved;
    private ItemListener mItemListener;
    private ItemTouchHelper mItemTouchHelper;
    private IconCache mIconCache;
    private int[] mIconPlaceholderColors;
    private int mNextPlaceholderColor;

    public interface ItemListener {
        void onAppTapped(AppItem appItem);
        void onAppLongTapped(AppItem appItem);
        void onSectionTapped(SectionItem sectionItem);
        void onSectionLongTapped(SectionItem sectionItem);
    }

    public static class ViewHolderBase extends RecyclerView.ViewHolder {
        public ViewHolderBase(View view) {
            super(view);
        }
    }

    public static class AppItemHolder extends ViewHolderBase {
        public final LinearLayout layout;
        public final ImageView appIcon;
        public final TextView appName;
        public IconLoaderTask iconLoader;
        public AppItemHolder(View view) {
            super(view);
            this.layout = (LinearLayout) view.findViewById(R.id.layout);
            this.appIcon = (ImageView) view.findViewById(R.id.icon);
            this.appName = (TextView) view.findViewById(R.id.app_name);
        }
    }

    public static class SectionItemHolder extends ViewHolderBase {
        public final RelativeLayout layout;
        public final TextView sectionName;
        public SectionItemHolder(View view) {
            super(view);
            this.layout = (RelativeLayout) view.findViewById(R.id.layout);
            this.sectionName = (TextView) view.findViewById(R.id.app_name);
        }
    }

    public ApplistAdapter(Context context,
                          PackageManager packageManager,
                          DataModel model,
                          String pageName,
                          ItemListener itemListener,
                          IconCache iconCache) {
        mContext = context;
        mHandler = new Handler();
        mPackageManager = packageManager;
        mModel = model;
        mPageName = pageName;
        mItems = Collections.emptyList();
        mItemListener = itemListener;
        mIconCache = iconCache;
        mIconPlaceholderColors = mContext.getResources().getIntArray(R.array.icon_placeholders);
        mNextPlaceholderColor = 0;

        setHasStableIds(true);
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

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        mItemTouchHelper = itemTouchHelper;
    }

    public void setChangingOrder(boolean changing) {
        mIsChangingOrder = changing;
    }

    public boolean isChangingOrder() {
        return mIsChangingOrder;
    }

    public List<String> getSectionNames() {
        List<String> result = new ArrayList<>();
        for (BaseItem item : getItems()) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                result.add(sectionItem.getName());
            }
        }
        return result;
    }

    public Map<String, Boolean> getSectionCollapsedStates() {
        Map<String, Boolean> result = new HashMap<>();
        for (BaseItem item : getItems()) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                result.put(sectionItem.getName(), sectionItem.isCollapsed());
            }
        }
        return result;
    }

    public void setFilter(@Nullable String filterText) {
        mFilterText = filterText;
        mFilteredItems = filterItems();
        notifyDataSetChanged();
    }

    public boolean isFiltered() {
        return mFilterText != null;
    }

    public int getItemPosition(BaseItem item) {
        List<BaseItem> items = getItems();
        for (int i = 0; i < items.size(); ++i) {
            BaseItem it = items.get(i);
            if (it.getId() == item.getId()) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public long getItemId(int position) {
        return getItems().get(position).getId();
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        BaseItem item = getItems().get(position);
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
                PageData pageData = mModel.getPage(mPageName);
                if (pageData == null) {
                    pageData = new PageData(DataModel.INVALID_ID, mPageName, new ArrayList<SectionData>());
                }
                return ViewModelUtils.modelToView(pageData);
            }
        }).continueWith(new Continuation<List<BaseItem>, Void>() {
            @Override
            public Void then(Task<List<BaseItem>> task) throws Exception {
                List<BaseItem> items = task.getResult();
                if (items != null) {
                    mItems = items;
                    if (mFilterText != null) {
                        mFilteredItems = filterItems();
                    }
                    notifyDataSetChanged();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(getItems(), i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(getItems(), i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        mIsItemMoved = true;
    }

    @Override
    public void onItemMoveEnd() {
//        if (mIsItemMoved) {
//            mIsItemMoved = false;
//            final PageData pageData = ViewModelUtils.viewToModel(mPageName, getItems());
//            Task.callInBackground(new Callable<Void>() {
//                @Override
//                public Void call() throws Exception {
//                    mModel.setPage(pageData);
//                    return null;
//                }
//            });
//        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.SectionsChangedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    private List<BaseItem> getItems() {
        if (mFilterText != null) {
            return mFilteredItems;
        }
        return mItems;
    }

    private List<BaseItem> filterItems() {
        if (mFilterText == null) {
            return null;
        }
        if (mFilterText.isEmpty()) {
            return mItems;
        }

        List<BaseItem> result = new ArrayList<>();
        String lowercaseFilterText = mFilterText.toLowerCase();
        SectionItem currentSectionItem = null;
        for (BaseItem item : mItems) {
            if (item instanceof SectionItem) {
                currentSectionItem = (SectionItem) item;
            } else {
                String lowercaseItemName = item.getName().toLowerCase();
                if (lowercaseItemName.contains(lowercaseFilterText)) {
                    if (currentSectionItem != null) {
                        result.add(currentSectionItem);
                        currentSectionItem = null;
                    }
                    result.add(item);
                }
            }
        }
        return result;
    }

    private void bindAppItemHolder(AppItemHolder holder, int position) {
        final AppItem item = (AppItem) getItems().get(position);

        Bitmap icon = mIconCache.getIcon(mIconCache.createKey(item));
        if (icon == null) {
            if (holder.iconLoader != null) {
                if (!holder.iconLoader.isLoadingFor(item)) {
                    holder.iconLoader.cancel(true);
                    holder.iconLoader = null;
                }
            }
            if (holder.iconLoader == null) {
                holder.iconLoader = new IconLoaderTask(item, holder, mPackageManager, mIconCache);
                holder.iconLoader.execute();
                holder.appIcon.setImageBitmap(null);
                holder.appIcon.setBackgroundColor(mIconPlaceholderColors[mNextPlaceholderColor]);
                mNextPlaceholderColor = (mNextPlaceholderColor + 1) % mIconPlaceholderColors.length;
            }
        } else {
            if (holder.iconLoader != null) {
                holder.iconLoader.cancel(true);
                holder.iconLoader = null;
            }
            holder.appIcon.setBackgroundColor(Color.TRANSPARENT);
            holder.appIcon.setImageBitmap(icon);
        }

        holder.appName.setText(item.getName());

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onAppTapped(item);
            }
        });

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mItemListener.onAppLongTapped(item);
                return true;
            }
        });
    }

    private void bindSectionItemHolder(final SectionItemHolder holder, int position) {
        final SectionItem item = (SectionItem) getItems().get(position);
        holder.sectionName.setText(
                item.isCollapsed()
                        ? item.getName() + " ..."
                        : item.getName());

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mItemListener.onSectionLongTapped(item);
                return true;
            }
        });

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onSectionTapped(item);
            }
        });

        holder.layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN
                        && mIsChangingOrder) {
                    mItemTouchHelper.startDrag(holder);
                }
                return false;
            }
        });
    }

}
