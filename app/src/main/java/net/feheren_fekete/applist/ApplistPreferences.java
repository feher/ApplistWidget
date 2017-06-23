package net.feheren_fekete.applist;

import android.content.Context;
import android.content.SharedPreferences;

public class ApplistPreferences {

    private static final String APPLIST_PREFERENCES = "APPLIST_PREFERENCES";

    private static final String PREFERENCE_SHOW_REARRANGE_ITEMS_HELP = "PREFERENCE_SHOW_REARRANGE_ITEMS_HELP";
    private static final boolean DEFAULT_SHOW_REARRANGE_ITEMS_HELP = true;

    private static final String PREFERENCE_SHOW_USE_LAUNCHER_TIP = "PREFERENCE_SHOW_USE_LAUNCHER_TIP";
    private static final boolean DEFAULT_SHOW_USE_LAUNCHER_TIP = true;

    private static final String PREFERENCE_DEVICE_LOCALE = "DEVICE_LOCALE";
    private static final String DEFAULT_DEVICE_LOCALE = "";

    private static final String PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION = "LAST_ACTIVE_LAUNCHER_PAGE_POSITION";
    private static final int DEFAULT_LAST_ACTIVE_LAUNCHER_PAGE_POSITION = -1;

    private SharedPreferences mSharedPreferences;

    public ApplistPreferences(Context context) {
        mSharedPreferences = context.getApplicationContext().getSharedPreferences(
                APPLIST_PREFERENCES, Context.MODE_PRIVATE);
    }

    public boolean getShowRearrangeItemsHelp() {
        return mSharedPreferences.getBoolean(PREFERENCE_SHOW_REARRANGE_ITEMS_HELP, DEFAULT_SHOW_REARRANGE_ITEMS_HELP);
    }

    public void setShowRearrangeItemsHelp(boolean showRearrangeItemsHelp) {
        mSharedPreferences.edit().putBoolean(PREFERENCE_SHOW_REARRANGE_ITEMS_HELP, showRearrangeItemsHelp).apply();
    }

    public boolean getShowUseLauncherTip() {
        return mSharedPreferences.getBoolean(PREFERENCE_SHOW_USE_LAUNCHER_TIP, DEFAULT_SHOW_USE_LAUNCHER_TIP);
    }

    public void setShowUseLauncherTip(boolean show) {
        mSharedPreferences.edit().putBoolean(PREFERENCE_SHOW_USE_LAUNCHER_TIP, show).apply();
    }

    public String getDeviceLocale() {
        return mSharedPreferences.getString(PREFERENCE_DEVICE_LOCALE, DEFAULT_DEVICE_LOCALE);
    }

    public void setDeviceLocale(String deviceLocale) {
        mSharedPreferences.edit().putString(PREFERENCE_DEVICE_LOCALE, deviceLocale).apply();
    }

    public int getLastActiveLauncherPagePosition() {
        return mSharedPreferences.getInt(PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION, DEFAULT_LAST_ACTIVE_LAUNCHER_PAGE_POSITION);
    }

    public void setLastActiveLauncherPagePosition(int pagePosition) {
        mSharedPreferences.edit().putInt(PREFERENCE_LAST_ACTIVE_LAUNCHER_PAGE_POSITION, pagePosition).apply();
    }

}
