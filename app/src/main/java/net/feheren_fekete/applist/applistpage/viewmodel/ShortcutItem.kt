package net.feheren_fekete.applist.applistpage.viewmodel

import android.content.Intent

class ShortcutItem(id: Long,
                   name: String,
                   customName: String,
                   customIconPath: String,
                   val intent: Intent,
                   val iconPath: String) : StartableItem(id, name, customName, customIconPath)
