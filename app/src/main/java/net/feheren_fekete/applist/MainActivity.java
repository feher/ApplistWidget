package net.feheren_fekete.applist;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import net.feheren_fekete.applist.applistpage.ApplistPageFragment;
import net.feheren_fekete.applist.applistpage.ShortcutHelper;
import net.feheren_fekete.applist.launcher.LauncherFragment;
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorFragment;
import net.feheren_fekete.applist.launcher.pagepicker.PagePickerFragment;
import net.feheren_fekete.applist.widgetpage.WidgetHelper;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;
import net.feheren_fekete.applist.widgetpage.MyAppWidgetHost;
import net.feheren_fekete.applist.settings.SettingsUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static org.koin.java.KoinJavaComponent.get;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_RESTART =
            MainActivity.class.getCanonicalName()+ "ACTION_RESTART";

    private ShortcutHelper mShortcutHelper = get(ShortcutHelper.class);
    private WidgetHelper mWidgetHelper = get(WidgetHelper.class);
    private SettingsUtils mSettingsUtils = get(SettingsUtils.class);

    private MyAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mIsHomePressed;
    private boolean mShouldHandleIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        mSettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        mAppWidgetHost = new MyAppWidgetHost(getApplicationContext(), 1234567);

        showLauncherFragment(-1);
        mShouldHandleIntent = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mShouldHandleIntent = true;
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // This occurs when the Home button is pressed.
            // Be careful! It may not be true in the future or on some devices.
            mIsHomePressed = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWidgetHelper.handleActivityResult(requestCode, resultCode, data, mAppWidgetHost);
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
        if (mShouldHandleIntent) {
            mShouldHandleIntent = false;
            handleIntent(getIntent());
        }
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
    public void onShowPagePickerEvent(WidgetHelper.ShowPagePickerEvent event) {
        showPagePickerFragment(
                getResources().getString(R.string.page_picker_pin_widget_title),
                getResources().getString(R.string.page_picker_message),
                event.data);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowPagePickerEvent(WidgetPageFragment.ShowPagePickerEvent event) {
        showPagePickerFragment(
                getResources().getString(R.string.page_picker_move_widget_title),
                getResources().getString(R.string.page_picker_message),
                event.data);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageEditorDoneEvent(PageEditorFragment.DoneEvent event) {
        showLauncherFragment(-1);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageEditorPageTappedEvent(PageEditorFragment.PageTappedEvent event) {
        if (mWidgetHelper.handlePagePicked(this, event.pageData, event.requestData)) {
            showLauncherFragment(event.pageData.getId());
        }
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

    private void handleIntent(@Nullable Intent intent) {
        boolean handled;
        if (intent != null) {
            handled = mShortcutHelper.handleIntent(this, intent);
            if (!handled) {
                handled = mWidgetHelper.handleIntent(this, intent, mAppWidgetManager, mAppWidgetHost);
            }
            if (!handled) {
                if (ACTION_RESTART.equals(intent.getAction())) {
                    Intent restartIntent = new Intent(this, MainActivity.class);
                    restartIntent.setAction(Intent.ACTION_MAIN);
                    restartIntent.addCategory(Intent.CATEGORY_HOME);
                    setIntent(restartIntent);
                    recreate();
                }
            }
        }
    }

    private void showLauncherFragment(long activePageId) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container, LauncherFragment.newInstance(activePageId))
                .commit();
    }

    private void showPageEditorFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container,
                        PageEditorFragment.newInstance(true, false, new Bundle()))
                .commit();
    }

    private void showPagePickerFragment(String title,
                                        String message,
                                        Bundle data) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container,
                        PagePickerFragment.newInstance(title, message, data))
                .commit();
    }

}
