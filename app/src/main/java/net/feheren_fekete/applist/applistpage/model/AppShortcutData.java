package net.feheren_fekete.applist.applistpage.model;

public class AppShortcutData extends StartableData {

    private String mPackageName;
    private String mShortcutId;

    public AppShortcutData(long id, String name, String packageName, String shortcutId) {
        super(id, name);
        mPackageName = packageName;
        mShortcutId = shortcutId;
    }

    public String getShortcutId(){
        return mShortcutId;
    }

    @Override
    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AppShortcutData)) {
            return false;
        }
        AppShortcutData other = (AppShortcutData) o;
        return mPackageName.equals(other.mPackageName)
                && mShortcutId.equals(other.mShortcutId);
    }
}
