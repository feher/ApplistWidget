package net.feheren_fekete.applist.applistpage.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData
import net.feheren_fekete.applist.utils.FileUtils
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class ApplistViewModel: ViewModel() {

    private val repository: ApplistPageRepository by inject(ApplistPageRepository::class.java)
    private val fileUtils: FileUtils by inject(FileUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

    fun getItems() = repository.getItems()

    fun setSectionCollapsed(sectionId: Long, collapsed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSectionCollapsed(sectionId, collapsed)
        }
    }

    fun updateItemPositionsAndParentSectionIds(orderedItemIds: List<Long>,
                                               parentSectionIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            repository.updateItemPositionsAndParentSectionIds(orderedItemIds, parentSectionIds)
        }
    }

    fun removeShortcut(shortcutId: Long) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            repository.removeShortcut(shortcutId)
        }
    }

    fun setStartableCustomName(startableId: Long, customName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setStartableCustomName(startableId, customName)
        }
    }

    fun setSectionName(sectionId: Long, sectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSectionName(sectionId, sectionName)
        }
    }

    fun removeSection(sectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeSection(sectionId)
        }
    }

    fun sortSection(sectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sortSection(sectionId)
        }
    }

    fun createSection(sectionName: String, appsToMove: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.transaction {
                val sectionId = repository.addNewSection(sectionName)
                if (sectionId != ApplistItemData.INVALID_ID && appsToMove.isNotEmpty()) {
                    repository.moveStartablesToSection(appsToMove, sectionId, true)
                }
            }
        }
    }

    fun setAllSectionsCollapsed(collapsed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAllSectionsCollapsed(collapsed)
        }
    }

    suspend fun getSections(): List<Pair<Long, String>> {
        return withContext(Dispatchers.IO) {
            repository.getSections()
        }
    }

    fun moveStartablesToSection(startableIds: List<Long>, sectionId: Long, append: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveStartablesToSection(startableIds, sectionId, append)
        }
    }

    fun updateData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Remove this after all users updated to 5.1
            // Remove unused iconCache
            fileUtils.deleteFiles(
                    fileUtils.getIconCacheDirPath(context.applicationContext),
                    "")

            repository.updateInstalledApps(context)
            badgeStore.cleanup()
        }
    }

    fun updateBadgesFromLauncher() {
        viewModelScope.launch(Dispatchers.Default) {
            badgeStore.updateBadgesFromLauncher()
        }
    }

}
