package net.feheren_fekete.applist.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.SettingsUtils;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = LauncherActivity.class.getSimpleName();

    public static final String ACTION_RESTART =
            LauncherActivity.class.getCanonicalName()+ "ACTION_RESTART";

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mPager = (ViewPager) findViewById(R.id.main_view_pager);
        mPagerAdapter = new LauncherPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
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

}
