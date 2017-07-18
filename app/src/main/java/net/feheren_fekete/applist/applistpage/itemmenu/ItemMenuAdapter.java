package net.feheren_fekete.applist.applistpage.itemmenu;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;

import java.util.List;

public class ItemMenuAdapter extends ArrayAdapter<ItemMenuItem> {

    private @Nullable ItemMenuListener mListener;

    public final class ViewHolder {
        public int position = -1;
        public ViewGroup layout;
        public ImageView icon;
        public TextView name;
        public ViewHolder(View itemView) {
            this.layout = itemView.findViewById(R.id.item_menu_item_layout);
            this.icon = itemView.findViewById(R.id.item_menu_item_icon);
            this.name = itemView.findViewById(R.id.item_menu_item_name);
            this.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onItemSelected(getItem(ViewHolder.this.position));
                    }
                }
            });
        }
    }

    public ItemMenuAdapter(Context context) {
        super(context, R.layout.item_menu_item);
    }

    @Override
    @NonNull
    public View getView(int i, View convertView, @NonNull ViewGroup viewGroup) {
        View itemView;
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(R.layout.item_menu_item, viewGroup, false);
            viewHolder = new ViewHolder(itemView);
            itemView.setTag(viewHolder);
        } else {
            itemView = convertView;
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ItemMenuItem item = getItem(i);
        viewHolder.icon.setImageDrawable(item.icon);
        viewHolder.name.setText(item.name);
        viewHolder.position = i;
        return itemView;
    }

    public void setItems(List<ItemMenuItem> items) {
        addAll(items);
        notifyDataSetChanged();
    }

    public void setListener(@Nullable ItemMenuListener listener) {
        mListener = listener;
    }

}
