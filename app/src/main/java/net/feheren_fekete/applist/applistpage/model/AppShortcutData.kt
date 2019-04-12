package net.feheren_fekete.applist.applistpage.model

class AppShortcutData(id: Long,
                      name: String,
                      customName: String,
                      packageName: String,
                      val shortcutId: String) : StartableData(id, packageName, name, customName) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppShortcutData) {
            return false
        }
        return packageName == other.packageName && shortcutId == other.shortcutId
    }
}
