package net.feheren_fekete.applist.applistpage.itemmenu;

import android.graphics.drawable.Drawable;

public class ItemMenuItem {

    public final String name;
    public final String text;
    public final Drawable icon;
    public final int backgroundResourceId;
    public final Object data;

    public ItemMenuItem(String name, String text, Drawable icon, int backgroundResourceId, Object data) {
        this.name = name;
        this.text = text;
        this.icon = icon;
        this.backgroundResourceId = backgroundResourceId;
        this.data = data;
    }

}
