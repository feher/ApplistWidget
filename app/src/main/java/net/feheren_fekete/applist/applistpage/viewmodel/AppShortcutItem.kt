package net.feheren_fekete.applist.applistpage.viewmodel

class AppShortcutItem(id: Long,
                      private val name: String,
                      val packageName: String,
                      val shortcutId: String,
                      val iconPath: String) : StartableItem(id) {

    override fun getName(): String {
        return name
    }

}
