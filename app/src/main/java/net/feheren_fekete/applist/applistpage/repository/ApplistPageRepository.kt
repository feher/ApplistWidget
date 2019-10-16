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
                val allItemsStructured = ArrayList<BaseItem>()
                val sections = items.filter {
                    it.type == ApplistItemData.TYPE_SECTION
                }.sortedBy {
                    it.position
                }
                for (section in sections) {
                    allItemsStructured.add(SectionItem(
                            section.id,
                            section.name,
                            section.id != ApplistItemData.DEFAULT_SECTION_ID,
                            section.sectionIsCollapsed))
                    val sectionItems = items.filter {
                        it.parentSectionId == section.id
                    }.sortedBy {
                        it.position
                    }
                    for (sectionItem in sectionItems) {
                        val baseItem = when (sectionItem.type) {
                            ApplistItemData.TYPE_APP -> AppItem(
                                    sectionItem.id,
                                    sectionItem.packageName,
                                    sectionItem.className,
                                    sectionItem.appVersionCode,
                                    sectionItem.name,
                                    sectionItem.customName,
                                    iconStorage.getCustomAppIconFilePath(sectionItem.packageName, sectionItem.className),
                                    sectionItem.parentSectionId)
                            ApplistItemData.TYPE_SHORTCUT -> {
                                val intent = Intent.parseUri(sectionItem.shortcutIntent, 0)
                                var packageName = intent.getPackage()
                                if (packageName == null && intent.component != null) {
                                    packageName = intent.component!!.packageName
                                }
                                if (packageName == null) {
                                    ApplistLog.getInstance().log(RuntimeException("Missing package name: " + intent.toUri(0)))
                                    null
                                } else {
                                    ShortcutItem(
                                            sectionItem.id,
                                            sectionItem.name,
                                            sectionItem.customName,
                                            iconStorage.getCustomShortcutIconFilePath(sectionItem.id),
                                            intent,
                                            iconStorage.getShortcutIconFilePath(sectionItem.id),
                                            sectionItem.parentSectionId)
                                }
                            }
                            ApplistItemData.TYPE_APP_SHORTCUT -> AppShortcutItem(
                                    sectionItem.id,
                                    sectionItem.name,
                                    sectionItem.customName,
                                    iconStorage.getCustomShortcutIconFilePath(sectionItem.id),
                                    sectionItem.packageName,
                                    sectionItem.appShortcutId,
                                    iconStorage.getShortcutIconFilePath(sectionItem.id),
                                    sectionItem.parentSectionId)
                            else -> {
                                ApplistLog.getInstance().log(RuntimeException(""))
                                null
                            }
                        }
                        if (baseItem != null) {
                            allItemsStructured.add(baseItem)
                        }
                    }
                }
                postValue(allItemsStructured)
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

    suspend fun updateItemPositionsAndParentSectionIds(orderedItemIds: Array<Long>,
                                                       parentSectionIds: Array<Long>) {
        applistPageDao.transcation {
            if (orderedItemIds.size != parentSectionIds.size) {
                throw RuntimeException("Array sizes don't match")
            }
            for (i in 0..orderedItemIds.size - 1) {
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

    suspend fun moveStartableToSection(startableId: Long, newSectionId: Long, append: Boolean) {
        applistPageDao.transcation {
            val item = applistPageDao.getItemById(startableId)
            if (item == null) {
                return@transcation
            }

            if (item.parentSectionId == newSectionId) {
                return@transcation
            }

            val newSection = applistPageDao.getItemById(newSectionId)
            if (newSection == null) {
                return@transcation
            }

            if (append) {
                val newSectionItemCount =
                        applistPageDao.getItemsBySectionSync(newSectionId).size
                moveStartableToPosition(startableId, newSection.position + newSectionItemCount + 1)
            } else {
                moveStartableToPosition(startableId, newSection.position + 1)
            }
        }
    }

    suspend fun moveStartablesToSection(startableIds: Array<Long>, sectionId: Long) {
        applistPageDao.transcation {
            for (itemId in startableIds) {
                moveStartableToSection(itemId, sectionId, true)
            }
        }
    }

    suspend fun moveStartableToPosition(startableId: Long, newPosition: Int) {
        applistPageDao.transcation {
            Log.d("ZIZI", "TRANS ${startableId} ${newPosition}")
            val item = applistPageDao.getItemById(startableId)
            if (item == null) {
                applistLog.log(RuntimeException("Item not found with ID " + startableId))
                return@transcation
            }

            Log.d("ZIZI", "ITEM OK pos = ${item.position}")

            val allItems = applistPageDao.getAllItemsSync().sortedBy {
                it.position
            }

            if (newPosition < 0 || newPosition > allItems.size) {
                applistLog.log(RuntimeException(
                        "Bad newPosition: ${newPosition} < 0 || ${newPosition} > ${allItems.size}"))
                return@transcation
            }

            Log.d("ZIZI", "NEW POS OK")

            // Determine the new parent section for the item
            val leftNeighborPos = newPosition - 1
            val rightNeighborPos = newPosition
            val newParentSectionId = if (leftNeighborPos >= 0) {
                val leftNeighbor = allItems[leftNeighborPos]
                if (leftNeighbor.type != ApplistItemData.TYPE_SECTION) {
                    leftNeighbor.parentSectionId
                } else {
                    leftNeighbor.id
                }
            } else if (rightNeighborPos < allItems.size) {
                val rightNeighbor = allItems[rightNeighborPos]
                if (rightNeighbor.type != ApplistItemData.TYPE_SECTION) {
                    rightNeighbor.parentSectionId
                } else {
                    ApplistItemData.INVALID_ID
                }
            } else {
                ApplistItemData.INVALID_ID
            }
            Log.d("ZIZI", "NEW PAR ${newParentSectionId}")
            if (newParentSectionId == ApplistItemData.INVALID_ID) {
                applistLog.log(RuntimeException("Cannot determine new parent section ID"))
                return@transcation
            }

            // Reposition the other items
            val oldPosition = item.position
            var updatedNewPosition = 0
            if (newPosition < oldPosition) {
                updatedNewPosition = newPosition
                // Move items down by one position that are in [newPos, oldPos)
                for (otherItem in allItems) {
                    if (updatedNewPosition <= otherItem.position
                            && otherItem.position < oldPosition) {
                        applistPageDao.updatePosition(otherItem.id, otherItem.position + 1)
                    }
                }
            } else {
                updatedNewPosition = newPosition - 1
                // Move items up by one position that are in (oldPos, newPos]
                for (otherItem in allItems) {
                    if (oldPosition < otherItem.position
                            && otherItem.position <= updatedNewPosition) {
                        applistPageDao.updatePosition(otherItem.id, otherItem.position - 1)
                    }
                }
            }

            // Put the item into position
            applistPageDao.updatePosition(item.id, updatedNewPosition)
            applistPageDao.updateParentSectionId(item.id, newParentSectionId)
        }
    }

    suspend fun moveSectionToPosition(sectionId: Long, newPosition: Int) {
        applistPageDao.transcation {
            val section = applistPageDao.getItemById(sectionId)
            if (section == null) {
                applistLog.log(RuntimeException("Section not found with ID " + sectionId))
                return@transcation
            }

            val oldPosition = section.position
            val sections = applistPageDao.getItemsByTypesSync(arrayOf(ApplistItemData.TYPE_SECTION))

            if (newPosition < 0 || newPosition >= sections.size) {
                applistLog.log(RuntimeException(
                        "Bad newPosition: ${newPosition} < 0 || ${newPosition} >= ${sections.size}"))
                return@transcation
            }

            if (newPosition < oldPosition) {
                // Move sections down by one position that are in [newPos, oldPos)
                for (sec in sections) {
                    if (newPosition <= sec.position && sec.position < oldPosition) {
                        applistPageDao.updatePosition(sec.id, sec.position + 1)
                    }
                }
            } else {
                // Move sections up by one position that are in (oldPos, newPos]
                for (sec in sections) {
                    if (oldPosition < sec.position && sec.position <= newPosition) {
                        applistPageDao.updatePosition(sec.id, sec.position - 1)
                    }
                }
            }

            // Put the item into position
            applistPageDao.updatePosition(section.id, newPosition)
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
            for (item in sectionItems) {
                moveStartableToSection(item.id, ApplistItemData.DEFAULT_SECTION_ID, false)
            }
            applistPageDao.delItem(sectionId)
        }
    }

    suspend fun addNewSection(sectionName: String): Long {
        var sectionId: Long = ApplistItemData.INVALID_ID
        applistPageDao.transcation {
            // Add the new section above the default section
            val defaultSection = applistPageDao.getItemById(ApplistItemData.DEFAULT_SECTION_ID)
            if (defaultSection == null) {
                return@transcation
            }
            val allItems = applistPageDao.getAllItemsSync()
            for (item in allItems) {
                if (defaultSection.position <= item.position) {
                    applistPageDao.updatePosition(item.id, item.position + 1)
                }
            }
            val section = ApplistItemData.createSection(0, sectionName)
            section.position = defaultSection.position
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