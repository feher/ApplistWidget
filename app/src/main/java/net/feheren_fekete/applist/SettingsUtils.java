package net.feheren_fekete.applist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUtils {

    public static void applyColorTheme(Activity activity) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String colorThemeName = sharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, "Brown");
        switch (colorThemeName) {
            case "Transparent":
                activity.setTheme(R.style.MyThemeTransparent);
                break;
            case "Black":
                activity.setTheme(R.style.MyThemeBlack);
                break;
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
            case "Red":
                activity.setTheme(R.style.MyThemeRed);
                break;
            case "Green":
                activity.setTheme(R.style.MyThemeGreen);
                break;
        }
    }

    public static int getColumnWidth(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String columnWidthValue = sharedPref.getString(
                SettingsActivity.PREF_KEY_COLUMN_WIDTH,
                context.getResources().getString(R.string.column_width_value_default));
        return Integer.parseInt(columnWidthValue);
    }

    public static boolean isThemeTransparent(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String colorThemeName = sharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, "Brown");
        return colorThemeName.equals("Transparent");
    }

    public static boolean isKeepAppsSortedAlphabetically(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPref.getBoolean(SettingsActivity.PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY, false);
    }

    public static boolean getShowBadge(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sharedPref.getBoolean(SettingsActivity.PREF_KEY_SHOW_BADGE, false);
    }

}
