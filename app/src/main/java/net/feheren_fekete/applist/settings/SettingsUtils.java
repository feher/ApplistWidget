package net.feheren_fekete.applist.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.ArrayMap;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;

import java.util.Map;

public class SettingsUtils {

    private static Map<String, Integer> sColorThemes;

    public static Map<String, Integer> createColorThemeMap(Context context) {
        Map<String, Integer> result = new ArrayMap<>();
        result.put(context.getString(R.string.color_theme_value_transparent_light), R.style.MyThemeTransparentLight);
        result.put(context.getString(R.string.color_theme_value_transparent_dark), R.style.MyThemeTransparentDark);
        result.put(context.getString(R.string.color_theme_value_black), R.style.MyThemeBlack);
        result.put(context.getString(R.string.color_theme_value_brown), R.style.MyThemeBrown);
        result.put(context.getString(R.string.color_theme_value_blue_grey), R.style.MyThemeBlueGrey);
        result.put(context.getString(R.string.color_theme_value_deep_purple), R.style.MyThemeDeepPurple);
        result.put(context.getString(R.string.color_theme_value_indigo), R.style.MyThemeIndigo);
        result.put(context.getString(R.string.color_theme_value_red), R.style.MyThemeRed);
        result.put(context.getString(R.string.color_theme_value_green), R.style.MyThemeGreen);
        return result;
    }

    public static void applyColorTheme(Activity activity) {
        if (sColorThemes == null) {
            sColorThemes = createColorThemeMap(activity);
        }

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        final String defaultColorThemeName = activity.getString(R.string.color_theme_value_default);
        final String colorThemeName = sharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, defaultColorThemeName);
        final Integer colorThemeId = sColorThemes.get(colorThemeName);
        if (colorThemeId != null) {
            activity.setTheme(colorThemeId);
        } else {
            ApplistLog.getInstance().log(new RuntimeException("Invalid color theme: " + colorThemeName));
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
