package net.feheren_fekete.applist.launcher.repository

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageDao
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData
import net.feheren_fekete.applist.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

class LauncherRepository(context: Context,
                         val launcherPageDao: LauncherPageDao,
                         val fileUtils: FileUtils) {

    private val pagesFilePath: String

    val pages: LiveData<List<LauncherPageData>>
        get() = launcherPageDao.pages()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            launcherPageDao.init()
        }

        // Remove this when all users have migrated
        pagesFilePath = context.filesDir.absolutePath + File.separator + "applist-launcher-pages.json"
        migrateLegacyData()
    }

    fun swapPagePositions(aId: Long, bId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            launcherPageDao.swapPositions(aId, bId)
        }
    }

    fun addPage(type: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            launcherPageDao.addPage(type)
        }
    }

    fun removePage(page: LauncherPageData) {
        GlobalScope.launch(Dispatchers.IO) {
            launcherPageDao.removePage(page.id)
        }
    }

    fun setMainPage(page: LauncherPageData) {
        GlobalScope.launch(Dispatchers.IO) {
            launcherPageDao.setMainPage(page.id)
        }
    }

    private fun migrateLegacyData() {
        GlobalScope.launch(Dispatchers.IO) {
            val legacyPages = loadLegacyData()
            if (legacyPages.isNotEmpty()) {
                ApplistLog.getInstance().analytics(
                        ApplistLog.MIGRATE_LAUNCHER_PAGES,
                        ApplistLog.LAUNCHER_PAGE_REPOSITORY)
                launcherPageDao.replacePages(legacyPages)
                File(pagesFilePath).delete()
            }
        }
    }

    private fun loadLegacyData(): List<LauncherPageData> {
        val pages = ArrayList<LauncherPageData>()
        val fileContent = fileUtils.readFile(pagesFilePath)
        if (!TextUtils.isEmpty(fileContent)) {
            try {
                val jsonObject = JSONObject(fileContent)

                val jsonPages = jsonObject.getJSONArray("pages")
                for (k in 0 until jsonPages.length()) {
                    val jsonPage = jsonPages.getJSONObject(k)
                    val pageData = LauncherPageData(
                            jsonPage.getLong("id"),
                            jsonPage.getInt("type"),
                            jsonPage.getBoolean("is-main-page"),
                            k)
                    pages.add(pageData)
                }
            } catch (e: JSONException) {
                ApplistLog.getInstance().log(e)
            }
        }
        return pages
    }

}
