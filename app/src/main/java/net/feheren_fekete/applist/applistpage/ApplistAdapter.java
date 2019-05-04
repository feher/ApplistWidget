package net.feheren_fekete.applist.applistpage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
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

    private List<BaseItem> mCollapsedItems;
    private List<BaseItem> mAllItems;
    private @Nullable String mFilterName;
    private @Nullable List<BaseItem> mFilteredItems;
    private ItemListener mItemListener;

    public interface ItemListener {
        void onStartableTapped(StartableItem startableItem);
        void onStartableLongTapped(StartableItem startableItem);
        void onStartableTouched(StartableItem startableItem);
        void onSectionTapped(SectionItem sectionItem);
        void onSectionLongTapped(SectionItem sectionItem);
        void onSectionTouched(SectionItem sectionItem);
    }

    public ApplistAdapter(ItemListener itemListener) {
        mCollapsedItems = Collections.emptyList();
        mAllItems = Collections.emptyList();
        mItemListener = itemListener;

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
            ((StartableItemHolder)holder).bind(startableItem);
        } else if (holder instanceof SectionItemHolder) {
            SectionItem item = (SectionItem) getItems().get(position);
            ((SectionItemHolder)holder).bind(item);
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
