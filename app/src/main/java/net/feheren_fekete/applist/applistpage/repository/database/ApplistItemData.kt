package net.feheren_fekete.applist.applistpage.repository.database

import android.content.Intent
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ApplistItemData(@PrimaryKey(autoGenerate = true) val id: Long,
                           val lastModifiedTimestamp: Long,
                           val type: Int,
                           val position: Int,
                           val packageName: String,
                           val className: String,
                           val name: String,
                           val customName: String,
                           val appVersionCode: Long,
                           val shortcutIntent: String,
                           val appShortcutId: String,
                           val parentSectionId: Long,
                           val sectionIsCollapsed: Boolean) {

    fun getDisplayName(): String {
        return if (customName.isNotEmpty()) {
            customName
        } else {
            name
        }
    }

    companion object {

        const val INVALID_ID: Long = -1
        const val DEFAULT_SECTION_ID: Long = 1

        const val TYPE_SECTION = 1
        const val TYPE_APP = 2
        const val TYPE_SHORTCUT = 3
        const val TYPE_APP_SHORTCUT = 4

        fun createSection(id: Long,
                          name: String): ApplistItemData {
            return ApplistItemData(
                    id,
                    0,
                    TYPE_SECTION,
                    0,
                    "",
                    "",
                    name,
                    name,
                    0,
                    "",
                    "",
                    INVALID_ID,
                    false
            )
        }

        fun createApp(id: Long,
                      packageName: String,
                      className: String,
                      versionCode: Long,
                      appName: String,
                      customName: String): ApplistItemData {
            return ApplistItemData(
                    id,
                    0,
                    TYPE_APP,
                    0,
                    packageName,
                    className,
                    appName,
                    customName,
                    versionCode,
                    "",
                    "",
                    DEFAULT_SECTION_ID,
                    false
            )
        }

        fun createShortcut(id: Long,
                           packageName: String,
                           name: String,
                           customName: String,
                           intent: Intent): ApplistItemData {
            return ApplistItemData(
                    id,
                    0,
                    TYPE_SHORTCUT,
                    0,
                    packageName,
                    "",
                    name,
                    customName,
                    0,
                    intent.toUri(0),
                    "",
                    DEFAULT_SECTION_ID,
                    false
            )
        }

        fun createAppShortcut(id: Long,
                              name: String,
                              customName: String,
                              packageName: String,
                              shortcutId: String): ApplistItemData {
            return ApplistItemData(
                    id,
                    0,
                    TYPE_SHORTCUT,
                    0,
                    packageName,
                    "",
                    name,
                    customName,
                    0,
                    "",
                    shortcutId,
                    DEFAULT_SECTION_ID,
                    false
            )
        }

        fun update(item: ApplistItemData, fromItem: ApplistItemData): ApplistItemData {
            return ApplistItemData(
                    item.id,
                    System.currentTimeMillis(),
                    fromItem.type,
                    item.position,
                    fromItem.packageName,
                    fromItem.className,
                    fromItem.name,
                    item.customName,
                    fromItem.appVersionCode,
                    fromItem.shortcutIntent,
                    fromItem.appShortcutId,
                    item.parentSectionId,
                    item.sectionIsCollapsed
            )
        }

    }

}
