package net.feheren_fekete.applist.applistpage.viewmodel;

import android.content.Intent;

public class ShortcutItem extends StartableItem {

    private String mName;
    private Intent mIntent;

    public ShortcutItem(long id, String name, Intent intent) {
        super(id);
        mName = name;
        mIntent = intent;
    }

    @Override
    public String getName() {
        return mName;
    }

    public Intent getIntent() {
        return mIntent;
    }
}
