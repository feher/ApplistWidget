package net.feheren_fekete.applist.di

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.applistpage.IconPreloadHelper
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPackIconsRepository
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPacksRepository
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPacksCache
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import net.feheren_fekete.applist.applistpage.repository.database.ApplistIconStorage
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.database.ApplistDatabase
import net.feheren_fekete.applist.database.Migration1to2
import net.feheren_fekete.applist.database.Migration2to3
import net.feheren_fekete.applist.launcher.LauncherStateManager
import net.feheren_fekete.applist.launcher.LauncherUtils
import net.feheren_fekete.applist.launcher.ScreenshotUtils
import net.feheren_fekete.applist.launcher.repository.LauncherRepository
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.FileUtils
import net.feheren_fekete.applist.utils.ImageUtils
import net.feheren_fekete.applist.utils.ScreenUtils
import net.feheren_fekete.applist.utils.WriteSettingsPermissionHelper
import net.feheren_fekete.applist.widgetpage.MyAppWidgetHost
import net.feheren_fekete.applist.widgetpage.WidgetHelper
import net.feheren_fekete.applist.widgetpage.WidgetUtils
import net.feheren_fekete.applist.widgetpage.model.WidgetModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val applistModule = module {
    single { androidContext().packageManager }
    single { FirebaseAnalytics.getInstance(androidContext()) }
    single { AppWidgetManager.getInstance(androidContext()) }
    single { MyAppWidgetHost(androidContext(), 1234567) as AppWidgetHost }
    single {
        Room.databaseBuilder(
                androidContext(),
                ApplistDatabase::class.java,
                "applist-db")
                .addMigrations(
                        Migration1to2(),
                        Migration2to3())
                .build()
    }
    single { get<ApplistDatabase>().applistPageDao() }
    single { get<ApplistDatabase>().pageDao() }
    single { ApplistLog() }
    single { ImageUtils() }
    single { FileUtils() }
    single { ScreenUtils() }
    single { WidgetUtils() }
    single { LauncherUtils() }
    single { ScreenshotUtils() }
    single { ShortcutHelper() }
    single { WidgetHelper() }
    single { IconPackHelper(androidContext(), get(), get(), get()) }
    single { IconPacksCache(androidContext(), get()) }
    single { IconPacksRepository(get(), get()) }
    single { IconPackIconsRepository(get(), get()) }
    single { IconPreloadHelper() }
    single { SettingsUtils(androidContext()) }
    single { WriteSettingsPermissionHelper(androidContext()) }
    single { BadgeUtils(androidContext()) }
    single { BadgeStore(androidContext(), get()) }
    single { ApplistIconStorage(androidContext(), get()) }
    single { ApplistPageRepository(androidContext(), get()) }
    single { LauncherRepository(androidContext(), get(), get()) }
    single { WidgetModel(androidContext()) }
    single { LauncherStateManager() }
    single { ApplistPreferences(androidContext()) }
}
