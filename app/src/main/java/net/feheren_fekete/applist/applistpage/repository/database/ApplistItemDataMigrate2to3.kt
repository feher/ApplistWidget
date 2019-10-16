package net.feheren_fekete.applist.applistpage.repository.database

import androidx.sqlite.db.SupportSQLiteDatabase

class ApplistItemDataMigrate2to3 {

    fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS ApplistItemData (" +
                        " id INTEGER NOT NULL PRIMARY KEY, " +
                        " lastModifiedTimestamp INTEGER NOT NULL," +
                        " type INTEGER NOT NULL," +
                        " position INTEGER NOT NULL," +
                        " packageName TEXT NOT NULL," +
                        " className TEXT NOT NULL," +
                        " name TEXT NOT NULL," +
                        " customName TEXT NOT NULL," +
                        " appVersionCode INTEGER NOT NULL," +
                        " shortcutIntent TEXT NOT NULL," +
                        " appShortcutId TEXT NOT NULL," +
                        " parentSectionId INTEGER NOT NULL," +
                        " sectionIsCollapsed INTEGER NOT NULL" +
                        ")"
        )
    }

}
