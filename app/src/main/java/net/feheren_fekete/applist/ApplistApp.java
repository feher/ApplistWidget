package net.feheren_fekete.applist;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.launcher.LauncherStateManager;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;


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
        LauncherStateManager.initInstance();
        ScreenUtils.initInstance();
        ScreenshotUtils.initInstance();
        SettingsUtils.initInstance(this);
        LauncherModel.initInstance(this);
        ApplistModel.initInstance(this, getPackageManager());
        WidgetModel.initInstance(this);

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

