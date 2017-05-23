package net.feheren_fekete.applist;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import net.feheren_fekete.applist.utils.AppUtils;
import net.feheren_fekete.applist.utils.RunnableWithArg;


public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREF_KEY_COLOR_THEME = "pref_key_color_theme";
    public static final String PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY = "pref_key_keep_apps_sorted_alphabetically";
    public static final String PREF_KEY_SHOW_BADGE = "pref_key_show_badge";
    public static final String PREF_KEY_SHOW_SMS_BADGE = "pref_key_show_sms_badge";
    public static final String PREF_KEY_SHOW_PHONE_BADGE = "pref_key_show_phone_badge";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(
                        android.R.id.content,
                        new SettingsFragment(),
                        SettingsFragment.class.getName())
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // This is a framework bugfix.
        // https://code.google.com/p/android/issues/detail?id=189121#c5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Fragment fragment = getFragmentManager().findFragmentByTag(
                SettingsFragment.class.getName());
        if (fragment != null) {
            fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final int SMS_PERMISSION_REQUEST_CODE = 1;
        private static final int PHONE_PERMISSION_REQUEST_CODE = 2;
        private String mDefaultThemeValue;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDefaultThemeValue = getResources().getString(R.string.color_theme_value_default);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onStart() {
            super.onStart();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            Preference preference = findPreference(PREF_KEY_COLOR_THEME);
            preference.setSummary(getColorTheme());
        }

        @Override
        public void onStop() {
            super.onStop();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_KEY_COLOR_THEME)) {
                Preference preference = findPreference(PREF_KEY_COLOR_THEME);
                preference.setSummary(getColorTheme());
                restartMainActivity();
            } else if (key.equals(PREF_KEY_SHOW_SMS_BADGE)) {
                if (getShowSmsBadge()) {
                    ensurePermission(Manifest.permission.RECEIVE_SMS, SMS_PERMISSION_REQUEST_CODE);
                }
            } else if (key.equals(PREF_KEY_SHOW_PHONE_BADGE)) {
                if (getShowPhoneBadge()) {
                    if (ensurePermission(Manifest.permission.READ_PHONE_STATE, PHONE_PERMISSION_REQUEST_CODE)) {
                        ensureDefaultPhoneApp();
                    }
                }
            }
        }

        private boolean ensurePermission(String permission, int requestCode) {
            Activity activity = getActivity();
            if (activity == null) {
                return false;
            }
            int permissionState = ActivityCompat.checkSelfPermission(activity, permission);
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{ permission }, requestCode);
                return false;
            } else {
                Log.d(TAG, "Already has permission " + permission);
                return true;
            }
        }

        private boolean ensureDefaultPhoneApp() {
            final Context appContext = getActivity().getApplicationContext();
            if (AppUtils.getPhoneApp(appContext) == null) {
                SwitchPreference showPhoneBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_PHONE_BADGE);
                showPhoneBadge.setChecked(false);
                ApplistDialogs.chooseAppDialog(
                        getActivity(),
                        getString(R.string.settings_show_phone_badge_select_default_app),
                        AppUtils.getAvailableAppsForIntent(getActivity(), AppUtils.getPhoneIntent()),
                        new RunnableWithArg<ResolveInfo>() {
                            @Override
                            public void run(ResolveInfo selectedPhoneApp) {
                                ComponentName componentName = new ComponentName(
                                        selectedPhoneApp.activityInfo.packageName,
                                        selectedPhoneApp.activityInfo.name);
                                AppUtils.savePhoneApp(appContext, componentName);
                                SwitchPreference showPhoneBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_PHONE_BADGE);
                                showPhoneBadge.setChecked(true);
                            }
                        });
                return false;
            } else {
                return true;
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            switch (requestCode) {
                case SMS_PERMISSION_REQUEST_CODE:
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "SMS permission granted");
                    } else {
                        SwitchPreference showSmsBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_SMS_BADGE);
                        showSmsBadge.setChecked(false);
                    }
                    break;
                case PHONE_PERMISSION_REQUEST_CODE:
                    SwitchPreference showPhoneBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_PHONE_BADGE);
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Phone permission granted");
                        if (!ensureDefaultPhoneApp()) {
                            showPhoneBadge.setChecked(false);
                        }
                    } else {
                        showPhoneBadge.setChecked(false);
                    }
                    break;
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

        private boolean getShowSmsBadge() {
            return getPreferenceScreen().getSharedPreferences().getBoolean(
                    PREF_KEY_SHOW_SMS_BADGE, false);
        }

        private boolean getShowPhoneBadge() {
            return getPreferenceScreen().getSharedPreferences().getBoolean(
                    PREF_KEY_SHOW_PHONE_BADGE, false);
        }

    }

}
