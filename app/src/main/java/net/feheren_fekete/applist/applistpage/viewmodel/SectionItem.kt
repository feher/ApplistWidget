package net.feheren_fekete.applist.applistpage.viewmodel


class SectionItem(id: Long,
                  name: String,
                  val isRemovable: Boolean,
                  val isCollapsed: Boolean) : BaseItem(id, name) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SectionItem) {
            return false
        }
        return name == other.name
    }
}
