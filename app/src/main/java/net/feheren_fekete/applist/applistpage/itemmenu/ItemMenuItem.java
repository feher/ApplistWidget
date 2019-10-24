package net.feheren_fekete.applist.applistpage.itemmenu;

import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;

public class ItemMenuItem {

    public final String name;
    public final String text;
    @Nullable public final Drawable icon;
    public final int backgroundResourceId;
    public final boolean isSwipeable;
    public final boolean isPinnable;
    @Nullable public final RemoteViews contentRemoteViews;
    public final Object data;

    public ItemMenuItem(String name,
                        String text,
                        @Nullable Drawable icon,
                        int backgroundResourceId,
                        boolean isSwipeable,
                        boolean isPinnable,
                        @Nullable RemoteViews contentRemoteViews,
                        Object data) {
        this.name = name;
        this.text = text;
        this.icon = icon;
        this.backgroundResourceId = backgroundResourceId;
        this.isSwipeable = isSwipeable;
        this.isPinnable = isPinnable;
        this.contentRemoteViews = contentRemoteViews;
        this.data = data;
    }

}
