package net.feheren_fekete.applist.di

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import com.google.firebase.analytics.FirebaseAnalytics
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.applistpage.IconPreloadHelper
import net.feheren_fekete.applist.applistpage.ShortcutHelper
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import net.feheren_fekete.applist.applistpage.model.ApplistModel
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.launcher.LauncherStateManager
import net.feheren_fekete.applist.launcher.LauncherUtils
import net.feheren_fekete.applist.launcher.ScreenshotUtils
import net.feheren_fekete.applist.launcher.model.LauncherModel
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.FileUtils
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
    single { ApplistLog() }
    single { FileUtils() }
    single { ScreenUtils() }
    single { WidgetUtils() }
    single { LauncherUtils() }
    single { ScreenshotUtils() }
    single { ShortcutHelper() }
    single { WidgetHelper() }
    single { IconPackHelper() }
    single { IconPreloadHelper() }
    single { SettingsUtils(androidContext()) }
    single { WriteSettingsPermissionHelper(androidContext()) }
    single { BadgeUtils(androidContext()) }
    single { BadgeStore(androidContext(), get()) }
    single { ApplistModel(androidContext(), get()) }
    single { LauncherModel(androidContext()) }
    single { WidgetModel(androidContext()) }
    single { LauncherStateManager() }
    single { ApplistPreferences(androidContext()) }
}
