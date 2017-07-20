package net.feheren_fekete.applist.applistpage.itemmenu;

import android.graphics.drawable.Drawable;

public class ItemMenuItem {

    public final String name;
    public final String text;
    public final Drawable icon;
    public final int backgroundResourceId;
    public final boolean isSwipeable;
    public final Object data;

    public ItemMenuItem(String name, String text, Drawable icon, int backgroundResourceId, boolean isSwipeable, Object data) {
        this.name = name;
        this.text = text;
        this.icon = icon;
        this.backgroundResourceId = backgroundResourceId;
        this.isSwipeable = isSwipeable;
        this.data = data;
    }

}
