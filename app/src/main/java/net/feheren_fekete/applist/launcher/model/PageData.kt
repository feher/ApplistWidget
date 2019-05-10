package net.feheren_fekete.applist.launcher.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PageData(@PrimaryKey(autoGenerate = true) val id: Long,
                    val type: Int,
                    val isMainPage: Boolean,
                    val position: Int) {

    companion object {

        const val INVALID_PAGE_ID: Long = -1

        const val TYPE_APPLIST_PAGE = 1
        const val TYPE_WIDGET_PAGE = 2
    }

}
