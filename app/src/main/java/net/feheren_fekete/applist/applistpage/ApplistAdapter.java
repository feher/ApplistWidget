package net.feheren_fekete.applist.applistpage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.applistpage.viewmodel.AppShortcutItem;
import net.feheren_fekete.applist.applistpage.viewmodel.ShortcutItem;
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem;
import net.feheren_fekete.applist.launcher.GlideApp;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ApplistAdapter
        extends RecyclerView.Adapter<ApplistAdapter.ViewHolderBase> {

    private static final String TAG = ApplistAdapter.class.getSimpleName();

    public static final int STARTABLE_ITEM_VIEW = 1;
    public static final int SECTION_ITEM_VIEW = 2;

    // TODO: Inject
    private BadgeStore mBadgeStore = BadgeStore.getInstance();
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();

    private Context mContext;
    private Fragment mFragment;
    private PackageManager mPackageManager;
    private FileUtils mFileUtils;
    private List<BaseItem> mCollapsedItems;
    private List<BaseItem> mAllItems;
    private @Nullable String mFilterName;
    private @Nullable List<BaseItem> mFilteredItems;
    private ItemListener mItemListener;
    private IconPackHelper mIconPackHelper;
    private IconCache mIconCache;
    private int[] mIconPlaceholderColors;
    private int mNextPlaceholderColor;
    private TypedValue mTypedValue = new TypedValue();

    public interface ItemListener {
        void onStartableTapped(StartableItem startableItem);
        void onStartableLongTapped(StartableItem startableItem);
        void onStartableTouched(StartableItem startableItem);
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

    public static class StartableItemHolder extends ViewHolderBase {
        public final View draggedOverIndicatorLeft;
        public final View draggedOverIndicatorRight;
        public final ImageView appIcon;
        public final TextView appNameWithoutShadow;
        public final TextView appNameWithShadow;
        public TextView appName;
        public final TextView badgeCount;
        public final ImageView shortcutIndicator;
        public IconLoaderTask iconLoader;
        public StartableItem item;
        private WeakReference<ItemListener> itemListenerRef;
        public StartableItemHolder(View view, ItemListener itemListener) {
            super(view, R.id.applist_app_item_layout);
            this.draggedOverIndicatorLeft = view.findViewById(R.id.applist_app_item_dragged_over_indicator_left);
            this.draggedOverIndicatorRight = view.findViewById(R.id.applist_app_item_dragged_over_indicator_right);
            this.appIcon = view.findViewById(R.id.applist_app_item_icon);
            this.appNameWithoutShadow = view.findViewById(R.id.applist_app_item_app_name);
            this.appNameWithShadow = view.findViewById(R.id.applist_app_item_app_name_with_shadow);
            this.badgeCount = view.findViewById(R.id.applist_app_item_badge_count);
            this.shortcutIndicator = view.findViewById(R.id.applist_app_item_shortcut_indicator);
            this.itemListenerRef = new WeakReference<>(itemListener);
            this.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ItemListener itemListener = itemListenerRef.get();
                    if (itemListener != null) {
                        itemListener.onStartableTapped(item);
                    }
                }
            });
            this.layout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ItemListener itemListener = itemListenerRef.get();
                    if (itemListener != null) {
                        itemListener.onStartableLongTapped(item);
                    }
                    return true;
                }
            });
            this.layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        ItemListener itemListener = itemListenerRef.get();
                        if (itemListener != null) {
                            itemListener.onStartableTouched(item);
                        }
                    }
                    return false;
                }
            });
        }
    }

    public static class SectionItemHolder extends ViewHolderBase {
        public final View draggedOverIndicatorLeft;
        public final View draggedOverIndicatorRight;
        public final TextView sectionName;
        public SectionItem item;
        private WeakReference<ItemListener> itemListenerRef;
        public SectionItemHolder(View view, ItemListener itemListener) {
            super(view, R.id.applist_section_item_layout);
            this.draggedOverIndicatorLeft = view.findViewById(R.id.applist_section_item_dragged_over_indicator_left);
            this.draggedOverIndicatorRight = view.findViewById(R.id.applist_section_item_dragged_over_indicator_right);
            this.sectionName = (TextView) view.findViewById(R.id.applist_section_item_section_name);
            this.itemListenerRef = new WeakReference<>(itemListener);
            this.layout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ItemListener itemListener = itemListenerRef.get();
                    if (itemListener != null) {
                        itemListener.onSectionLongTapped(item);
                    }
                    return true;
                }
            });
            this.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ItemListener itemListener = itemListenerRef.get();
                    if (itemListener != null) {
                        itemListener.onSectionTapped(item);
                    }
                }
            });
            this.layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        ItemListener itemListener = itemListenerRef.get();
                        if (itemListener != null) {
                            itemListener.onSectionTouched(item);
                        }
                    }
                    return false;
                }
            });
        }
    }

    public ApplistAdapter(Context context,
                          Fragment fragment,
                          PackageManager packageManager,
                          FileUtils fileUtils,
                          ItemListener itemListener,
                          IconPackHelper iconPackHelper,
                          IconCache iconCache) {
        mContext = context;
        mFragment = fragment;
        mPackageManager = packageManager;
        mFileUtils = fileUtils;
        mCollapsedItems = Collections.emptyList();
        mAllItems = Collections.emptyList();
        mItemListener = itemListener;
        mIconPackHelper = iconPackHelper;
        mIconCache = iconCache;
        mIconPlaceholderColors = mSettingsUtils.isThemeTransparent()
                ? mContext.getResources().getIntArray(R.array.icon_placeholder_colors_dark)
                : mContext.getResources().getIntArray(R.array.icon_placeholder_colors_light);
        mNextPlaceholderColor = 0;

        setHasStableIds(true);
    }

    @Override
    @NonNull
    public ViewHolderBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case STARTABLE_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.applist_startable_item, parent, false);
                return new StartableItemHolder(itemView, mItemListener);
            }
            case SECTION_ITEM_VIEW: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.applist_section_item, parent, false);
                return new SectionItemHolder(itemView, mItemListener);
            }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderBase holder, int position) {
        if (holder instanceof StartableItemHolder) {
            bindStartableItemHolder((StartableItemHolder) holder, position);
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

    public boolean isFilteredByName() {
        return mFilterName != null;
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
        List<BaseItem> items = getItems();
        if (position < items.size()) {
            BaseItem item = items.get(position);
            if (item instanceof StartableItem) {
                return STARTABLE_ITEM_VIEW;
            }
            if (item instanceof SectionItem) {
                return SECTION_ITEM_VIEW;
            }
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
        if (movedItem instanceof StartableItem && realToPosition == 0) {
            // Cannot move app above the first section header.
            return false;
        }

        if (movedItem instanceof StartableItem) {
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
            List<BaseItem> sectionAndStartables = new ArrayList<>();
            for (BaseItem item : mAllItems) {
                if (item instanceof SectionItem
                        && item.getId() == movedItem.getId()) {
                    sectionAndStartables.add(item);
                } else if (item instanceof StartableItem
                        && !sectionAndStartables.isEmpty()) {
                    sectionAndStartables.add(item);
                } else if (item instanceof SectionItem
                        && !sectionAndStartables.isEmpty()
                        && item.getId() != movedItem.getId()) {
                    break;
                }
            }
            mAllItems.removeAll(sectionAndStartables);

            int adjustedToPosition = realToPosition;
            if (realFromPosition < realToPosition) {
                adjustedToPosition = realToPosition - sectionAndStartables.size() + 1;
            }
            mAllItems.addAll(adjustedToPosition, sectionAndStartables);
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

    public boolean isStartableLastInSection(StartableItem item) {
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

    public void setEnabled(BaseItem item, boolean enabled) {
        item.setEnabled(enabled);
        notifyItemChanged(getRealItemPosition(item));
    }

    public void setHighlighted(BaseItem item, boolean highlighted) {
        item.setHighlighted(highlighted);
        notifyItemChanged(getRealItemPosition(item));
    }

    public void setAllStartablesEnabled(boolean enabled) {
        for (BaseItem baseItem : mAllItems) {
            if (baseItem instanceof StartableItem) {
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
                if (item instanceof StartableItem) {
                    result.add(item);
                }
            }
            return result;
        } else {
            List<BaseItem> result = new ArrayList<>();
            String lowercaseFilterText = mFilterName.toLowerCase();
            for (BaseItem item : mAllItems) {
                if (item instanceof StartableItem) {
                    String lowercaseItemName = item.getName().toLowerCase();
                    if (lowercaseItemName.contains(lowercaseFilterText)) {
                        result.add(item);
                    }
                }
            }
            return result;
        }
    }

    private void bindStartableItemHolder(StartableItemHolder holder, int position) {
        if (mFragment.getActivity() == null) {
            return;
        }

        final StartableItem item = (StartableItem) getItems().get(position);
        holder.item = item;

        // REF: 2017_06_22_22_08_setShadowLayer_not_working
        if (mSettingsUtils.isThemeTransparent()) {
            holder.appNameWithShadow.setVisibility(View.VISIBLE);
            holder.appNameWithoutShadow.setVisibility(View.INVISIBLE);
            holder.appName = holder.appNameWithShadow;
        } else {
            holder.appNameWithoutShadow.setVisibility(View.VISIBLE);
            holder.appNameWithShadow.setVisibility(View.INVISIBLE);
            holder.appName = holder.appNameWithoutShadow;
        }

        holder.appName.setText(item.getName());

        final float alpha = item.isEnabled() ? 1.0f : 0.3f;
        holder.appIcon.setAlpha(alpha);
        holder.appName.setAlpha(alpha);
        holder.badgeCount.setAlpha(alpha);

        if (item.isHighlighted()) {
            holder.layout.setBackgroundResource(R.drawable.applist_startable_item_highlighted_background);
        } else {
            holder.layout.setBackground(null);
        }

        holder.draggedOverIndicatorLeft.setVisibility(
                item.isDraggedOverLeft() ? View.VISIBLE : View.INVISIBLE);
        holder.draggedOverIndicatorRight.setVisibility(
                item.isDraggedOverRight() ? View.VISIBLE : View.INVISIBLE);

        if (item instanceof AppItem) {
            bindAppItemHolder(holder, (AppItem) item);
        } else if ((item instanceof ShortcutItem) || (item instanceof AppShortcutItem)) {
            bindShortcutItemHolder(holder, item);
        }
    }

    private void bindAppItemHolder(StartableItemHolder holder, AppItem item) {
        // Cancel loading of ShortcutItem icons into this holder.
        GlideApp.with(mContext).clear(holder.appIcon);

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
                        mIconPackHelper,
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

        if (mSettingsUtils.getShowBadge()) {
            int badgeCount = mBadgeStore.getBadgeCount(item.getPackageName(), item.getClassName());
            if (badgeCount > 0) {
                holder.badgeCount.setVisibility(View.VISIBLE);
                if (badgeCount != BadgeUtils.NOT_NUMBERED_BADGE_COUNT) {
                    holder.badgeCount.setText(String.valueOf(badgeCount));
                } else {
                    holder.badgeCount.setText(null);
                }
            } else {
                holder.badgeCount.setVisibility(View.GONE);
            }
        } else {
            holder.badgeCount.setVisibility(View.GONE);
        }

        holder.shortcutIndicator.setVisibility(View.GONE);
    }

    private void bindShortcutItemHolder(StartableItemHolder holder, StartableItem item) {
        // Cancel loading of AppItem icons into this holder.
        if (holder.iconLoader != null) {
            holder.iconLoader.cancel(true);
            holder.iconLoader = null;
        }

        File iconFile;
        if (item instanceof ShortcutItem) {
            ShortcutItem shortcutItem = (ShortcutItem) item;
            iconFile = new File(shortcutItem.getIconPath());
        } else {
            AppShortcutItem appShortcutItem = (AppShortcutItem) item;
            iconFile = new File(appShortcutItem.getIconPath());
        }
        if (iconFile.exists()) {
            holder.appIcon.setBackgroundColor(Color.TRANSPARENT);
            GlideApp.with(mContext).load(iconFile).into(holder.appIcon);
        } else {
            holder.appIcon.setImageBitmap(null);
            holder.appIcon.setBackgroundColor(mIconPlaceholderColors[mNextPlaceholderColor]);
            mNextPlaceholderColor = (mNextPlaceholderColor + 1) % mIconPlaceholderColors.length;
        }
        holder.badgeCount.setVisibility(View.GONE);
        holder.shortcutIndicator.setVisibility(View.VISIBLE);
    }

    private void bindSectionItemHolder(final SectionItemHolder holder, int position) {
        final SectionItem item = (SectionItem) getItems().get(position);
        holder.item = item;

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
    }

}
