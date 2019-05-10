package net.feheren_fekete.applist.launcher.model

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.utils.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

class LauncherModel(context: Context,
                    val pageDao: PageDao,
                    val fileUtils: FileUtils) {

    private val pagesFilePath: String

    val pages: LiveData<List<PageData>>
        get() = pageDao.pages()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            pageDao.init()
        }

        // Remove this when all users have migrated
        pagesFilePath = context.filesDir.absolutePath + File.separator + "applist-launcher-pages.json"
        migrateLegacyData()
    }

    fun swapPagePositions(aId: Long, bId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            pageDao.swapPositions(aId, bId)
        }
    }

    fun addPage(type: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            pageDao.addPage(type)
        }
    }

    fun removePage(page: PageData) {
        GlobalScope.launch(Dispatchers.IO) {
            pageDao.removePage(page.id)
        }
    }

    fun setMainPage(page: PageData) {
        GlobalScope.launch(Dispatchers.IO) {
            pageDao.setMainPage(page.id)
        }
    }

    private fun migrateLegacyData() {
        GlobalScope.launch(Dispatchers.IO) {
            val legacyPages = loadLegacyData()
            if (legacyPages.isNotEmpty()) {
                ApplistLog.getInstance().analytics(
                        ApplistLog.MIGRATE_LAUNCHER_PAGES,
                        ApplistLog.LAUNCHER_PAGE_REPOSITORY)
                pageDao.replacePages(legacyPages)
                File(pagesFilePath).delete()
            }
        }
    }

    private fun loadLegacyData(): List<PageData> {
        val pages = ArrayList<PageData>()
        val fileContent = fileUtils.readFile(pagesFilePath)
        if (!TextUtils.isEmpty(fileContent)) {
            try {
                val jsonObject = JSONObject(fileContent)

                val jsonPages = jsonObject.getJSONArray("pages")
                for (k in 0 until jsonPages.length()) {
                    val jsonPage = jsonPages.getJSONObject(k)
                    val pageData = PageData(
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
