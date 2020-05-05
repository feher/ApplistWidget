package net.feheren_fekete.applist.applistpage.repository.database

import android.content.Context
import android.content.Intent
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import org.koin.java.KoinJavaComponent.get
import java.io.File
import java.net.URISyntaxException

class MigrateJsonToRoom(context: Context,
                        private val applistPageDao: ApplistPageDao) {

    private val fileUtils = get(FileUtils::class.java)

    private val pagesFilePath = context.filesDir.absolutePath + File.separator + "applist-pages-v2.json"
    private var itemCount = 0;

    suspend fun migratePages(): Boolean {
        val file = File(pagesFilePath)
        if (!file.exists()) {
            return false
        }

        val fileContent = fileUtils.readFile(pagesFilePath)
        try {
            val jsonObject = JSONObject(fileContent)

            val jsonPages = jsonObject.getJSONArray(JSON_PAGES)
            for (i in 0 until jsonPages.length()) {
                val jsonPage = jsonPages.getJSONObject(i)
                migratePage(jsonPage)
            }
        } catch (e: JSONException) {
        }

        File(pagesFilePath).delete()
        return true
    }

    @Throws(JSONException::class)
    private suspend fun migratePage(jsonPage: JSONObject) {
        val jsonSections = jsonPage.getJSONArray(JSON_SECTIONS)
        for (j in 0 until jsonSections.length()) {
            val jsonSection = jsonSections.getJSONObject(j)
            migrateSection(jsonSection)
        }
    }

    @Throws(JSONException::class)
    private suspend fun migrateSection(jsonSection: JSONObject) {
        val sectionId = if (jsonSection.getBoolean(JSON_SECTION_IS_REMOVABLE)) {
            0L
        } else {
            ApplistItemData.DEFAULT_SECTION_ID
        }
        val sectionItem = ApplistItemData.createSection(
                sectionId, jsonSection.getString(JSON_SECTION_NAME))
        sectionItem.position = itemCount
        val finalSectionId = applistPageDao.addItem(sectionItem)
        itemCount += 1

        val jsonStartables = jsonSection.getJSONArray(JSON_STARTABLES)
        for (k in 0 until jsonStartables.length()) {
            val jsonStartable = jsonStartables.getJSONObject(k)
            migrateStartable(jsonStartable, finalSectionId)
        }
    }

    @Throws(JSONException::class)
    private suspend fun migrateStartable(jsonStartable: JSONObject, parentSectionId: Long) {
        val type = jsonStartable.getString(JSON_STARTABLE_TYPE)
        val item = if (JSON_STARTABLE_TYPE_APP == type) {
            ApplistItemData(
                    id = 0,
                    lastModifiedTimestamp = 0,
                    type = ApplistItemData.TYPE_APP,
                    position = itemCount,
                    packageName = jsonStartable.getString(JSON_APP_PACKAGE_NAME),
                    className = jsonStartable.getString(JSON_APP_CLASS_NAME),
                    name = jsonStartable.getString(JSON_STARTABLE_NAME),
                    customName = jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME),
                    appVersionCode = 0,
                    shortcutIntent = "",
                    appShortcutId = "",
                    parentSectionId = parentSectionId,
                    sectionIsCollapsed = false)
        } else if (type == JSON_STARTABLE_TYPE_SHORTCUT) {
            try {
                val intent = Intent.parseUri(jsonStartable.getString(JSON_SHORTCUT_INTENT), 0)
                var packageName = intent.getPackage()
                if (packageName == null && intent.component != null) {
                    packageName = intent.component!!.packageName
                }
                if (packageName == null) {
                    throw JSONException("Missing package name: " + intent.toUri(0))
                }
                ApplistItemData(
                        id = 0,
                        lastModifiedTimestamp = 0,
                        type = ApplistItemData.TYPE_SHORTCUT,
                        position = itemCount,
                        packageName = packageName,
                        className = "",
                        name = jsonStartable.getString(JSON_STARTABLE_NAME),
                        customName = jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME),
                        appVersionCode = 0,
                        shortcutIntent = intent.toUri(0),
                        appShortcutId = "",
                        parentSectionId = parentSectionId,
                        sectionIsCollapsed = false)
            } catch (e: URISyntaxException) {
                ApplistLog.getInstance().log(e)
                throw JSONException(e.message)
            }

        } else if (type == JSON_STARTABLE_TYPE_APP_SHORTCUT) {
            ApplistItemData(
                    id = 0,
                    lastModifiedTimestamp = 0,
                    type = ApplistItemData.TYPE_SHORTCUT,
                    position = itemCount,
                    packageName = jsonStartable.getString(JSON_APP_PACKAGE_NAME),
                    className = "",
                    name = jsonStartable.getString(JSON_STARTABLE_NAME),
                    customName = jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME),
                    appVersionCode = 0,
                    shortcutIntent = "",
                    appShortcutId = jsonStartable.getString(JSON_APP_SHORTCUT_ID),
                    parentSectionId = parentSectionId,
                    sectionIsCollapsed = false)
        } else {
            throw RuntimeException("Unknown type startable $type")
        }
        applistPageDao.addItem(item)
        itemCount += 1
    }

    companion object {

        private val JSON_PAGES = "pages"

        private val JSON_SECTIONS = "sections"
        private val JSON_SECTION_NAME = "name"

        private val JSON_SECTION_IS_REMOVABLE = "is-removable"

        private val JSON_STARTABLES = "startables"
        private val JSON_STARTABLE_TYPE = "type"
        private val JSON_STARTABLE_TYPE_APP = "app"
        private val JSON_STARTABLE_TYPE_SHORTCUT = "shortcut"
        private val JSON_STARTABLE_TYPE_APP_SHORTCUT = "app-shortcut"
        private val JSON_STARTABLE_NAME = "name"
        private val JSON_STARTABLE_CUSTOM_NAME = "custom-name"
        private val JSON_APP_PACKAGE_NAME = "package-name"
        private val JSON_APP_CLASS_NAME = "class-name"
        private val JSON_SHORTCUT_INTENT = "intent"
        private val JSON_APP_SHORTCUT_ID = "shortcut-id"
    }

}
