package net.feheren_fekete.applist;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import net.feheren_fekete.applist.model.DataModel;


public class ApplistApp extends MultiDexApplication {

    private static final String TAG = ApplistApp.class.getSimpleName();

    private static int[] mIconPlaceholderColors;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        ApplistLog.initInstance();
        DataModel.initInstance(this, getPackageManager());

        mIconPlaceholderColors = new int[6];
        mIconPlaceholderColors[0] = 0xffcdbbbb;
        mIconPlaceholderColors[1] = 0xffc6c5b1;
        mIconPlaceholderColors[2] = 0xff9eb5a1;
        mIconPlaceholderColors[3] = 0xff8688a1;
        mIconPlaceholderColors[4] = 0xff957b98;
        mIconPlaceholderColors[5] = 0xff937577;
    }

    public int[] getIconPlaceholderColors() {
        return mIconPlaceholderColors;
    }

}

