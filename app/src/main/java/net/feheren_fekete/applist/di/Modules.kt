package net.feheren_fekete.applist.di

import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.WriteSettingsPermissionHelper
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val applistModule = module {
    single { androidContext().packageManager }
    single { SettingsUtils(androidContext()) }
    single { BadgeUtils(androidContext()) }
    single { BadgeStore(androidContext(), get()) }
    single { IconPackHelper() }
    single { WriteSettingsPermissionHelper(androidContext()) }
}
