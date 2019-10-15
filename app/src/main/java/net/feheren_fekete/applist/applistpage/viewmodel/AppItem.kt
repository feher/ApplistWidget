package net.feheren_fekete.applist.applistpage.viewmodel


class AppItem(id: Long,
              val packageName: String,
              val className: String,
              val versionCode: Long,
              name: String,
              customName: String,
              customIconPath: String,
              parentSectionId: Long) : StartableItem(id, name, customName, customIconPath, parentSectionId) {

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
