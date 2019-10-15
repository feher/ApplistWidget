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
import net.feheren_fekete.applist.applistpage.viewmodel.*
import net.feheren_fekete.applist.utils.AppUtils
import org.koin.java.KoinJavaComponent.inject

class ApplistPageRepository(val context: Context,
                            val applistPageDao: ApplistPageDao) {

    private val applistLog: ApplistLog by inject(ApplistLog::class.java)
    private val iconStorage: ApplistIconStorage by inject(ApplistIconStorage::class.java)

    init {
        GlobalScope.launch(Dispatchers.IO) {
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
        val installedApps = AppUtils.getInstalledApps(context)
        val items = applistPageDao.getAllItemsSync()
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

        for (installedApp in installedApps) {
            val item = updatedItems.find {
                it.type == ApplistItemData.TYPE_APP
                        && it.packageName == installedApp.packageName
                        && it.className == installedApp.className
            }
            if (item == null) {
                updatedItems.add(installedApp)
            }
        }

        val defaultSection = updatedItems.find {
            it.id == ApplistItemData.DEFAULT_SECTION_ID
        }
        if (defaultSection == null) {
            updatedItems.add(ApplistItemData.createSection(
                    ApplistItemData.DEFAULT_SECTION_ID,
                    context.getString(R.string.uncategorized_group)))
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
                    it.name.toLowerCase()
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

    suspend fun moveStartableToSection(startableId: Long, newSectionId: Long) {
        applistPageDao.transcation {
            val item = applistPageDao.getItemById(startableId)
            if (item == null) {
                return@transcation
            }

            // In the old section, move items up by one position that come after our item
            val oldSectionItems = applistPageDao.getItemsBySectionSync(item.parentSectionId)
            for (oldSectionItem in oldSectionItems) {
                if (oldSectionItem.position > item.position) {
                    applistPageDao.updatePosition(oldSectionItem.id, oldSectionItem.position - 1)
                }
            }

            // Put our item at the end of the new section
            val newSectionItems = applistPageDao.getItemsBySectionSync(newSectionId)
            val newPosition = newSectionItems.size
            applistPageDao.updateParentSectionId(startableId, newSectionId)
            applistPageDao.updatePosition(startableId, newPosition)
        }
    }

    suspend fun moveStartablesToSection(startableIds: Array<Long>, sectionId: Long) {
        applistPageDao.transcation {
            for (itemId in startableIds) {
                moveStartableToSection(itemId, sectionId)
            }
        }
    }

    suspend fun moveStartableToPosition(startableId: Long, newPosition: Int) {
        applistPageDao.transcation {
            val item = applistPageDao.getItemById(startableId)
            if (item == null) {
                applistLog.log(RuntimeException("Item not found with ID " + startableId))
                return@transcation
            }

            val oldPosition = item.position
            val sectionItems = applistPageDao.getItemsBySectionSync(item.parentSectionId)

            if (newPosition < 0 || newPosition >= sectionItems.size) {
                applistLog.log(RuntimeException(
                        "Bad newPosition: ${newPosition} < 0 || ${newPosition} >= ${sectionItems.size}"))
                return@transcation
            }

            if (newPosition < oldPosition) {
                // In the section, move items down by one position that are in [newPos, oldPos)
                for (sectionItem in sectionItems) {
                    if (newPosition <= sectionItem.position
                            && sectionItem.position < oldPosition) {
                        applistPageDao.updatePosition(sectionItem.id, sectionItem.position + 1)
                    }
                }
            } else {
                // In the section, move items up by one position that are in (oldPos, newPos]
                for (sectionItem in sectionItems) {
                    if (oldPosition < sectionItem.position
                            && sectionItem.position <= newPosition) {
                        applistPageDao.updatePosition(sectionItem.id, sectionItem.position - 1)
                    }
                }
            }

            // Put the item into position
            applistPageDao.updatePosition(item.id, newPosition)
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
            applistPageDao.resetParentSectionIds(sectionId)
            applistPageDao.delItem(sectionId)
        }
    }

    suspend fun addNewSection(sectionName: String): Long {
        return applistPageDao.addItem(ApplistItemData.createSection(0, sectionName))
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