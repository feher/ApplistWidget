package net.feheren_fekete.applist;

import android.content.Context;
import android.content.SharedPreferences;

public class ApplistPreferences {

    private static final String APPLIST_PREFERENCES = "APPLIST_PREFERENCES";

    private static final String PREFERENCE_DEVICE_LOCALE = "DEVICE_LOCALE";
    private static final String DEFAULT_DEVICE_LOCALE = "";

    private SharedPreferences mSharedPreferences;

    public ApplistPreferences(Context context) {
        mSharedPreferences = context.getApplicationContext().getSharedPreferences(
                APPLIST_PREFERENCES, Context.MODE_PRIVATE);
    }

    public String getDeviceLocale() {
        return mSharedPreferences.getString(PREFERENCE_DEVICE_LOCALE, DEFAULT_DEVICE_LOCALE);
    }

    public void setDeviceLocale(String deviceLocale) {
        mSharedPreferences.edit().putString(PREFERENCE_DEVICE_LOCALE, deviceLocale).apply();
    }

}
