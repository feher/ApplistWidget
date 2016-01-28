package net.feheren_fekete.applistwidget;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

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
    private ItemListener mItemListener;
    private IconCache mIconCache;
    private ConcurrentHashMap<String, IconLoaderTask> mIconLoaders;

    public interface ItemListener {
        void onAppLongTapped(int position);
        void onAppTapped(int position);
        void onSectionLongTapped(int position);
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
            this.appName = (TextView) view.findViewById(R.id.name);
        }
    }

    public static class SectionItemHolder extends ViewHolderBase {
        public final LinearLayout layout;
        public final TextView sectionName;
        public SectionItemHolder(View view) {
            super(view);
            this.layout = (LinearLayout) view.findViewById(R.id.layout);
            this.sectionName = (TextView) view.findViewById(R.id.name);
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
        mIconLoaders = new ConcurrentHashMap<>();
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

        String key = mIconCache.createKey(item);
        Bitmap icon = mIconCache.getIcon(key);
        if (icon == null) {
            IconLoaderTask iconLoaderTask = new IconLoaderTask(
                    item, holder.appIcon, mPackageManager, mIconCache, mIconLoaders);
            mIconLoaders.put(key, iconLoaderTask);
            iconLoaderTask.execute();
        } else {
            holder.appIcon.setImageBitmap(icon);
        }

        holder.appName.setText(item.getAppName());

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onAppTapped(position);
            }
        });

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mItemListener.onAppLongTapped(position);
                return true;
            }
        });
    }

    private void bindSectionItemHolder(SectionItemHolder holder, final int position) {
        SectionItem item = (SectionItem) mItems.get(position);
        holder.sectionName.setText(item.getName());

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mItemListener.onSectionLongTapped(position);
                return true;
            }
        });
    }

    public BaseItem getItem(int position) {
        return mItems.get(position);
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
                List<BaseItem> items = task.getResult();
                if (items != null) {
                    mItems = items;
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
        private WeakReference<ImageView> iconViewRef;
        private PackageManager packageManager;
        private WeakReference<IconCache> iconCacheRef;
        private WeakReference<ConcurrentHashMap<String, IconLoaderTask>> iconLoadersRef;
        public IconLoaderTask(AppItem appItem,
                              ImageView iconView,
                              PackageManager packageManager,
                              IconCache iconCache,
                              ConcurrentHashMap<String, IconLoaderTask> iconLoaders) {
            this.appItem = appItem;
            this.iconViewRef = new WeakReference<>(iconView);
            this.packageManager = packageManager;
            this.iconCacheRef = new WeakReference<>(iconCache);
            this.iconLoadersRef = new WeakReference<>(iconLoaders);
        }
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                Drawable iconDrawable = packageManager.getActivityIcon(
                        new ComponentName(appItem.getPackageName(), appItem.getComponentName()));
                return drawableToBitmap(iconDrawable);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap iconBitmap) {
            ImageView iconView = iconViewRef.get();
            IconCache iconCache = iconCacheRef.get();
            ConcurrentHashMap<String, IconLoaderTask> iconLoaders = iconLoadersRef.get();
            if (iconBitmap != null
                    && iconView != null
                    && iconCache != null
                    && iconLoaders != null) {
                String key = iconCache.createKey(appItem);
                IconLoaderTask iconLoader = iconLoaders.get(key);
                if (iconLoader == this) {
                    iconCache.addIcon(key, iconBitmap);
                    iconView.setImageBitmap(iconBitmap);
                    iconLoaders.remove(key);
                }
            }

        }
    }

}
