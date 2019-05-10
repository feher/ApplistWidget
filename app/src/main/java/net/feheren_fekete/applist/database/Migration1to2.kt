package net.feheren_fekete.applist.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageDataMigrate1to2

class Migration1to2: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        LauncherPageDataMigrate1to2().migrate(database)
    }
}
