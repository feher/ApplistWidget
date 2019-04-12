package net.feheren_fekete.applist.applistpage.viewmodel

class AppShortcutItem(id: Long,
                      name: String,
                      customName: String,
                      val packageName: String,
                      val shortcutId: String,
                      val iconPath: String) : StartableItem(id, name, customName)

