package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.widgetpage.WidgetUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class WidgetPickerAdapter extends RecyclerView.Adapter<WidgetPickerViewHolder> {

    // TODO: Inject
    private final WidgetUtils mWidgetUtils = WidgetUtils.getInstance();

    private WidgetPickerViewHolder.Listener mListener;
    private List<WidgetPickerItem> mItems = new ArrayList<>();

    public WidgetPickerAdapter(WidgetPickerViewHolder.Listener listener) {
        mListener = listener;
    }

    public void setItems(List<WidgetPickerItem> widgetPickerItems) {
        mItems = widgetPickerItems;
        notifyDataSetChanged();
    }

    public WidgetPickerItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public WidgetPickerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_picker_item, parent, false);
        return new WidgetPickerViewHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(WidgetPickerViewHolder holder, int position) {
        holder.bind(mItems.get(position), mWidgetUtils);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

}
