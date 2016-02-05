package net.feheren_fekete.applistwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
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

import java.lang.ref.WeakReference;
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
                          ItemListener itemListener) {
        mContext = context;
        mPackageManager = packageManager;
        mModel = model;
        mPageName = pageName;
        mItems = Collections.emptyList();
        mItemListener = itemListener;
        mIconCache = new IconCache();
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

    public void setFilter(@Nullable String filterText) {
        mFilterText = filterText;
        mFilteredItems = filterItems();
        notifyDataSetChanged();
    }

    public boolean isFiltered() {
        return mFilterText != null;
    }

//    public void collapseSection(SectionItem sectionItem) {
//        int position = -1;
//        for (int i = 0; i < mItems.size(); ++i) {
//            BaseItem item = mItems.get(i);
//            if (item instanceof SectionItem) {
//                SectionItem s = (SectionItem) item;
//                if (s.getName().equals(sectionItem.getName())) {
//                    position = i;
//                    break;
//                }
//            }
//        }
//        if (position == -1) {
//            return;
//        }
//        for (int i = position + 1; i < mItems.size(); ) {
//            if (mItems.get(i) instanceof AppItem) {
//                mItems.remove(i);
//                notifyItemRemoved(i);
//            } else {
//                break;
//            }
//        }
//    }

    public BaseItem getItem(int position) {
        return getItems().get(position);
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
                    if (mFilterText == null) {
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
        loadAllItems();
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

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static final class IconCache {
        private LruCache<String, Bitmap> mCache;

        public IconCache() {
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;

            mCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }

        public void addIcon(String key, Bitmap bitmap) {
            if (getIcon(key) == null) {
                mCache.put(key, bitmap);
            }
        }

        public Bitmap getIcon(String key) {
            return mCache.get(key);
        }

        public String createKey(AppItem appItem) {
            return appItem.getPackageName() + "::" + appItem.getComponentName();
        }
    }

    private static final class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private AppItem appItem;
        private WeakReference<AppItemHolder> appItemHolderRef;
        private PackageManager packageManager;
        private WeakReference<IconCache> iconCacheRef;

        public IconLoaderTask(AppItem appItem,
                              AppItemHolder appItemHolder,
                              PackageManager packageManager,
                              IconCache iconCache) {
            this.appItem = appItem;
            this.appItemHolderRef = new WeakReference<>(appItemHolder);
            this.packageManager = packageManager;
            this.iconCacheRef = new WeakReference<>(iconCache);
        }

        public boolean isLoadingFor(AppItem item) {
            return appItem.equals(item);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                Drawable iconDrawable = null;
                if (!isCancelled()) {
                    iconDrawable = packageManager.getActivityIcon(
                            new ComponentName(appItem.getPackageName(), appItem.getComponentName()));
                } else {
                    return null;
                }
                if (!isCancelled() && iconDrawable != null) {
                    return drawableToBitmap(iconDrawable);
                } else {
                    return null;
                }
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap iconBitmap) {
            AppItemHolder appItemHolder = appItemHolderRef.get();
            IconCache iconCache = iconCacheRef.get();
            if (iconBitmap != null
                    && appItemHolder != null
                    && iconCache != null
                    && appItemHolder.iconLoader == this
                    && !isCancelled()) {
                String key = iconCache.createKey(appItem);
                iconCache.addIcon(key, iconBitmap);
                appItemHolder.appIcon.setBackgroundColor(Color.TRANSPARENT);
                appItemHolder.appIcon.setImageBitmap(iconBitmap);
                appItemHolder.iconLoader = null;
            }

        }
    }

}
