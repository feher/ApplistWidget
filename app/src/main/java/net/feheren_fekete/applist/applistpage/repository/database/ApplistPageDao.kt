package net.feheren_fekete.applist.applistpage.repository.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ApplistPageDao {

    @Transaction
    fun init() {
    }

    @Transaction
    suspend fun transcation(action: suspend () -> Unit) {
        action()
    }

    @Transaction
    suspend fun replaceItems(items: List<ApplistItemData>) {
        delItems()
        addItems(items)
    }

    @Query("SELECT COUNT(*) FROM ApplistItemData")
    suspend fun getItemCount(): Long

    @Query("SELECT COUNT(*) FROM ApplistItemData WHERE type = ${ApplistItemData.TYPE_APP_SHORTCUT} AND packageName = :packageName AND appShortcutId = :appShortcutId")
    suspend fun getAppShortcutCount(packageName: String, appShortcutId: String): Long

    @Query("SELECT * FROM ApplistItemData")
    fun getAllItems(): LiveData<List<ApplistItemData>>

    @Query("SELECT * FROM ApplistItemData")
    suspend fun getAllItemsSync(): List<ApplistItemData>

    @Query("SELECT * FROM ApplistItemData WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): ApplistItemData?

    @Query("SELECT * FROM ApplistItemData WHERE type = :type ORDER BY customName ASC")
    fun getItemsByType(type: Int): LiveData<List<ApplistItemData>>

    @Query("SELECT * FROM ApplistItemData WHERE type in (:types) ORDER BY customName ASC")
    suspend fun getItemsByTypesSync(types: Array<Int>): List<ApplistItemData>

    @Query("SELECT * FROM ApplistItemData WHERE parentSectionId = :sectionId ORDER BY customName ASC")
    fun getItemsBySection(sectionId: Long): LiveData<List<ApplistItemData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(applistItem: ApplistItemData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItems(applistItems: List<ApplistItemData>)

    @Delete
    suspend fun delItem(applistItem: ApplistItemData)

    @Query("DELETE FROM ApplistItemData WHERE id = :itemId")
    suspend fun delItem(itemId: Long)

    @Query("DELETE FROM ApplistItemData")
    suspend fun delItems()

    @Query("UPDATE ApplistItemData SET lastModifiedTimestamp = :timestamp WHERE id = :itemId")
    suspend fun updateTimestamp(itemId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ApplistItemData SET parentSectionId = :parentSectionId WHERE id = :itemId")
    suspend fun updateParentSectionId(itemId: Long, parentSectionId: Long)

    @Query("UPDATE ApplistItemData SET sectionIsCollapsed = :collapsed WHERE id = :itemId")
    suspend fun updateSectionCollapsed(itemId: Long, collapsed: Boolean)

    @Query("UPDATE ApplistItemData SET customName = :customName WHERE id = :itemId")
    suspend fun updateCustomName(itemId: Long, customName: String)

    @Query("UPDATE ApplistItemData SET name = :name WHERE id = :itemId")
    suspend fun updateName(itemId: Long, name: String)

    @Query("UPDATE ApplistItemData SET parentSectionId = ${ApplistItemData.DEFAULT_SECTION_ID} WHERE type != ${ApplistItemData.TYPE_SECTION} AND parentSectionId = :parentSectionId")
    suspend fun resetParentSectionIds(parentSectionId: Long)

}
