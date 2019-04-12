package net.feheren_fekete.applist.applistpage.model

import java.util.Comparator

abstract class StartableData(id: Long,
                             val packageName: String,
                             var name: String,
                             var customName: String) : BaseData(id) {

    class NameComparator : Comparator<StartableData> {
        override fun compare(lhs: StartableData, rhs: StartableData): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

}
