package net.feheren_fekete.applist

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.di.applistModule
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class ApplistApp : MultiDexApplication() {

    private lateinit var shortcutHelper: ShortcutHelper

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())

        // Use EventBus index to avoid crashes
        // https://github.com/greenrobot/EventBus/issues/514
        // http://greenrobot.org/eventbus/documentation/subscriber-index/
        EventBus.builder().addIndex(ApplistEventBusIndex()).installDefaultEventBus()

        startKoin {
            androidContext(this@ApplistApp)
            modules(applistModule)
        }

        shortcutHelper = get<ShortcutHelper>()
        shortcutHelper.registerInstallShortcutReceiver(this)
    }

    companion object {
        private val TAG = ApplistApp::class.java.simpleName
    }

}

