package net.feheren_fekete.applist.applistpage.viewmodel

class AppShortcutItem(id: Long,
                      name: String,
                      customName: String,
                      customIconPath: String,
                      val packageName: String,
                      val shortcutId: String,
                      val iconPath: String,
                      parentSectionId: Long) : StartableItem(id, name, customName, customIconPath, parentSectionId)

