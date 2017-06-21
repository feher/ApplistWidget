package net.feheren_fekete.applist.applistpage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.ApplistApp;
import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.applistpage.model.PageData;
import net.feheren_fekete.applist.applistpage.model.SectionData;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;
import net.feheren_fekete.applist.applistpage.viewmodel.ViewModelUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class ApplistAdapter
        extends RecyclerView.Adapter<ApplistAdapter.ViewHolderBase> {

    private static final String TAG = ApplistAdapter.class.getSimpleName();

    public static final int APP_ITEM_VIEW = 1;
    public static final int SECTION_ITEM_VIEW = 2;

    private Context mContext;
    private Fragment mFragment;
    private PackageManager mPackageManager;
    private FileUtils mFileUtils;
    private BadgeStore mBadgeStore;
    private List<BaseItem> mCollapsedItems;
    private List<BaseItem> mAllItems;
    private @Nullable String mFilterName;
    private @Nullable Class mFilterType;
    private @Nullable List<BaseItem> mFilteredItems;
    private ItemListener mItemListener;
    private IconCache mIconCache;
    private int[] mIconPlaceholderColors;
    private int mNextPlaceholderColor;
    private TypedValue mTypedValue = new TypedValue();

    public interface ItemListener {
        void onAppTapped(AppItem appItem);
        void onAppLongTapped(AppItem appItem);
        void onAppTouched(AppItem appItem);
        void onSectionTapped(SectionItem sectionItem);
        void onSectionLongTapped(SectionItem sectionItem);
        void onSectionTouched(SectionItem sectionItem);
    }

    public static class ViewHolderBase extends RecyclerView.ViewHolder {
        public final ViewGroup layout;
        public ViewHolderBase(View view, @IdRes int layoutId) {
            super(view);
            this.layout = (ViewGroup) view.findViewById(layoutId);
        }
    }

    public static class AppItemHolder extends ViewHolderBase {
        public final View draggedOverIndicatorLeft;
        public final View draggedOverIndicatorRight;
        public final ImageView appIcon;
        public final TextView appName;
        public final TextView badgeCount;
        public IconLoaderTask iconLoader;
        public AppItemHolder(View view) {
            super(view, R.id.applist_app_item_layout);
            this.draggedOverIndicatorLeft = view.findViewById(R.id.applist_app_item_dragged_over_indicator_left);
            this.draggedOverIndicatorRight = view.findViewById(R.id.applist_app_item_dragged_over_indicator_right);
            this.appIcon = (ImageView) view.findViewById(R.id.applist_app_item_icon);
            this.appName = (TextView) view.findViewById(R.id.applist_app_item_app_name);
            this.badgeCount = (TextView) view.findViewById(R.id.applist_app_item_badge_count);
        }
    }

    public static class SectionItemHolder extends ViewHolderBase {
        public final View draggedOverIndicatorLeft;
        public final View draggedOverIndicatorRight;
        public final TextView sectionName;
        public SectionItemHolder(View view) {
            super(view, R.id.applist_section_item_layout);
            this.draggedOverIndicatorLeft = view.findViewById(R.id.applist_section_item_dragged_over_indicator_left);
            this.draggedOverIndicatorRight = view.findViewById(R.id.applist_section_item_dragged_over_indicator_right);
            this.sectionName = (TextView) view.findViewById(R.id.applist_section_item_app_name);
        }
    }

    public ApplistAdapter(Context context,
                          Fragment fragment,
                          PackageManager packageManager,
                          FileUtils fileUtils,
                          BadgeStore badgeStore,
                          ItemListener itemListener,
                          IconCache iconCache) {
        mContext = context;
        mFragment = fragment;
        mPackageManager = packageManager;
        mFileUtils = fileUtils;
        mBadgeStore = badgeStore;
        mCollapsedItems = Collections.emptyList();
        mAllItems = Collections.emptyList();
        mItemListener = itemListener;
        mIconCache = iconCache;
        mIconPlaceholderColors = ((ApplistApp)context.getApplicationContext()).getIconPlaceholderColors();
        mNextPlaceholderColor = 0;

        setHasStableIds(true);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case APP_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.applist_app_item, parent, false);
                return new AppItemHolder(itemView);
            }
            case SECTION_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.applist_section_item, parent, false);
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

    public String getUncategorizedSectionName() {
        String result = "";
        for (BaseItem item : getItems()) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                if (!sectionItem.isRemovable()) {
                    result = sectionItem.getName();
                }
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

    public int getNextSectionPosition(BaseItem item) {
        final int itemPosition = getItemPosition(item);
        if (itemPosition != RecyclerView.NO_POSITION) {
            List<BaseItem> items = getItems();
            for (int i = itemPosition + 1; i < items.size(); ++i) {
                BaseItem it = items.get(i);
                if (it instanceof SectionItem) {
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Nullable
    public BaseItem getNextSection(BaseItem item) {
        final int itemPosition = getItemPosition(item);
        if (itemPosition != RecyclerView.NO_POSITION) {
            List<BaseItem> items = getItems();
            for (int i = itemPosition + 1; i < items.size(); ++i) {
                BaseItem it = items.get(i);
                if (it instanceof SectionItem) {
                    return it;
                }
            }
        }
        return null;
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

    public void setItems(List<BaseItem> items) {
        mAllItems = items;
        updateCollapsedAndFilteredItems();
        notifyDataSetChanged();
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return false;
        }
        List<BaseItem> items = getItems();
        if (fromPosition < 0 || fromPosition >= items.size()) {
            ApplistLog.getInstance().log(new RuntimeException("Bad fromPosition " + fromPosition));
            return false;
        }
        if (toPosition < 0 || toPosition >= items.size()) {
            ApplistLog.getInstance().log(new RuntimeException("Bad toPosition " + toPosition));
            return false;
        }

        final int realFromPosition = getRealItemPosition(items.get(fromPosition));
        final int realToPosition = getRealItemPosition(items.get(toPosition));

        BaseItem movedItem = mAllItems.get(realFromPosition);
        if (movedItem instanceof AppItem && realToPosition == 0) {
            // Cannot move app above the first section header.
            return false;
        }

        if (movedItem instanceof AppItem) {
            if (realFromPosition < realToPosition) {
                for (int i = realFromPosition; i < realToPosition; i++) {
                    Collections.swap(mAllItems, i, i + 1);
                }
            } else {
                for (int i = realFromPosition; i > realToPosition; i--) {
                    Collections.swap(mAllItems, i, i - 1);
                }
            }
        } else if (movedItem instanceof SectionItem) {
            List<BaseItem> sectionAndApps = new ArrayList<>();
            for (BaseItem item : mAllItems) {
                if (item instanceof SectionItem
                        && item.getId() == movedItem.getId()) {
                    sectionAndApps.add(item);
                } else if (item instanceof AppItem
                        && !sectionAndApps.isEmpty()) {
                    sectionAndApps.add(item);
                } else if (item instanceof SectionItem
                        && !sectionAndApps.isEmpty()
                        && item.getId() != movedItem.getId()) {
                    break;
                }
            }
            mAllItems.removeAll(sectionAndApps);

            int adjustedToPosition = realToPosition;
            if (realFromPosition < realToPosition) {
                adjustedToPosition = realToPosition - sectionAndApps.size() + 1;
            }
            mAllItems.addAll(adjustedToPosition, sectionAndApps);
        }

        updateCollapsedAndFilteredItems();
        notifyItemMoved(fromPosition, toPosition);

        return true;
    }

    public BaseItem getItem(int position) {
        return getItems().get(position);
    }

    public List<BaseItem> getAllItems() {
        return new ArrayList<>(mAllItems);
    }

    public boolean isAppLastInSection(AppItem item) {
        boolean result = false;
        final int position = getRealItemPosition(item);
        if (position == mAllItems.size() - 1) {
            result = true;
        } else if (mAllItems.get(position + 1) instanceof SectionItem) {
            result = true;
        }
        return result;
    }

    public boolean isSectionEmpty(SectionItem item) {
        boolean result = false;
        final int position = getRealItemPosition(item);
        if (position == mAllItems.size() - 1) {
            result = true;
        } else if (mAllItems.get(position + 1) instanceof SectionItem) {
            result = true;
        }
        return result;
    }

    public boolean isSectionLast(SectionItem item) {
        boolean result = true;
        final int position = getRealItemPosition(item);
        for (int i = position + 1; i < mAllItems.size(); ++i) {
            if (mAllItems.get(i) instanceof SectionItem) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void setEnabled(BaseItem item, boolean enabled) {
        item.setEnabled(enabled);
        notifyItemChanged(getRealItemPosition(item));
    }

    public void setAllAppsEnabled(boolean enabled) {
        for (BaseItem baseItem : mAllItems) {
            if (baseItem instanceof AppItem) {
                baseItem.setEnabled(enabled);
            }
        }
        notifyDataSetChanged();
    }

    public void setSectionsHighlighted(boolean highlighted) {
        for (BaseItem baseItem : mAllItems) {
            if (baseItem instanceof SectionItem) {
                baseItem.setHighlighted(highlighted);
            }
        }
        notifyDataSetChanged();
    }

    private List<BaseItem> getItems() {
        if (mFilteredItems != null) {
            return mFilteredItems;
        }
        return mCollapsedItems;
    }

    private int getRealItemPosition(BaseItem item) {
        for (int i = 0; i < mAllItems.size(); ++i) {
            if (mAllItems.get(i).getId() == item.getId()) {
                return i;
            }
        }
        return -1;
    }

    private void updateCollapsedAndFilteredItems() {
        mCollapsedItems = getCollapsedItems();
        if (mFilterName != null) {
            mFilteredItems = filterItemsByName();
        }
        if (mFilterType != null) {
            mFilteredItems = filterItemsByType();
        }
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
            List<BaseItem> result = new ArrayList<>();
            for (BaseItem item : mAllItems) {
                if (item instanceof AppItem) {
                    result.add(item);
                }
            }
            return result;
        } else {
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
        if (mFragment.getActivity() == null) {
            return;
        }

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
                        mIconCache, mFileUtils.getIconCacheDirPath(mContext));
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
        if (SettingsUtils.isThemeTransparent(mContext)) {
            holder.appName.setShadowLayer(12, 2, 2, android.R.color.black);
        } else {
            holder.appName.setShadowLayer(0, 0, 0, 0);
        }
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

        final float alpha = item.isEnabled() ? 1.0f : 0.3f;
        holder.appIcon.setAlpha(alpha);
        holder.appName.setAlpha(alpha);
        holder.badgeCount.setAlpha(alpha);

        holder.draggedOverIndicatorLeft.setVisibility(
                item.isDraggedOverLeft() ? View.VISIBLE : View.INVISIBLE);
        holder.draggedOverIndicatorRight.setVisibility(
                item.isDraggedOverRight() ? View.VISIBLE : View.INVISIBLE);

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

        holder.layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mItemListener.onAppTouched(item);
                }
                return false;
            }
        });
    }

    private void bindSectionItemHolder(final SectionItemHolder holder, int position) {
        final SectionItem item = (SectionItem) getItems().get(position);
        holder.sectionName.setText(
                item.isCollapsed()
                        ? item.getName() + " ..."
                        : item.getName());

        Resources.Theme theme = mContext.getTheme();
        if (item.isHighlighted()) {
            theme.resolveAttribute(R.attr.sectionTextHighlightColor, mTypedValue, true);
        } else {
            theme.resolveAttribute(R.attr.sectionTextColor, mTypedValue, true);
        }
        holder.sectionName.setTextColor(mTypedValue.data);

        final float alpha = item.isEnabled() ? 1.0f : 0.3f;
        holder.sectionName.setAlpha(alpha);

        holder.draggedOverIndicatorLeft.setVisibility(
                item.isDraggedOverLeft() ? View.VISIBLE : View.INVISIBLE);
        holder.draggedOverIndicatorRight.setVisibility(
                item.isDraggedOverRight() ? View.VISIBLE : View.INVISIBLE);

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
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mItemListener.onSectionTouched(item);
                }
                return false;
            }
        });
    }

}
