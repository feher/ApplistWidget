package net.feheren_fekete.applist.applistpage.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.repository.database.ApplistIconStorage
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData
import net.feheren_fekete.applist.applistpage.repository.database.ApplistPageDao
import net.feheren_fekete.applist.applistpage.repository.database.MigrateJsonToRoom
import net.feheren_fekete.applist.applistpage.viewmodel.*
import net.feheren_fekete.applist.utils.AppUtils
import org.koin.java.KoinJavaComponent.inject

class ApplistPageRepository(val context: Context,
                            val applistPageDao: ApplistPageDao) {

    private val applistLog: ApplistLog by inject(ApplistLog::class.java)
    private val iconStorage: ApplistIconStorage by inject(ApplistIconStorage::class.java)

    init {
        GlobalScope.launch(Dispatchers.IO) {
            val migrateJsonToRoom = MigrateJsonToRoom(context, applistPageDao)
            if (migrateJsonToRoom.migratePages()) {
                applistLog.analytics(
                        ApplistLog.MIGRATE_APPLIST,
                        ApplistLog.APPLIST_PAGE_REPOSITORY)
            }
            updateInstalledApps(context)
        }
    }

    inner class ApplistItemsLiveData: MediatorLiveData<List<BaseItem>>() {
        private val allItemsRawLiveData: LiveData<List<ApplistItemData>>
        init {
            allItemsRawLiveData = applistPageDao.getAllItems()
            addSource(allItemsRawLiveData) { items ->
                val result = ArrayList<BaseItem>()
                val sortedItems = items.sortedBy {
                    it.position
                }
                for (item in sortedItems) {
                    val baseItem = when (item.type) {
                        ApplistItemData.TYPE_SECTION -> SectionItem(
                                item.id,
                                item.name,
                                item.id != ApplistItemData.DEFAULT_SECTION_ID,
                                item.sectionIsCollapsed)
                        ApplistItemData.TYPE_APP -> AppItem(
                                item.id,
                                item.packageName,
                                item.className,
                                item.appVersionCode,
                                item.name,
                                item.customName,
                                iconStorage.getCustomAppIconFilePath(item.packageName, item.className),
                                item.parentSectionId)
                        ApplistItemData.TYPE_SHORTCUT -> {
                            val intent = Intent.parseUri(item.shortcutIntent, 0)
                            var packageName = intent.getPackage()
                            if (packageName == null && intent.component != null) {
                                packageName = intent.component!!.packageName
                            }
                            if (packageName == null) {
                                ApplistLog.getInstance().log(RuntimeException("Missing package name: " + intent.toUri(0)))
                                null
                            } else {
                                ShortcutItem(
                                        item.id,
                                        item.name,
                                        item.customName,
                                        iconStorage.getCustomShortcutIconFilePath(item.id),
                                        intent,
                                        iconStorage.getShortcutIconFilePath(item.id),
                                        item.parentSectionId)
                            }
                        }
                        ApplistItemData.TYPE_APP_SHORTCUT -> AppShortcutItem(
                                item.id,
                                item.name,
                                item.customName,
                                iconStorage.getCustomShortcutIconFilePath(item.id),
                                item.packageName,
                                item.appShortcutId,
                                iconStorage.getShortcutIconFilePath(item.id),
                                item.parentSectionId)
                        else -> {
                            ApplistLog.getInstance().log(RuntimeException(""))
                            null
                        }
                    }
                    if (baseItem != null) {
                        result.add(baseItem)
                    }
                }
                postValue(result)
            }
        }
    }

    fun getItems(): LiveData<List<BaseItem>> {
        val result = ApplistItemsLiveData()
        return result
    }

    suspend fun updateInstalledApps(context: Context) {
        val installedApps = AppUtils.getInstalledApps(context).sortedBy {
            it.getDisplayName().toLowerCase()
        }
        val items = applistPageDao.getAllItemsSync().sortedBy {
            it.position
        }
        val updatedItems = ArrayList<ApplistItemData>()

        for (item in items) {
            if (item.type == ApplistItemData.TYPE_SECTION) {
                updatedItems.add(item)
            } else if (item.type == ApplistItemData.TYPE_APP) {
                val installedApp = installedApps.find {
                    it.packageName == item.packageName
                            && it.className == item.className
                }
                if (installedApp == null) {
                    removeIcons(item)
                } else {
                    updatedItems.add(ApplistItemData.update(item, installedApp))
                }
            } else if (item.type == ApplistItemData.TYPE_SHORTCUT
                    || item.type == ApplistItemData.TYPE_APP_SHORTCUT) {
                val installedApp = installedApps.find {
                    it.packageName == item.packageName
                }
                if (installedApp == null) {
                    removeIcons(item)
                }
            }
        }

        // Create default section if missing
        var defaultSectionPos = updatedItems.indexOfFirst {
            it.id == ApplistItemData.DEFAULT_SECTION_ID
        }
        if (defaultSectionPos == -1) {
            updatedItems.add(ApplistItemData.createSection(
                    ApplistItemData.DEFAULT_SECTION_ID,
                    context.getString(R.string.uncategorized_group)))
            defaultSectionPos = updatedItems.size - 1
        }

        // Add new apps to the top of the default section
        for (installedApp in installedApps.asReversed()) {
            val item = updatedItems.find {
                it.type == ApplistItemData.TYPE_APP
                        && it.packageName == installedApp.packageName
                        && it.className == installedApp.className
            }
            if (item == null) {
                updatedItems.add(defaultSectionPos + 1, installedApp)
            }
        }

        // Re-number
        updatedItems.forEachIndexed { index, item ->
            item.position = index
        }

        applistPageDao.replaceItems(updatedItems)
    }

    suspend fun forEachAppItem(action: suspend (ApplistItemData) -> Unit) {
        val items = applistPageDao.getItemsByTypesSync(arrayOf(ApplistItemData.TYPE_APP))
        for (item in items) {
            action(item)
        }
    }

    suspend fun transaction(action: suspend () -> Unit) {
        applistPageDao.transcation(action)
    }

    suspend fun removeAllIcons() {
        applistPageDao.transcation {
            val items = applistPageDao.getItemsByTypesSync(arrayOf(
                    ApplistItemData.TYPE_APP, ApplistItemData.TYPE_SHORTCUT, ApplistItemData.TYPE_APP_SHORTCUT))
            for (item in items) {
                removeIcons(item)
                applistPageDao.updateTimestamp(item.id)
            }

        }
    }

    private fun removeIcons(item: ApplistItemData) {
        if (item.type == ApplistItemData.TYPE_APP) {
            iconStorage.deleteCustomStartableIcon(
                    iconStorage.getCustomAppIconFilePath(
                            item.packageName, item.className))
        } else if (item.type == ApplistItemData.TYPE_APP_SHORTCUT) {
            iconStorage.deleteCustomStartableIcon(
                    iconStorage.getCustomShortcutIconFilePath(item.id))
            iconStorage.deleteShortcutIcon(item.id)
        } else if (item.type == ApplistItemData.TYPE_SHORTCUT) {
            iconStorage.deleteCustomStartableIcon(
                    iconStorage.getCustomShortcutIconFilePath(item.id))
            iconStorage.deleteShortcutIcon(item.id)
        } else {
            applistLog.log(IllegalStateException())
        }
    }

    suspend fun setSectionCollapsed(sectionId: Long, collapsed: Boolean) {
        applistPageDao.updateSectionCollapsed(sectionId, collapsed)
    }

    suspend fun setAllSectionsCollapsed(collapsed: Boolean) {
        applistPageDao.updateAllSectionsCollapsed(collapsed)
    }

    suspend fun getSections(): List<Pair<Long, String>> {
        val result = ArrayList<Pair<Long, String>>()
        val sections =
                applistPageDao.getItemsByTypesSync(arrayOf(ApplistItemData.TYPE_SECTION)).sortedBy {
                    it.position
                }
        for (section in sections) {
            result.add(Pair(section.id, section.name))
        }
        return result
    }

    suspend fun updateItemPositionsAndParentSectionIds(orderedItemIds: List<Long>,
                                                       parentSectionIds: List<Long>) {
        applistPageDao.transcation {
            if (orderedItemIds.size != parentSectionIds.size) {
                throw RuntimeException("Array sizes don't match")
            }
            for (i in orderedItemIds.indices) {
                applistPageDao.updatePosition(orderedItemIds[i], i)
                applistPageDao.updateParentSectionId(orderedItemIds[i], parentSectionIds[i])
            }
        }
    }

    suspend fun updateSectionPositions(orderedSectionIds: Array<Long>) {
        applistPageDao.transcation {
            var position = 0
            for (sectionId in orderedSectionIds) {
                applistPageDao.updatePosition(sectionId, position)
                position += 1

                val sectionItems = applistPageDao
                        .getItemsBySectionSync(sectionId).sortedBy {
                    it.position
                }
                for (sectionItem in sectionItems) {
                    applistPageDao.updatePosition(sectionItem.id, position)
                    position += 1
                }
            }
        }
    }

    suspend fun sortSection(sectionId: Long) {
        applistPageDao.transcation {
            val section = applistPageDao.getItemById(sectionId) ?: return@transcation
            val items = applistPageDao.getItemsBySectionSync(sectionId).sortedBy {
                it.getDisplayName().toLowerCase()
            }
            items.forEachIndexed { index, item ->
                applistPageDao.updatePosition(item.id, section.position + index)
            }
        }
    }

    suspend fun moveStartablesToSection(startableIds: List<Long>, sectionId: Long, append: Boolean) {
        applistPageDao.transcation {
            val items = applistPageDao.getAllItemsSync()
                    .sortedBy {
                        it.position
                    }
                    .toMutableList()
            val movedItems = ArrayList<ApplistItemData>()
            var sectionItem: ApplistItemData? = null

            for (item in items) {
                if (startableIds.contains(item.id)) {
                    movedItems.add(item)
                    item.parentSectionId = sectionId
                }
                if (item.id == sectionId) {
                    sectionItem = item
                }
            }

            if (sectionItem == null) {
                return@transcation
            }

            // Remove the items from their old places
            items.removeAll(movedItems)

            // Move the items to their new places
            var sectionIndex = -1
            var lastSectionItemIndex = -1
            for (i in items.indices) {
                val item = items[i]
                if (item.type == ApplistItemData.TYPE_SECTION) {
                    if (sectionIndex == -1) {
                        if (item.id == sectionId) {
                            sectionIndex = i
                        }
                    } else {
                        lastSectionItemIndex = i - 1
                        break
                    }
                }
            }
            if (sectionIndex == -1) {
                throw IllegalStateException()
            }
            if (lastSectionItemIndex == -1) {
                lastSectionItemIndex = items.size - 1
            }
            if (append) {
                items.addAll(lastSectionItemIndex + 1, movedItems)
            } else {
                items.addAll(sectionIndex + 1, movedItems)
            }

            // Update item positions and parent section IDs
            val orderedItemIds = ArrayList<Long>()
            val parentSectionIds = ArrayList<Long>()
            for (item in items) {
                orderedItemIds.add(item.id)
                parentSectionIds.add(item.parentSectionId)
            }
            updateItemPositionsAndParentSectionIds(orderedItemIds, parentSectionIds)
        }
    }

    suspend fun setStartableCustomName(startableId: Long, customName: String) {
        applistPageDao.updateCustomName(startableId, customName)
    }

    suspend fun setSectionName(sectionId: Long, sectionName: String) {
        applistPageDao.updateName(sectionId, sectionName)
    }

    suspend fun removeSection(sectionId: Long) {
        applistPageDao.transcation {
            val sectionItems = applistPageDao.getItemsBySectionSync(sectionId)
            val sectionItemIds = ArrayList<Long>()
            for (item in sectionItems) {
                sectionItemIds.add(item.id)
            }
            moveStartablesToSection(sectionItemIds, ApplistItemData.DEFAULT_SECTION_ID, false)
            applistPageDao.delItem(sectionId)
        }
    }

    suspend fun addNewSection(sectionName: String): Long {
        var sectionId: Long = ApplistItemData.INVALID_ID
        applistPageDao.transcation {
            // We add the new section to the top, so we shift everything
            // down one position.
            val allItems = applistPageDao.getAllItemsSync()
            for (item in allItems) {
                applistPageDao.updatePosition(item.id, item.position + 1)
            }

            val section = ApplistItemData.createSection(0, sectionName)
            section.position = 0
            sectionId = applistPageDao.addItem(section)
        }
        return sectionId
    }

    suspend fun hasAppShortcut(packageName: String, shortcutId: String): Boolean {
        val count = applistPageDao.getAppShortcutCount(packageName, shortcutId)
        return (count > 0)
    }

    suspend fun storeCustomStartableIcon(itemId: Long, iconPath: String, icon: Bitmap, notify: Boolean) {
        iconStorage.storeCustomStartableIcon(iconPath, icon)
        if (notify) {
            applistPageDao.updateTimestamp(itemId, System.currentTimeMillis())
        }
    }

    suspend fun deleteCustomStartableIcon(itemId: Long, iconPath: String) {
        iconStorage.deleteCustomStartableIcon(iconPath)
        applistPageDao.updateTimestamp(itemId, System.currentTimeMillis())
    }

    suspend fun addShortcut(item: ApplistItemData, shortcutIcon: Bitmap) {
        iconStorage.storeShortcutIcon(item.id, shortcutIcon)
        applistPageDao.addItem(item)
    }

    suspend fun removeShortcut(shortcutId: Long) {
        val shortcut = applistPageDao.getItemById(shortcutId)
        if (shortcut != null) {
            removeIcons(shortcut)
        }
        applistPageDao.delItem(shortcutId)
    }

    fun getCustomAppIconPath(item: ApplistItemData): String {
        return iconStorage.getCustomAppIconFilePath(
                item.packageName, item.className)
    }

}