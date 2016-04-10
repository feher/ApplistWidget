package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import net.feheren_fekete.applistwidget.model.DataModel;


public class ApplistApp extends MultiDexApplication {

    private static final String TAG = ApplistApp.class.getSimpleName();

    private static int[] mThemeColors;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        DataModel.initInstance(this, getPackageManager());

        mThemeColors = new int[6];
        mThemeColors[0] = 0xffcdbbbb;
        mThemeColors[1] = 0xffc6c5b1;
        mThemeColors[2] = 0xff9eb5a1;
        mThemeColors[3] = 0xff8688a1;
        mThemeColors[4] = 0xff957b98;
        mThemeColors[5] = 0xff937577;
    }

    public int[] getThemeColors() {
        return mThemeColors;
    }

}

