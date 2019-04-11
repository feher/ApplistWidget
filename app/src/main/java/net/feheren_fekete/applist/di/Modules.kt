package net.feheren_fekete.applist.di

import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import net.feheren_fekete.applist.utils.WriteSettingsPermissionHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val applistModule = module {
    single { IconPackHelper() }
    single { WriteSettingsPermissionHelper(androidContext()) }
}