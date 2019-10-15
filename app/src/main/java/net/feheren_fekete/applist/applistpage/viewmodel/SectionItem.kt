package net.feheren_fekete.applist.applistpage.viewmodel


class SectionItem(id: Long,
                  name: String,
                  val isRemovable: Boolean,
                  var isCollapsed: Boolean) : BaseItem(id, name) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SectionItem) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
