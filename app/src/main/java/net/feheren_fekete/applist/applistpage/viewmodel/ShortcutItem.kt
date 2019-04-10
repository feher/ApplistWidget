package net.feheren_fekete.applist.applistpage.viewmodel

import android.content.Intent

class ShortcutItem(id: Long,
                   private val name: String,
                   val intent: Intent,
                   val iconPath: String) : StartableItem(id) {

    override fun getName(): String {
        return name
    }

}
