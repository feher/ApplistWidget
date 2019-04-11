package net.feheren_fekete.applist

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.di.applistModule
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import net.feheren_fekete.applist.widgetpage.model.WidgetModel
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class ApplistApp : MultiDexApplication() {

    private val shortcutHelper: ShortcutHelper by inject()

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
        WidgetHelper.initInstance()
        shortcutHelper.registerInstallShortcutReceiver(this)
    }

    companion object {
        private val TAG = ApplistApp::class.java.simpleName
    }

}

