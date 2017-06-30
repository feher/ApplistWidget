package net.feheren_fekete.applist.applistpage.viewmodel;

public class AppShortcutItem extends StartableItem {

    private String mName;
    private String mPackageName;
    private String mShortcutId;
    private String mIconPath;

    public AppShortcutItem(long id, String name, String packageName, String shortcutId, String iconPath) {
        super(id);
        mName = name;
        mPackageName = packageName;
        mShortcutId = shortcutId;
        mIconPath = iconPath;
    }

    @Override
    public String getName() {
        return mName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getShortcutId() {
        return mShortcutId;
    }

    public String getIconPath() {
        return mIconPath;
    }

}
