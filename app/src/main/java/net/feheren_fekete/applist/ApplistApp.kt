package net.feheren_fekete.applist

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.di.applistModule
import net.feheren_fekete.applist.launcher.LauncherStateManager
import net.feheren_fekete.applist.launcher.model.LauncherModel
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import net.feheren_fekete.applist.widgetpage.model.WidgetModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class ApplistApp : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())

        startKoin {
            androidContext(this@ApplistApp)
            modules(applistModule)
        }

        ApplistLog.initInstance()
        LauncherStateManager.initInstance()
        LauncherModel.initInstance(this)
        WidgetModel.initInstance(this)
        WidgetHelper.initInstance()
        ShortcutHelper.initInstance()
        ShortcutHelper.getInstance().registerInstallShortcutReceiver(this)
    }

    companion object {
        private val TAG = ApplistApp::class.java.simpleName
    }

}

