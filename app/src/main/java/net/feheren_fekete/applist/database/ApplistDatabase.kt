package net.feheren_fekete.applist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData
import net.feheren_fekete.applist.applistpage.repository.database.ApplistPageDao
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageDao
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData

@Database(entities = arrayOf(ApplistItemData::class, LauncherPageData::class), exportSchema = false, version = 3)
abstract class ApplistDatabase: RoomDatabase() {

    abstract fun applistPageDao(): ApplistPageDao

    abstract fun pageDao(): LauncherPageDao

}
