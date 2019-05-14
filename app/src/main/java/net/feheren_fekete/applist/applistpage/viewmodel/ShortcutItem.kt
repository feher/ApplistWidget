package net.feheren_fekete.applist.applistpage.viewmodel

import android.content.Intent

class ShortcutItem(id: Long,
                   name: String,
                   customName: String,
                   val intent: Intent,
                   iconPath: String) : StartableItem(id, name, customName, iconPath)
