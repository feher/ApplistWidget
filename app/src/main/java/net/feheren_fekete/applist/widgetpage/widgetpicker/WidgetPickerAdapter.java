package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.R;

import java.util.ArrayList;
import java.util.List;

public class WidgetPickerAdapter extends RecyclerView.Adapter<WidgetPickerViewHolder> {

    private WidgetPickerViewHolder.Listener mListener;
    private List<WidgetPickerItem> mItems = new ArrayList<>();

    public WidgetPickerAdapter(Context context, WidgetPickerViewHolder.Listener listener) {
        mListener = listener;
    }

    public void setItems(List<WidgetPickerData> widgetPickerDatas) {
        mItems.clear();
        for (WidgetPickerData widgetPickerData : widgetPickerDatas) {
            mItems.add(new WidgetPickerItem(widgetPickerData));
        }
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
        holder.bind(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

}
