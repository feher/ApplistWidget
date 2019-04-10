package net.feheren_fekete.applist.applistpage.viewmodel


class AppItem(id: Long,
              val packageName: String,
              val className: String,
              private val mAppName: String) : StartableItem(id) {

    override fun getName(): String {
        return mAppName
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppItem) {
            return false
        }
        return packageName == other.packageName && className == other.className
    }
}
