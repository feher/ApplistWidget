package net.feheren_fekete.applist;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import net.feheren_fekete.applist.applistpage.ApplistPageFragment;
import net.feheren_fekete.applist.applistpage.ShortcutHelper;
import net.feheren_fekete.applist.launcher.LauncherFragment;
import net.feheren_fekete.applist.launcher.PageEditorFragment;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;
import net.feheren_fekete.applist.widgetpage.MyAppWidgetHost;
import net.feheren_fekete.applist.settings.SettingsUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_RESTART =
            MainActivity.class.getCanonicalName()+ "ACTION_RESTART";

    // TODO: Inject these singletons.
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();

    private MyAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mIsHomePressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        mSettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        mAppWidgetHost = new MyAppWidgetHost(getApplicationContext(), 1234567);

        showLauncherFragment();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean handled = handleIntent(intent);
        if (!handled) {
            if (ACTION_RESTART.equals(intent.getAction())) {
                finish();
                startActivity(intent);
            } else {
                // This occurs when the Home button is pressed.
                // Be careful! It may not be true in the future or on some devices.
                mIsHomePressed = true;
            }
        }
    }

    private boolean handleIntent(@Nullable Intent intent) {
        boolean handled = false;
        if (intent != null) {
            handled = new ShortcutHelper(this).handleIntent(intent);
        }
        return handled;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAppWidgetHost.stopListening();
    }

    @Override
    public void onBackPressed() {
        // Don't exit on back-press. We are a launcher.
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowPageEditorEvent(WidgetPageFragment.ShowPageEditorEvent event) {
        showPageEditorFragment();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowPageEditorEvent(ApplistPageFragment.ShowPageEditorEvent event) {
        showPageEditorFragment();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageEditorDoneEvent(PageEditorFragment.DoneEvent event) {
        showLauncherFragment();
    }

    public boolean isHomePressed() {
        boolean wasHomePressed = mIsHomePressed;
        mIsHomePressed = false;
        return wasHomePressed;
    }

    public MyAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public AppWidgetManager getAppWidgetManager() {
        return mAppWidgetManager;
    }

    private void showLauncherFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container, new LauncherFragment())
                .commit();
    }

    private void showPageEditorFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container, new PageEditorFragment())
                .commit();
    }

}
