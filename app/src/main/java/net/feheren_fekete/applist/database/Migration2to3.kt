package net.feheren_fekete.applist.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemDataMigrate2to3

class Migration2to3: Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        ApplistItemDataMigrate2to3().migrate(database)
    }
}
