package net.feheren_fekete.applist.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.ArrayMap;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsUtils {

    private Context mContext;
    private SharedPreferences mSharedPref;
    private Map<String, Integer> mColorThemes;
    private List<String> mTransparentColorThemes;
    private String mDefaultColorTheme;

    private static SettingsUtils sInstance;

    public static void initInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsUtils(context);
        }
    }

    public static SettingsUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(SettingsUtils.class.getSimpleName() + " singleton is not initialized");
        }
    }

    private SettingsUtils(Context context) {
        mContext = context;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        mDefaultColorTheme = context.getString(R.string.color_theme_value_default);
    }

    public void applyColorTheme(Activity activity) {
        if (mColorThemes == null) {
            mColorThemes = createColorThemeMap();
        }

        final String colorTheme = mSharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, mDefaultColorTheme);
        final Integer colorThemeId = mColorThemes.get(colorTheme);
        if (colorThemeId != null) {
            activity.setTheme(colorThemeId);
        } else {
            ApplistLog.getInstance().log(new RuntimeException("Invalid color theme, reverting to default theme: " + colorTheme));
            mSharedPref.edit().putString(SettingsActivity.PREF_KEY_COLOR_THEME, mDefaultColorTheme).apply();
            activity.setTheme(mColorThemes.get(mDefaultColorTheme));
        }
    }

    public int getColumnWidth() {
        String columnWidthValue = mSharedPref.getString(
                SettingsActivity.PREF_KEY_COLUMN_WIDTH,
                mContext.getResources().getString(R.string.column_width_value_default));
        return Integer.parseInt(columnWidthValue);
    }

    public boolean isThemeTransparent() {
        if (mTransparentColorThemes == null) {
            mTransparentColorThemes = new ArrayList<>();
            mTransparentColorThemes.add(mContext.getString(R.string.color_theme_value_transparent_light));
            mTransparentColorThemes.add(mContext.getString(R.string.color_theme_value_transparent_dark));
        }
        final String colorTheme = mSharedPref.getString(SettingsActivity.PREF_KEY_COLOR_THEME, mDefaultColorTheme);
        return mTransparentColorThemes.contains(colorTheme);
    }

    public boolean isKeepAppsSortedAlphabetically() {
        return mSharedPref.getBoolean(SettingsActivity.PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY, false);
    }

    public boolean getShowBadge() {
        return getShowNewContentBadge()
                || getShowPhoneBadge()
                || getShowSmsBadge()
                || getShowNotificationBadge();
    }

    public boolean getShowNewContentBadge() {
        return mSharedPref.getBoolean(SettingsActivity.PREF_KEY_SHOW_NEW_CONTENT_BADGE, false);
    }

    public boolean getShowPhoneBadge() {
        return mSharedPref.getBoolean(SettingsActivity.PREF_KEY_SHOW_PHONE_BADGE, false);
    }

    public boolean getShowSmsBadge() {
        return mSharedPref.getBoolean(SettingsActivity.PREF_KEY_SHOW_SMS_BADGE, false);
    }

    public boolean getShowNotificationBadge() {
        return mSharedPref.getBoolean(SettingsActivity.PREF_KEY_SHOW_NOTIFICATION_BADGE, false);
    }

    private Map<String, Integer> createColorThemeMap() {
        Map<String, Integer> result = new ArrayMap<>();
        result.put(mContext.getString(R.string.color_theme_value_transparent_light), R.style.MyThemeTransparentLight);
        result.put(mContext.getString(R.string.color_theme_value_transparent_dark), R.style.MyThemeTransparentDark);
        result.put(mContext.getString(R.string.color_theme_value_black), R.style.MyThemeBlack);
        result.put(mContext.getString(R.string.color_theme_value_brown), R.style.MyThemeBrown);
        result.put(mContext.getString(R.string.color_theme_value_blue_grey), R.style.MyThemeBlueGrey);
        result.put(mContext.getString(R.string.color_theme_value_deep_purple), R.style.MyThemeDeepPurple);
        result.put(mContext.getString(R.string.color_theme_value_indigo), R.style.MyThemeIndigo);
        result.put(mContext.getString(R.string.color_theme_value_red), R.style.MyThemeRed);
        result.put(mContext.getString(R.string.color_theme_value_green), R.style.MyThemeGreen);
        return result;
    }

}
