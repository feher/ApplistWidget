package net.feheren_fekete.applist.applistpage.model

class AppData(id: Long,
              packageName: String,
              val className: String,
              appName: String,
              customName: String) : StartableData(id, packageName, appName, customName) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppData) {
            return false
        }
        return packageName == other.packageName && className == other.className
    }

}
