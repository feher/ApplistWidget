package net.feheren_fekete.applist.applistpage.viewmodel

abstract class StartableItem(id: Long,
                             name: String,
                             val customName: String,
                             val customIconPath: String,
                             var parentSectionId: Long) : BaseItem(id, name) {

    fun getDisplayName(): String {
        return if (customName.isNotEmpty()) {
            customName
        } else {
            name
        }
    }
}
