package net.feheren_fekete.applist.applistpage.model;

import android.content.Intent;

public class ShortcutData extends StartableData {

    private Intent mIntent;
    private String mCachedIntentUri;

    public ShortcutData(long id, String name, Intent intent) {
        super(id, name);
        mIntent = intent;
    }

    @Override
    public String getPackageName() {
        return mIntent.getPackage();
    }

    public Intent getIntent(){
        return mIntent;
    }

    private String getIntentUri() {
        if (mCachedIntentUri == null) {
            mCachedIntentUri = mIntent.toUri(0);
        }
        return mCachedIntentUri;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ShortcutData)) {
            return false;
        }
        final ShortcutData other = (ShortcutData) o;
        return getIntentUri().equals(other.getIntentUri());
    }
}
