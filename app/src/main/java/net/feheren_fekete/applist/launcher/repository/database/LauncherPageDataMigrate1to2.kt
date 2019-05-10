package net.feheren_fekete.applist.launcher.repository.database

import androidx.sqlite.db.SupportSQLiteDatabase

class LauncherPageDataMigrate1to2 {

    fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE pagedata RENAME TO launcherpagedata")
    }

}
