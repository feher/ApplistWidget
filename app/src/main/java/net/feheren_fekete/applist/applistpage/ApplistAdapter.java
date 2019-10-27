package net.feheren_fekete.applist.applistpage;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ApplistAdapter
        extends RecyclerView.Adapter<ViewHolderBase> {

    private static final String TAG = ApplistAdapter.class.getSimpleName();

    public static final int STARTABLE_ITEM_VIEW = 1;
    public static final int SECTION_ITEM_VIEW = 2;

    private ApplistLog mApplistLog;
    private List<BaseItem> mCollapsedItems;
    private List<BaseItem> mAllItems;
    private @Nullable String mFilterName;
    private @Nullable List<BaseItem> mFilteredItems;
    private ItemListener mItemListener;
    private boolean mIsSelectionModeEnabled = false;

    // HACK: Needed to preserve the selected state of the item that
    //       triggered the mIsSelectionModeEnabled.
    //       I.e. when the user long-taps an app and selects "Reorder apps".
    private long mStickySelectedItemId = -1;

    public interface ItemListener {
        void onStartableTapped(StartableItem startableItem);
        void onStartableLongTapped(StartableItem startableItem);
        void onStartableTouched(StartableItem startableItem);
        void onSectionTapped(SectionItem sectionItem);
        void onSectionLongTapped(SectionItem sectionItem);
        void onSectionTouched(SectionItem sectionItem);
    }

    public ApplistAdapter(ApplistLog applistLog, ItemListener itemListener) {
        mCollapsedItems = Collections.emptyList();
        mAllItems = Collections.emptyList();
        mItemListener = itemListener;
        mApplistLog = applistLog;

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
            StartableItem startableItem = (StartableItem) getItems().get(position);
            ((StartableItemHolder)holder).bind(startableItem, mIsSelectionModeEnabled);
        } else if (holder instanceof SectionItemHolder) {
            SectionItem item = (SectionItem) getItems().get(position);
            ((SectionItemHolder)holder).bind(item);
        }
    }

    public void setSelectionModeEnabled(boolean enabled) {
        mIsSelectionModeEnabled = enabled;
        notifyDataSetChanged();
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

    public List<String> getStartableDisplayNames() {
        List<String> result = new ArrayList<>();
        for (BaseItem item : getItems()) {
            if (item instanceof StartableItem) {
                StartableItem startableItem = (StartableItem) item;
                result.add(startableItem.getDisplayName());
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

    public void setAllSectionsCollapsed(boolean collapsed) {
        for (BaseItem item : mAllItems) {
            if (item instanceof SectionItem) {
                ((SectionItem) item).setCollapsed(collapsed);
            }
        }
        updateCollapsedAndFilteredItems();
        notifyDataSetChanged();
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
        applyStickySelection();
        updateCollapsedAndFilteredItems();
        notifyDataSetChanged();
    }

    public BaseItem getItem(int position) {
        return getItems().get(position);
    }

    public List<Long> getAllItemIds() {
        List<Long> result = new ArrayList<>();
        for (BaseItem item : mAllItems) {
            result.add(item.getId());
        }
        return result;
    }

    public List<Long> getAllParentSectionIds() {
        List<Long> result = new ArrayList<>();
        for (BaseItem item : mAllItems) {
            if (item instanceof SectionItem) {
                result.add(ApplistItemData.INVALID_ID);
            } else if (item instanceof StartableItem) {
                result.add(((StartableItem) item).getParentSectionId());
            }
        }
        return result;
    }

    public void setHighlighted(BaseItem item, boolean highlighted) {
        item.setHighlighted(highlighted);
        notifyItemChanged(getItemPosition(item));
    }

    private void applyStickySelection() {
        if (mStickySelectedItemId == -1) {
            return;
        }
        for (BaseItem item : mAllItems) {
            if (item.getId() == mStickySelectedItemId) {
                item.setSelected(true);
            }
        }
        mStickySelectedItemId = -1;
    }

    public void setSelected(BaseItem item, boolean selected) {
        item.setSelected(selected);
        if (selected) {
            mStickySelectedItemId = item.getId();
        } else if (!hasSelectedItem()) {
            mStickySelectedItemId = -1;
        }
        notifyItemChanged(getItemPosition(item));
    }

    public void unselectAll() {
        mStickySelectedItemId = -1;
        for (BaseItem item : getItems()) {
            if (item.isSelected()) {
                item.setSelected(false);
                notifyItemChanged(getItemPosition(item));
            }
        }
    }

    public int getSelectedCount() {
        int count = 0;
        for (BaseItem item : getItems()) {
            if (item.isSelected()) {
                ++count;
            }
        }
        return count;
    }

    public boolean hasSelectedItem() {
        for (BaseItem item : getItems()) {
            if (item.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public List<Long> getSelectedIds() {
        List<Long> ids = new ArrayList<>();
        for (BaseItem item : getItems()) {
            if (item.isSelected()) {
                ids.add(item.getId());
            }
        }
        return ids;
    }

    public void setDragged(BaseItem item, boolean dragged) {
        item.setDragged(dragged);
        notifyItemChanged(getItemPosition(item));
    }

    public void clearDragged() {
        for (BaseItem item : getItems()) {
            if (item.isDragged()) {
                item.setDragged(false);
                notifyItemChanged(getItemPosition(item));
            }
        }
    }

    public boolean moveItemsToSection(List<Long> itemIds, long sectionId) {
        List<BaseItem> items = getItems();
        List<BaseItem> movedItems = new ArrayList<>();
        BaseItem sectionItem = null;

        for (BaseItem item : items) {
            if (itemIds.contains(item.getId())) {
                movedItems.add(item);
            }
            if (item.getId() == sectionId) {
                sectionItem = item;
            }
        }

        if (sectionItem == null) {
            return false;
        }

        items.removeAll(movedItems);

        int sectionIndex = -1;
        int lastSectionItemIndex = -1;
        for (int i = 0; i < items.size(); ++i) {
            BaseItem item = items.get(i);
            if (item instanceof SectionItem) {
                if (sectionIndex == -1) {
                    if (item.getId() == sectionId) {
                        sectionIndex = i;
                        lastSectionItemIndex = sectionIndex + 1;
                    }
                } else {
                    lastSectionItemIndex = i - 1;
                }
            }
        }
        items.addAll(lastSectionItemIndex + 1, movedItems);

        notifyDataSetChanged();

        return true;
    }

    public boolean moveItem(int oldPosition, int newPosition) {
        if (newPosition == oldPosition) {
            return false;
        }

        List<BaseItem> items = getItems();
        BaseItem item = items.get(oldPosition);

        // Cannot move startable above 1st section
        if (item instanceof StartableItem) {
            if (newPosition == 0) {
                return false;
            }
            if (hasCollapsedSection()) {
                throw new IllegalStateException();
            }
        }

        // Change the item position.
        items.remove(oldPosition);
        items.add(newPosition, item);

        // Update the item's parentSectionId.
        if (item instanceof StartableItem) {
            StartableItem startableItem = (StartableItem) item;
            BaseItem previousItem = (newPosition > 0) ? items.get(newPosition - 1) : null;
            BaseItem nextItem = (newPosition < items.size() - 1) ? items.get(newPosition + 1) : null;
            if (previousItem instanceof SectionItem) {
                startableItem.setParentSectionId(previousItem.getId());
            } else if (previousItem instanceof StartableItem) {
                startableItem.setParentSectionId(((StartableItem) previousItem).getParentSectionId());
            } else if (nextItem instanceof StartableItem) {
                startableItem.setParentSectionId(((StartableItem) nextItem).getParentSectionId());
            }
            mAllItems = items;
            updateCollapsedAndFilteredItems();
        }

        // Reposition the section's items
        if (item instanceof SectionItem) {
            List<BaseItem> newAllItems = new ArrayList<>();
            for (BaseItem section : items) {
                newAllItems.add(section);
                for (BaseItem itm : mAllItems) {
                    if (itm instanceof StartableItem) {
                        StartableItem startableItem = (StartableItem) itm;
                        if (startableItem.getParentSectionId() == section.getId()) {
                            newAllItems.add(startableItem);
                        }
                    }
                }
            }
            mAllItems = newAllItems;
            updateCollapsedAndFilteredItems();
        }

        notifyItemMoved(oldPosition, newPosition);
        return true;
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

    private boolean hasCollapsedSection() {
        for (BaseItem item : mAllItems) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                if (sectionItem.isCollapsed()) {
                    return true;
                }
            }
        }
        return false;
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
                    String lowercaseItemName = ((StartableItem) item).getDisplayName().toLowerCase();
                    if (lowercaseItemName.contains(lowercaseFilterText)) {
                        result.add(item);
                    }
                }
            }
            return result;
        }
    }

}
