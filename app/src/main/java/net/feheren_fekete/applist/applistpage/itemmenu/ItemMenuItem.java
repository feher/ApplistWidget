package net.feheren_fekete.applist.applistpage.itemmenu;

import android.graphics.drawable.Drawable;

public class ItemMenuItem {

    public final String name;
    public final Drawable icon;
    public final Object data;

    public ItemMenuItem(String name, Drawable icon, Object data) {
        this.name = name;
        this.icon = icon;
        this.data = data;
    }

}
