package net.feheren_fekete.applist.applistpage.viewmodel;

import android.content.Intent;

public class ShortcutItem extends StartableItem {

    private String mName;
    private Intent mIntent;
    private String mIconPath;

    public ShortcutItem(long id, String name, Intent intent, String iconPath) {
        super(id);
        mName = name;
        mIntent = intent;
        mIconPath = iconPath;
    }

    @Override
    public String getName() {
        return mName;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public String getIconPath() {
        return mIconPath;
    }

}
