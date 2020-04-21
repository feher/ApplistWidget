package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName

data class IconPackIcon(
    val rank: Int,
    val drawableName: String,
    val componentNames: List<ComponentName>
)
