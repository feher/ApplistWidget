package net.feheren_fekete.applist.launcher.repository.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface LauncherPageDao {

    @Query("SELECT * FROM launcherpagedata ORDER BY position")
    fun pages() : LiveData<List<LauncherPageData>>

    @Query("SELECT * FROM launcherpagedata ORDER BY position")
    fun getPages() : List<LauncherPageData>

    @Query("DELETE FROM launcherpagedata")
    suspend fun deleteAllPages()

    @Transaction
    suspend fun init() {
        // Always have one applist page.
        val applistPage = getApplistPageInternal()
        if (applistPage == null) {
            addPage(LauncherPageData.TYPE_APPLIST_PAGE)
        }

        // Always have one main page.
        val mainPage = getMainPageInternal()
        if (mainPage == null) {
            val applistPage = getApplistPageInternal()
            setMainPage(applistPage!!.id)
        }
    }

    @Transaction
    suspend fun addPage(type: Int) {
        val position = (getLastPagePositionInternal() ?: 1) + 1
        addPageInternal(LauncherPageData(0, type, false, position))
    }

    @Transaction
    suspend fun swapPositions(idA: Long, idB: Long) {
        val pageA = getPage(idA) ?: return
        val pageB = getPage(idB) ?: return
        setPositionInternal(idA, pageB.position)
        setPositionInternal(idB, pageA.position)
    }

    @Transaction
    suspend fun removePage(id: Long) {
        val page = getPage(id) ?: return

        delPageInternal(page)

        // Give out fresh position numbers
        val pages = getPages()
        pages.forEachIndexed { i, page ->
            setPositionInternal(page.id, i)
        }

        // Set a new main page
        if (page.isMainPage) {
            val notMainPage = getNotMainPageInternal()
            if (notMainPage != null) {
                setMainPageInternal(notMainPage.id, true)
            }
        }
    }

    @Transaction
    suspend fun setMainPage(id: Long) {
        val mainPage = getMainPageInternal()
        if (mainPage != null) {
            setMainPageInternal(mainPage.id, false)
        }
        setMainPageInternal(id, true)
    }

    @Transaction
    suspend fun replacePages(pages: List<LauncherPageData>) {
        deleteAllPages()
        addPagesInternal(pages)
    }

    @Insert(onConflict = REPLACE)
    suspend fun addPageInternal(page: LauncherPageData)

    @Insert(onConflict = REPLACE)
    suspend fun addPagesInternal(pages: List<LauncherPageData>)

    @Delete
    suspend fun delPageInternal(page: LauncherPageData)

    @Query("UPDATE launcherpagedata SET position = :position WHERE id = :id")
    suspend fun setPositionInternal(id: Long, position: Int)

    @Query("UPDATE launcherpagedata SET isMainPage = :isMainPage WHERE id = :id")
    suspend fun setMainPageInternal(id: Long, isMainPage: Boolean)

    @Query("SELECT * from launcherpagedata WHERE isMainPage = 1 ORDER BY position LIMIT 1")
    suspend fun getMainPageInternal(): LauncherPageData?

    @Query("SELECT * from launcherpagedata WHERE type = ${LauncherPageData.TYPE_APPLIST_PAGE} ORDER BY position LIMIT 1")
    suspend fun getApplistPageInternal(): LauncherPageData?

    @Query("SELECT * from launcherpagedata WHERE isMainPage = 0 ORDER BY position LIMIT 1")
    suspend fun getNotMainPageInternal(): LauncherPageData?

    @Query("SELECT * FROM launcherpagedata WHERE id = :id")
    suspend fun getPage(id: Long): LauncherPageData?

    @Query("SELECT position FROM launcherpagedata ORDER BY position DESC LIMIT 1")
    suspend fun getLastPagePositionInternal(): Int?

}
