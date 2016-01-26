package net.feheren_fekete.applistwidget.viewmodel;

import net.feheren_fekete.applistwidget.model.AppData;
import net.feheren_fekete.applistwidget.model.PageData;
import net.feheren_fekete.applistwidget.model.SectionData;

import java.util.ArrayList;
import java.util.List;

public class ViewModelUtils {

    public static List<BaseItem> modelToView(PageData pageData) {
        List<BaseItem> result = new ArrayList<>();
        for (SectionData sectionData : pageData.getSections()) {
            result.add(new SectionItem(sectionData.getName()));
            for (AppData appData : sectionData.getApps()) {
                result.add(new AppItem(
                        appData.getPackageName(),
                        appData.getAppName()));
            }
        }
        return result;
    }

    public static PageData viewToModel(String pageName, List<BaseItem> items) {
        List<SectionData> sectionDatas = new ArrayList<>();
        SectionData sectionData = null;
        List<AppData> appDatas = null;
        for (BaseItem item : items) {
            if (item instanceof SectionItem) {
                SectionItem sectionItem = (SectionItem) item;
                if (sectionData != null) {
                    sectionDatas.add(sectionData);
                }
                // Start a new section
                appDatas = new ArrayList<>();
                sectionData = new SectionData(sectionItem.getName(), appDatas);
            } else if (item instanceof AppItem) {
                AppItem appItem = (AppItem) item;
                appDatas.add(new AppData(appItem.getPackageName(), appItem.getAppName()));
            }
        }
        if (sectionData != null) {
            sectionDatas.add(sectionData);
        }
        return new PageData(pageName, sectionDatas);
    }

}
