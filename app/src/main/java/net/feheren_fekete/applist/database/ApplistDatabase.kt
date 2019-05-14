package net.feheren_fekete.applist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageDao
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData

@Database(entities = arrayOf(LauncherPageData::class), exportSchema = false, version = 2)
abstract class ApplistDatabase: RoomDatabase() {

    abstract fun pageDao(): LauncherPageDao

}
