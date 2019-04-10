package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.widgetpage.WidgetUtils;

import androidx.recyclerview.widget.RecyclerView;

public class WidgetPickerViewHolder extends RecyclerView.ViewHolder {

    public interface Listener {
        void onWidgetTapped(int position);
    }

    private ImageView mIcon;
    private ImageView mImage;
    private TextView mTitle;
    private Listener mListener;

    public WidgetPickerViewHolder(View itemView, Listener listener) {
        super(itemView);
        mListener = listener;
        mIcon = itemView.findViewById(R.id.widget_picker_item_icon);
        mImage = itemView.findViewById(R.id.widget_picker_item_image);
        mTitle = itemView.findViewById(R.id.widget_picker_item_title);
        itemView.findViewById(R.id.widget_picker_item_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onWidgetTapped(getAdapterPosition());
            }
        });
    }

    public void bind(WidgetPickerItem widgetPickerItem, WidgetUtils widgetUtils) {
        final Drawable icon = widgetPickerItem.getIcon(mIcon.getContext(), widgetUtils);
        final Drawable previewImage = widgetPickerItem.getPreviewImage(mIcon.getContext(), widgetUtils);
        mIcon.setImageDrawable(icon);
        mImage.setImageDrawable(previewImage != null ? previewImage : icon);
        mTitle.setText(widgetPickerItem.getLabel(mTitle.getContext(), widgetUtils));
    }

}
