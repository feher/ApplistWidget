package net.feheren_fekete.applist.applistpage.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
                    it.getDisplayName().toLowerCase()
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
                        it.getDisplayName().toLowerCase()
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
                                    iconStorage.getCustomAppIconFilePath(sectionItem.packageName, sectionItem.className))
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
                                            iconStorage.getShortcutIconFilePath(sectionItem.id))
                                }
                            }
                            ApplistItemData.TYPE_APP_SHORTCUT -> AppShortcutItem(
                                    sectionItem.id,
                                    sectionItem.name,
                                    sectionItem.customName,
                                    iconStorage.getCustomShortcutIconFilePath(sectionItem.id),
                                    sectionItem.packageName,
                                    sectionItem.appShortcutId,
                                    iconStorage.getShortcutIconFilePath(sectionItem.id))
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
                            && item.className == item.className
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

    suspend fun moveStartableToSection(startableId: Long, sectionId: Long) {
        applistPageDao.updateParentSectionId(startableId, sectionId)
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