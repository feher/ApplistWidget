package net.feheren_fekete.applist.settings;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.WindowManager;

import net.feheren_fekete.applist.MainActivity;
import net.feheren_fekete.applist.applistpage.ApplistDialogs;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.launcher.LauncherUtils;
import net.feheren_fekete.applist.utils.AppUtils;
import net.feheren_fekete.applist.utils.RunnableWithArg;
import net.feheren_fekete.applist.utils.ScreenUtils;

import java.util.concurrent.Callable;

import bolts.Task;

import static org.koin.java.KoinJavaComponent.get;


public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREF_KEY_COLOR_THEME = "pref_key_color_theme";
    public static final String PREF_KEY_COLUMN_WIDTH = "pref_key_column_width";
    public static final String PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY = "pref_key_keep_apps_sorted_alphabetically";
    public static final String PREF_KEY_SHOW_NEW_CONTENT_BADGE = "pref_key_show_new_content_badge";
    public static final String PREF_KEY_SHOW_SMS_BADGE = "pref_key_show_sms_badge";
    public static final String PREF_KEY_SHOW_PHONE_BADGE = "pref_key_show_phone_badge";
    public static final String PREF_KEY_SHOW_NOTIFICATION_BADGE = "pref_key_show_notification_badge";

    private SettingsUtils mSettingsUtils = get(SettingsUtils.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        mSettingsUtils.applyColorTheme(this);

        // We don't want the settings screen to be transparent.
        // So, we revert the windowShowWallpaper flag and restore the background color.
        // REF: 2017_06_23_22_41_dont_show_wallpaper_in_settings_screen.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawableResource(android.R.color.white);

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

        // TODO: Inject these singletons.
        private ScreenUtils mScreenUtils = ScreenUtils.getInstance();
        private LauncherUtils mLauncherUtils = LauncherUtils.getInstance();
        private ApplistModel mApplistModel = get(ApplistModel.class);

        private static final int SMS_PERMISSION_REQUEST_CODE = 1;
        private static final int PHONE_PERMISSION_REQUEST_CODE = 2;
        private String mDefaultThemeValue;
        private String mDefaultColumnWidthValue;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDefaultThemeValue = getResources().getString(R.string.color_theme_value_default);
            mDefaultColumnWidthValue = getResources().getString(R.string.column_width_value_default);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
            final int topPadding = mScreenUtils.getStatusBarHeight(getActivity());
            // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
            final int bottomPadding = mScreenUtils.hasNavigationBar(getActivity()) ? mScreenUtils.getNavigationBarHeight(getActivity()) : 0;
            getView().setPadding(0, topPadding, 0, bottomPadding);
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

            preference = findPreference(PREF_KEY_COLUMN_WIDTH);
            preference.setSummary(getColumnWidth());
        }

        @Override
        public void onStop() {
            super.onStop();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY)) {
                handleChangeKeepAppsSorted();
            } else if (key.equals(PREF_KEY_COLOR_THEME)) {
                handleChangeColorTheme();
            } else if (key.equals(PREF_KEY_COLUMN_WIDTH)) {
                handleChangeColumnWidth();
            } else if (key.equals(PREF_KEY_SHOW_SMS_BADGE)) {
                handleChangeShowSmsBadge();
            } else if (key.equals(PREF_KEY_SHOW_PHONE_BADGE)) {
                handleChangeShowPhoneBadge();
            } else if (key.equals(PREF_KEY_SHOW_NOTIFICATION_BADGE)) {
                handleChangeShowNotificationBadge();
            }
        }

        private void handleChangeKeepAppsSorted() {
            if (getKeepAppsSortedAlphabetically()) {
                ApplistDialogs.questionDialog(
                        getActivity(),
                        getResources().getString(R.string.settings_keep_apps_sorted_alphabetically),
                        getResources().getString(R.string.settings_keep_apps_sorted_dialog_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                Task.callInBackground(new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        mApplistModel.sortStartables();
                                        return null;
                                    }
                                });
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                SwitchPreference keepAppsSorted = (SwitchPreference) findPreference(
                                        PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY);
                                keepAppsSorted.setChecked(false);
                            }
                        }
                );
            }
        }

        private void handleChangeColorTheme() {
            Preference preference = findPreference(PREF_KEY_COLOR_THEME);
            preference.setSummary(getColorTheme());
            restartMainActivity();
        }

        private void handleChangeColumnWidth() {
            Preference preference = findPreference(PREF_KEY_COLUMN_WIDTH);
            preference.setSummary(getColumnWidth());
            restartMainActivity();
        }

        private void handleChangeShowSmsBadge() {
        }

        private void handleChangeShowPhoneBadge() {
            if (getShowPhoneBadge()) {
                if (ensurePermission(Manifest.permission.READ_PHONE_STATE, PHONE_PERMISSION_REQUEST_CODE)) {
                    ensureDefaultPhoneApp();
                }
            }
        }

        private void handleChangeShowNotificationBadge() {
            if (getShowNotificationBadge()) {
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.settings_show_notification_badge_dialog_title)
                        .setMessage(R.string.settings_show_notification_badge_dialog_message)
                        .setPositiveButton(R.string.settings_open_system_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final String intentAction = (Build.VERSION.SDK_INT >= 22)
                                        ? Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                        : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
                                startActivity(new Intent(intentAction));
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SwitchPreference showNotificationBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_NOTIFICATION_BADGE);
                                showNotificationBadge.setChecked(false);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                SwitchPreference showNotificationBadge = (SwitchPreference) findPreference(PREF_KEY_SHOW_NOTIFICATION_BADGE);
                                showNotificationBadge.setChecked(false);
                            }
                        })
                        .setCancelable(true)
                        .create();
                alertDialog.show();
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
                mLauncherUtils.chooseAppDialog(
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

        // REF: 2017_06_22_22_36_launchMode_needed_for_restart
        private void restartMainActivity() {
            Intent i = new Intent(getActivity(), MainActivity.class);
            i.setAction(MainActivity.ACTION_RESTART);
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

        private String getColumnWidth() {
            String columnWidthValue = getPreferenceScreen().getSharedPreferences().getString(PREF_KEY_COLUMN_WIDTH, mDefaultColumnWidthValue);
            String[] columnWidthValues = getResources().getStringArray(R.array.column_width_values);
            String[] columnWidths = getResources().getStringArray(R.array.column_widths);
            for (int i = 0; i < columnWidthValues.length; ++i) {
                if (columnWidthValues[i].equals(columnWidthValue)) {
                    return columnWidths[i];
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

        private boolean getShowNotificationBadge() {
            return getPreferenceScreen().getSharedPreferences().getBoolean(
                    PREF_KEY_SHOW_NOTIFICATION_BADGE, false);
        }

        private boolean getKeepAppsSortedAlphabetically() {
            return getPreferenceScreen().getSharedPreferences().getBoolean(
                    PREF_KEY_KEEP_APPS_SORTED_ALPHABETICALLY, false);
        }

    }

}
