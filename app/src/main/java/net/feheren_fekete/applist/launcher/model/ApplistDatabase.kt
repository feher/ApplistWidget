package net.feheren_fekete.applist.launcher.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(PageData::class), version = 1)
abstract class ApplistDatabase: RoomDatabase() {

    abstract fun pageDao(): PageDao

}
