package net.feheren_fekete.applist.applistpage.itemmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ItemMenuAdapter extends ArrayAdapter<ItemMenuItem> {

    private static final String TAG = ItemMenuAdapter.class.getSimpleName();

    private @Nullable ItemMenuListener mListener;
    private int mDefaultBackgroundResourceId;
    private int mItemSwipeThreshold; // px

    public final class ViewHolder {
        public ItemMenuItem item;
        public ViewGroup layout;
        public ImageView icon;
        public TextView name;
        public ImageView dragHandle;
        public FrameLayout pin;
        public FrameLayout contentView;
        public ViewHolder(final View itemView) {
            this.layout = itemView.findViewById(R.id.item_menu_item_layout);
            this.icon = itemView.findViewById(R.id.item_menu_item_icon);
            this.name = itemView.findViewById(R.id.item_menu_item_name);
            this.dragHandle = itemView.findViewById(R.id.item_menu_item_drag_handle);
            this.pin = itemView.findViewById(R.id.item_menu_item_pin_layout);
            this.contentView = itemView.findViewById(R.id.item_menu_item_content_view);
            this.layout.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onItemSelected(item);
                }
            });
            this.pin.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onItemPinClicked(item);
                }
            });
            this.layout.setOnTouchListener(new View.OnTouchListener() {
                private float fingerDownX;
                private boolean isSwiping;
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (!item.isSwipeable) {
                        return false;
                    }
                    boolean handled = false;
                    final int action = motionEvent.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN: {
                            fingerDownX = motionEvent.getRawX();
                            isSwiping = false;
                            layout.setTranslationX(0);
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            final float diffX = motionEvent.getRawX() - fingerDownX;
                            if (!isSwiping) {
                                if (Math.abs(diffX) > mItemSwipeThreshold) {
                                    isSwiping = true;
                                    layout.requestDisallowInterceptTouchEvent(true);
                                }
                            }
                            if (isSwiping) {
                                handled = true;
                                layout.setTranslationX(diffX);
                            }
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            final float diffX = motionEvent.getRawX() - fingerDownX;
                            if (isSwiping) {
                                handled = true;
                                if (Math.abs(diffX) > (layout.getWidth() / 2.0f)) {
                                    final float fullTranslation = (Math.signum(diffX) == 1) ? layout.getWidth() : -layout.getWidth();
                                    layout.animate()
                                            .translationX(fullTranslation)
                                            .setDuration(100)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    if (mListener != null) {
                                                        mListener.onItemSwiped(item);
                                                    }
                                                }
                                            })
                                            .start();
                                } else {
                                    layout.animate().translationX(0).setDuration(150).start();
                                }
                            }
                            isSwiping = false;
                            break;
                        }
                        case MotionEvent.ACTION_CANCEL: {
                            if (isSwiping) {
                                handled = true;
                                layout.animate().translationX(0).setDuration(150).start();
                            }
                            isSwiping = false;
                            break;
                        }
                    }
                    return handled;
                }
            });
        }
    }

    public ItemMenuAdapter(Context context) {
        super(context, R.layout.item_menu_item);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        mDefaultBackgroundResourceId = typedValue.resourceId;
        mItemSwipeThreshold = context.getResources().getDimensionPixelSize(R.dimen.item_menu_swipe_threshold);
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
        final boolean isRemoteViews = item.name.isEmpty() && item.text.isEmpty() && item.contentRemoteViews != null;

        viewHolder.pin.setVisibility(item.isPinnable ? View.VISIBLE : View.GONE);

        if (!isRemoteViews) {
            viewHolder.dragHandle.setVisibility(View.GONE);
            viewHolder.contentView.setVisibility(View.GONE);
            viewHolder.name.setVisibility(View.VISIBLE);
            if (item.icon != null) {
                viewHolder.icon.setVisibility(View.VISIBLE);
                viewHolder.icon.setImageDrawable(item.icon);
                viewHolder.name.setPadding(0, 0, 0, 0);
            } else {
                viewHolder.icon.setVisibility(View.GONE);
                viewHolder.icon.setImageDrawable(null);
                final int paddingLeft = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.item_menu_item_name_padding);
                viewHolder.name.setPadding(paddingLeft, 0, 0, 0);
            }
            if (!item.name.isEmpty()) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.item_menu_item_height);
                itemView.setLayoutParams(layoutParams);
                viewHolder.name.setMaxLines(1);
                viewHolder.name.setMaxWidth(Integer.MAX_VALUE);
                viewHolder.name.setText(item.name);
            } else if (!item.text.isEmpty()) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                itemView.setLayoutParams(layoutParams);
                viewHolder.name.setMaxLines(2);
                viewHolder.name.setMaxWidth(itemView.getContext().getResources().getDimensionPixelSize(R.dimen.item_menu_text_width));
                viewHolder.name.setText(item.text);
            }
        } else {
            viewHolder.icon.setVisibility(View.GONE);
            viewHolder.name.setVisibility(View.GONE);
            viewHolder.dragHandle.setVisibility(View.VISIBLE);
            viewHolder.contentView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            itemView.setLayoutParams(layoutParams);
            // It is important to use Application Context for the RemoteViews. Otherwise, some
            // RemoteViews cannot be displayed.
            View view = item.contentRemoteViews.apply(getContext().getApplicationContext(), viewHolder.contentView);
            viewHolder.contentView.addView(view);
        }

        if (item.backgroundResourceId != 0) {
            viewHolder.layout.setBackgroundResource(item.backgroundResourceId);
        } else {
            viewHolder.layout.setBackgroundResource(mDefaultBackgroundResourceId);
        }
        viewHolder.layout.setTranslationX(0);
        viewHolder.item = item;
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
