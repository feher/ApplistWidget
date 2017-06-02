package net.feheren_fekete.applist;

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import net.feheren_fekete.applist.launcher.LauncherFragment;
import net.feheren_fekete.applist.launcherpage.MyAppWidgetHost;
import net.feheren_fekete.applist.settings.SettingsUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_RESTART =
            MainActivity.class.getCanonicalName()+ "ACTION_RESTART";

    private MyAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        mAppWidgetHost = new MyAppWidgetHost(getApplicationContext(), 1234567);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_activity_fragment_container, new LauncherFragment())
                .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_RESTART.equals(intent.getAction())) {
            finish();
            startActivity(intent);
        } else {
            //loadData();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        findViewById(R.id.main_activity_layout).setBackground(wallpaperDrawable);
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

    public MyAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public AppWidgetManager getAppWidgetManager() {
        return mAppWidgetManager;
    }

}
