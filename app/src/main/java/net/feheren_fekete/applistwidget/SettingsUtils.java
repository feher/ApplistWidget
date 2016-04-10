package net.feheren_fekete.applistwidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUtils {
    public static void applyColorTheme(Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        String colorThemeName = sharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, "Brown");
        switch (colorThemeName) {
            case "Brown":
                activity.setTheme(R.style.MyThemeBrown);
                break;
            case "Blue Grey":
                activity.setTheme(R.style.MyThemeBlueGrey);
                break;
            case "Deep Purple":
                activity.setTheme(R.style.MyThemeDeepPurple);
                break;
            case "Indigo":
                activity.setTheme(R.style.MyThemeIndigo);
                break;
            case "Orange":
                activity.setTheme(R.style.MyThemeOrange);
                break;
            case "Green":
                activity.setTheme(R.style.MyThemeGreen);
                break;
        }
    }
}
