package net.feheren_fekete.applist.applistpage.viewmodel


class AppItem(id: Long,
              val packageName: String,
              val className: String,
              name: String,
              customName: String,
              iconPath: String) : StartableItem(id, name, customName, iconPath) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppItem) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
