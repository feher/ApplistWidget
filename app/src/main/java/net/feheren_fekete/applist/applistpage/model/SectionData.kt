package net.feheren_fekete.applist.applistpage.model

class SectionData(id: Long,
                  var name: String,
                  startables: List<StartableData>,
                  val isRemovable: Boolean,
                  var isCollapsed: Boolean) : BaseData(id) {

    private val startables = mutableListOf<StartableData>().apply {
        addAll(startables)
    }

    fun isEmpty() = startables.isEmpty()

    fun getStartables() = startables

    fun setStartables(startableDatas: List<StartableData>) {
        startables.clear()
        startables.addAll(startableDatas)
    }

    fun addStartables(index: Int, startableDatas: List<StartableData>) {
        startables.addAll(index, startableDatas)
    }

    fun addStartable(startableData: StartableData) {
        startables.add(startableData)
    }

    fun getStartable(startableId: Long) =
            startables.find {
                it.id == startableId
            }

    fun hasStartable(startableId: Long) =
            getStartable(startableId) != null

    fun hasStartable(startableData: StartableData) =
            startables.contains(startableData)

    fun removeStartable(startableId: Long): Boolean {
        val startable = startables.find {
            it.id == startableId
        }
        return if (startable != null) {
            startables.remove(startable)
            true
        } else {
            false
        }
    }

    fun sortStartablesAlphabetically() {
        startables.sortWith(StartableData.NameComparator())
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is SectionData) {
            return false
        }
        return name == other.name
    }

    class NameComparator : Comparator<SectionData> {
        override fun compare(lhs: SectionData, rhs: SectionData): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

}
