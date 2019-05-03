package net.feheren_fekete.applist.applistpage.viewmodel

import net.feheren_fekete.applist.applistpage.model.*

import java.util.ArrayList
import java.util.Collections

object ViewModelUtils {

    fun modelToView(applistModel: ApplistModel,
                    badgeStore: BadgeStore,
                    pageData: PageData): List<BaseItem> {
        val result = ArrayList<BaseItem>()
        for (sectionData in pageData.sections) {
            result.add(SectionItem(
                    sectionData.id,
                    sectionData.name,
                    sectionData.isRemovable,
                    sectionData.isCollapsed))
            for (startableData in sectionData.getStartables()) {
                if (startableData is AppData) {
                    result.add(AppItem(
                            startableData.id,
                            startableData.packageName,
                            startableData.className,
                            startableData.versionCode,
                            startableData.name,
                            startableData.customName,
                            badgeStore.getBadgeCount(startableData.packageName, startableData.className)))
                } else if (startableData is ShortcutData) {
                    result.add(ShortcutItem(
                            startableData.id,
                            startableData.name,
                            startableData.customName,
                            startableData.intent,
                            applistModel.getShortcutIconPath(startableData)))
                } else if (startableData is AppShortcutData) {
                    result.add(AppShortcutItem(
                            startableData.id,
                            startableData.name,
                            startableData.customName,
                            startableData.packageName,
                            startableData.shortcutId,
                            applistModel.getShortcutIconPath(startableData)))
                }
            }
        }
        return result
    }

    fun viewToModel(pageId: Long, pageName: String, items: List<BaseItem>): PageData {
        val sectionDatas = ArrayList<SectionData>()
        var sectionData: SectionData? = null
        var startableDatas: MutableList<StartableData> = ArrayList()
        for (item in items) {
            if (item is SectionItem) {
                // Finish previous section
                if (sectionData != null) {
                    sectionData.setStartables(startableDatas)
                    sectionDatas.add(sectionData)
                }
                // Start a new section
                startableDatas = ArrayList()
                sectionData = SectionData(
                        item.id,
                        item.name,
                        emptyList(),
                        item.isRemovable,
                        item.isCollapsed)
            } else if (item is AppItem) {
                startableDatas.add(AppData(
                        item.id,
                        item.packageName,
                        item.className,
                        item.versionCode,
                        item.name,
                        item.customName))
            } else if (item is ShortcutItem) {
                (item.intent.getPackage()
                        ?: item.intent.component?.packageName)?.let {
                    startableDatas.add(ShortcutData(
                            item.id,
                            it,
                            item.name,
                            item.customName,
                            item.intent))
                }
            } else if (item is AppShortcutItem) {
                startableDatas.add(AppShortcutData(
                        item.id,
                        item.name,
                        item.customName,
                        item.packageName,
                        item.shortcutId))
            }
        }
        if (sectionData != null) {
            sectionData.setStartables(startableDatas)
            sectionDatas.add(sectionData)
        }
        return PageData(pageId, pageName, sectionDatas)
    }

}
