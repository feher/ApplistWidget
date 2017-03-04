package net.feheren_fekete.applist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.model.PageData;
import net.feheren_fekete.applist.model.SectionData;
import net.feheren_fekete.applist.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.viewmodel.AppItem;
import net.feheren_fekete.applist.viewmodel.BaseItem;
import net.feheren_fekete.applist.viewmodel.SectionItem;
import net.feheren_fekete.applist.viewmodel.ViewModelUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private Fragment mFragment;
    private Handler mHandler;
    private PackageManager mPackageManager;
    private DataModel mModel;
    private BadgeStore mBadgeStore;
    private String mPageName;
    private List<BaseItem> mCollapsedItems;
    private List<BaseItem> mAllItems;
    private @Nullable String mFilterName;
    private @Nullable Class mFilterType;
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
        public final ConstraintLayout layout;
        public final ImageView appIcon;
        public final TextView appName;
        public final TextView badgeCount;
        public IconLoaderTask iconLoader;
        public AppItemHolder(View view) {
            super(view);
            this.layout = (ConstraintLayout) view.findViewById(R.id.layout);
            this.appIcon = (ImageView) view.findViewById(R.id.icon);
            this.appName = (TextView) view.findViewById(R.id.app_name);
            this.badgeCount = (TextView) view.findViewById(R.id.badge_count);
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
                          Fragment fragment,
                          PackageManager packageManager,
                          DataModel model,
                          BadgeStore badgeStore,
                          String pageName,
                          ItemListener itemListener,
                          IconCache iconCache) {
        mContext = context;
        mFragment = fragment;
        mHandler = new Handler();
        mPackageManager = packageManager;
        mModel = model;
        mBadgeStore = badgeStore;
        mPageName = pageName;
        mCollapsedItems = Collections.emptyList();
        mAllItems = Collections.emptyList();
        mItemListener = itemListener;
        mIconCache = iconCache;
        mIconPlaceholderColors = ((ApplistApp)context.getApplicationContext()).getThemeColors();
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

    public void setNameFilter(@Nullable String filterText) {
        mFilterName = filterText;
        mFilteredItems = filterItemsByName();
        notifyDataSetChanged();
    }

    public void setTypeFilter(@Nullable Class filterType) {
        mFilterType = filterType;
        mFilteredItems = filterItemsByType();
        notifyDataSetChanged();
    }

    public boolean isFilteredByName() {
        return mFilterName != null;
    }

    public boolean isFilteredByType() {
        return mFilterType != null;
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
                    mAllItems = items;
                    mCollapsedItems = getCollapsedItems();
                    if (mFilterName != null) {
                        mFilteredItems = filterItemsByName();
                    }
                    if (mFilterType != null) {
                        mFilteredItems = filterItemsByType();
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSectionsChangedEvent(DataModel.SectionsChangedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBadgeEvent(BadgeStore.BadgeEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    private List<BaseItem> getItems() {
        if (mFilteredItems != null) {
            return mFilteredItems;
        }
        return mCollapsedItems;
    }

    private List<BaseItem> getCollapsedItems() {
        List<BaseItem> result = new ArrayList<>();
        SectionItem currentSection = null;
        for (BaseItem item : mAllItems) {
            if (item instanceof SectionItem) {
                result.add(item);
                currentSection = (SectionItem) item;
            } else if (!currentSection.isCollapsed()) {
                result.add(item);
            }
        }
        return result;
    }

    private List<BaseItem> filterItemsByName() {
        if (mFilterName == null) {
            return null;
        }
        if (mFilterName.isEmpty()) {
            return mCollapsedItems;
        }

        List<BaseItem> result = new ArrayList<>();
        String lowercaseFilterText = mFilterName.toLowerCase();
        for (BaseItem item : mAllItems) {
            if (item instanceof AppItem) {
                String lowercaseItemName = item.getName().toLowerCase();
                if (lowercaseItemName.contains(lowercaseFilterText)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<BaseItem> filterItemsByType() {
        if (mFilterType == null) {
            return null;
        }
        List<BaseItem> result = new ArrayList<>();
        for (BaseItem item : mAllItems) {
            if (mFilterType.isInstance(item)) {
                result.add(item);
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
                holder.iconLoader = new IconLoaderTask(
                        item, holder, mPackageManager,
                        mIconCache, FileUtils.getIconCacheDirPath(mContext));
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
        if (SettingsUtils.getShowBadge(mFragment.getActivity())) {
            int badgeCount = mBadgeStore.getBadgeCount(item.getPackageName(), item.getComponentName());
            if (badgeCount > 0) {
                holder.badgeCount.setVisibility(View.VISIBLE);
                if (badgeCount != BadgeUtils.NOT_NUMBERED_BADGE_COUNT) {
                    holder.badgeCount.setText(String.valueOf(badgeCount));
                } else {
                    holder.badgeCount.setText("\u2022");
                }
            } else {
                holder.badgeCount.setVisibility(View.GONE);
            }
        } else {
            holder.badgeCount.setVisibility(View.GONE);
        }

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
