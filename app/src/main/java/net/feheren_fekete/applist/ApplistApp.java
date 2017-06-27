package net.feheren_fekete.applist;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import net.feheren_fekete.applist.applistpage.ShortcutHelper;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.launcher.LauncherStateManager;
import net.feheren_fekete.applist.launcher.LauncherUtils;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;


public class ApplistApp extends MultiDexApplication {

    private static final String TAG = ApplistApp.class.getSimpleName();

    private ShortcutHelper mShortcutHelper;

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
        LauncherUtils.initInstance();
        SettingsUtils.initInstance(this);
        LauncherModel.initInstance(this);
        ApplistModel.initInstance(this, getPackageManager());
        WidgetModel.initInstance(this);

        mShortcutHelper = new ShortcutHelper(this);
        mShortcutHelper.registerInstallShortcutReceiver();
    }

}

