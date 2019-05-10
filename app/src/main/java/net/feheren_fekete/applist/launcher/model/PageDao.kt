package net.feheren_fekete.applist.launcher.model

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface PageDao {

    @Query("SELECT * FROM pagedata ORDER BY position")
    fun pages() : LiveData<List<PageData>>

    @Query("SELECT * FROM pagedata ORDER BY position")
    fun getPages() : List<PageData>

    @Query("DELETE FROM pagedata")
    suspend fun deleteAllPages()

    @Transaction
    suspend fun init() {
        // Always have one applist page.
        val applistPage = getApplistPageInternal()
        if (applistPage == null) {
            addPage(PageData.TYPE_APPLIST_PAGE)
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
        addPageInternal(PageData(0, type, false, position))
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
    suspend fun replacePages(pages: List<PageData>) {
        deleteAllPages()
        addPagesInternal(pages)
    }

    @Insert(onConflict = REPLACE)
    suspend fun addPageInternal(page: PageData)

    @Insert(onConflict = REPLACE)
    suspend fun addPagesInternal(pages: List<PageData>)

    @Delete
    suspend fun delPageInternal(page: PageData)

    @Query("UPDATE pagedata SET position = :position WHERE id = :id")
    suspend fun setPositionInternal(id: Long, position: Int)

    @Query("UPDATE pagedata SET isMainPage = :isMainPage WHERE id = :id")
    suspend fun setMainPageInternal(id: Long, isMainPage: Boolean)

    @Query("SELECT * from pagedata WHERE isMainPage = 1 ORDER BY position LIMIT 1")
    suspend fun getMainPageInternal(): PageData?

    @Query("SELECT * from pagedata WHERE type = ${PageData.TYPE_APPLIST_PAGE} ORDER BY position LIMIT 1")
    suspend fun getApplistPageInternal(): PageData?

    @Query("SELECT * from pagedata WHERE isMainPage = 0 ORDER BY position LIMIT 1")
    suspend fun getNotMainPageInternal(): PageData?

    @Query("SELECT * FROM pagedata WHERE id = :id")
    suspend fun getPage(id: Long): PageData?

    @Query("SELECT position FROM pagedata ORDER BY position DESC LIMIT 1")
    suspend fun getLastPagePositionInternal(): Int?

}
