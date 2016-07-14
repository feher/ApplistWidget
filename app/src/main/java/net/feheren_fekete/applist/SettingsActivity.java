package net.feheren_fekete.applist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

    public static final String PREF_KEY_COLOR_THEME = "pref_key_color_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String mDefaultThemeValue;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDefaultThemeValue = getResources().getString(R.string.color_theme_value_default);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            Preference preference = findPreference(PREF_KEY_COLOR_THEME);
            preference.setSummary(getColorTheme());
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_KEY_COLOR_THEME)) {
                Preference preference = findPreference(PREF_KEY_COLOR_THEME);
                preference.setSummary(getColorTheme());
                restartMainActivity();
            }
        }

        private void restartMainActivity() {
            Intent i = new Intent(getActivity(), ApplistActivity.class);
            i.setAction(ApplistActivity.ACTION_RESTART);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(i);
            getActivity().finish();
        }

        private String getColorTheme() {
            String colorThemeValue = getPreferenceScreen().getSharedPreferences().getString(PREF_KEY_COLOR_THEME, mDefaultThemeValue);
            String[] colorThemeValues = getResources().getStringArray(R.array.color_theme_values);
            String[] colorThemes = getResources().getStringArray(R.array.color_themes);
            for (int i = 0; i < colorThemeValues.length; ++i) {
                if (colorThemeValues[i].equals(colorThemeValue)) {
                    return colorThemes[i];
                }
            }
            return "";
        }
    }

}
