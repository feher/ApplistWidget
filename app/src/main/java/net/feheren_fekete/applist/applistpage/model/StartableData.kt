package net.feheren_fekete.applist.applistpage.model

import java.util.Comparator

abstract class StartableData(id: Long,
                             val packageName: String,
                             var name: String,
                             var customName: String) : BaseData(id) {

    fun displayName() = if (customName.isNotEmpty()) customName else name

    class NameComparator : Comparator<StartableData> {
        override fun compare(lhs: StartableData, rhs: StartableData): Int {
            return lhs.displayName().toLowerCase().compareTo(rhs.displayName().toLowerCase())
        }
    }

}
