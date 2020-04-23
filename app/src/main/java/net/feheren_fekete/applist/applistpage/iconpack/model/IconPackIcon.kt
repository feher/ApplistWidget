package net.feheren_fekete.applist.applistpage.iconpack.model

import android.content.ComponentName

data class IconPackIcon(
    val rank: Int = 0,
    val drawableName: String = "",
    val componentNames: List<ComponentName> = emptyList()
) {

    fun isValid() = drawableName.isNotEmpty()

    fun isSameAs(other: IconPackIcon) = (drawableName == other.drawableName)

}
