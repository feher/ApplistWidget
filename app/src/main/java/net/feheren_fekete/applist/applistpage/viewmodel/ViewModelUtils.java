package net.feheren_fekete.applist.applistpage.viewmodel;

import net.feheren_fekete.applist.applistpage.model.AppData;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.PageData;
import net.feheren_fekete.applist.applistpage.model.SectionData;
import net.feheren_fekete.applist.applistpage.model.ShortcutData;
import net.feheren_fekete.applist.applistpage.model.StartableData;

import java.util.ArrayList;
import java.util.List;

public class ViewModelUtils {

    public static List<BaseItem> modelToView(ApplistModel applistModel, PageData pageData) {
        List<BaseItem> result = new ArrayList<>();
        for (SectionData sectionData : pageData.getSections()) {
            result.add(new SectionItem(
                    sectionData.getId(),
                    sectionData.getName(),
                    sectionData.isRemovable(),
                    sectionData.isCollapsed()));
            for (StartableData startableData : sectionData.getStartables()) {
                if (startableData instanceof AppData) {
                    AppData appData = (AppData) startableData;
                    result.add(new AppItem(
                            appData.getId(),
                            appData.getPackageName(),
                            appData.getClassName(),
                            appData.getName()));
                } else if (startableData instanceof ShortcutData) {
                    ShortcutData shortcutData = (ShortcutData) startableData;
                    result.add(new ShortcutItem(
                            shortcutData.getId(),
                            shortcutData.getName(),
                            shortcutData.getIntent(),
                            applistModel.getShortcutIconPath(shortcutData)));
                }
            }
        }
        return result;
    }

    public static PageData viewToModel(long pageId, String pageName, List<BaseItem> items) {
        List<SectionData> sectionDatas = new ArrayList<>();
        SectionData sectionData = null;
        List<StartableData> startableDatas = new ArrayList<>();
        for (BaseItem item : items) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                if (sectionData != null) {
                    sectionDatas.add(sectionData);
                }
                // Start a new section
                startableDatas = new ArrayList<>();
                sectionData = new SectionData(
                        sectionItem.getId(),
                        sectionItem.getName(),
                        startableDatas,
                        sectionItem.isRemovable(),
                        sectionItem.isCollapsed());
            } else if (item instanceof AppItem) {
                AppItem appItem = (AppItem) item;
                startableDatas.add(new AppData(
                        appItem.getId(),
                        appItem.getPackageName(),
                        appItem.getClassName(),
                        appItem.getName()));
            } else if (item instanceof ShortcutItem) {
                ShortcutItem shortcutItem = (ShortcutItem) item;
                startableDatas.add(new ShortcutData(
                        shortcutItem.getId(),
                        shortcutItem.getName(),
                        shortcutItem.getIntent()));
            }
        }
        if (sectionData != null) {
            sectionDatas.add(sectionData);
        }
        return new PageData(pageId, pageName, sectionDatas);
    }

}
