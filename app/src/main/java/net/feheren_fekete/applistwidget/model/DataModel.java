package net.feheren_fekete.applistwidget.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DataModel {

    public static final String DEFAULT_PAGE_NAME = "Apps";
    private static final String UNCATEGORIZED_SECTION_NAME = "Uncategorized";

    private PackageManager mPackageManager;
    private String mDatabasePath;
    private List<AppData> mInstalledApps;
    private List<PageData> mPages;

    public static final class PagesChangedEvent {}
    public static final class SectionsChangedEvent {}
    public static final class DataLoadedEvent {}

    public DataModel(Context context, PackageManager packageManager) {
        mPackageManager = packageManager;
        mDatabasePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist.json";
        mInstalledApps = new ArrayList<>();
        mPages = new ArrayList<>();
    }

    public void loadData() {
        mInstalledApps = getInstalledApps();
        List<PageData> pages = loadPages();
        for (PageData page : pages) {
            updateSections(page);
        }
        mPages = pages;
        EventBus.getDefault().postSticky(new DataLoadedEvent());
    }

    public void storeData() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonPages = new JSONArray();
            for (PageData page : mPages) {
                JSONObject jsonPage = new JSONObject();
                jsonPage.put("name", page.getName());

                JSONArray jsonSections = new JSONArray();
                for (SectionData section : page.getSections()) {
                    JSONObject jsonSection = new JSONObject();
                    jsonSection.put("name", section.getName());

                    JSONArray jsonApps = new JSONArray();
                    for (AppData app : section.getApps()) {
                        JSONObject jsonApp = new JSONObject();
                        jsonApp.put("package-name", app.getPackageName());
                        jsonApp.put("app-name", app.getAppName());
                        jsonApps.put(jsonApp);
                    }

                    jsonSection.put("apps", jsonApps);
                    jsonSections.put(jsonSection);
                }

                jsonPage.put("sections", jsonSections);
                jsonPages.put(jsonPage);
            }

            jsonObject.put("pages", jsonPages);
        } catch (JSONException e) {
            return;
        }

        BufferedWriter bw = null;
        try {
            String content = jsonObject.toString();
            FileWriter fw = new FileWriter(mDatabasePath);
            bw = new BufferedWriter(fw);
            bw.write(content);
        } catch (IOException e) {
            // TODO: Report error.
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // TODO: Report error
                }
            }
        }
    }

    public PageData getPage(String pageName) {
        for (PageData page : mPages) {
            if (page.getName().equals(pageName)) {
                return page;
            }
        }
        return new PageData(pageName, new ArrayList<SectionData>());
    }

    public int getPageCount() {
        return mPages.size();
    }

    public void addNewPage(String pageName) {
        PageData page = new PageData(pageName, new ArrayList<SectionData>());
        addUncategorizedSection(page);
        // Always add to the beginning of the list.
        mPages.add(0, page);
        EventBus.getDefault().post(new PagesChangedEvent());
    }

    public void setPage(PageData page) {
        for (PageData p : mPages) {
            if (p.getName().equals(page.getName())) {
                p.setSections(page.getSections());
            }
        }
        EventBus.getDefault().post(new SectionsChangedEvent());
    }

    public void removePage(String pageName) {
        List<PageData> remainingPages = new ArrayList<>();
        for (PageData p : mPages) {
            if (!p.getName().equals(pageName)) {
                remainingPages.add(p);
            }
        }
        mPages = remainingPages;
        EventBus.getDefault().post(new PagesChangedEvent());
    }

    public void removeAllPages() {
        mPages = new ArrayList<>();
        EventBus.getDefault().post(new PagesChangedEvent());
    }

    public void removeSection(String pageName, String sectionName) {
        for (PageData p : mPages) {
            if (p.getName().equals(pageName)) {
                p.removeSection(sectionName);
                EventBus.getDefault().post(new SectionsChangedEvent());
                return;
            }
        }
    }

    public void addNewSection(String pageName, String sectionName) {
        for (PageData page : mPages) {
            if (page.getName().equals(pageName)) {
                page.addSection(new SectionData(sectionName, new ArrayList<AppData>()));
                EventBus.getDefault().post(new SectionsChangedEvent());
                return;
            }
        }
    }

    public List<String> getPageNames() {
        List<String> pageNames = new ArrayList<>();
        for (PageData page : mPages) {
            pageNames.add(page.getName());
        }
        return pageNames;
    }

    private void addUncategorizedSection(PageData page) {
        List<AppData> uncategorizedApps = new ArrayList<>();
        for (AppData app : mInstalledApps) {
            if (!page.hasApp(app)) {
                uncategorizedApps.add(app);
            }
        }

        Collections.sort(uncategorizedApps, new Comparator<AppData>() {
            @Override
            public int compare(AppData lhs, AppData rhs) {
                return lhs.getAppName().compareToIgnoreCase(rhs.getAppName());
            }
        });

        SectionData uncategorizedSection = new SectionData(UNCATEGORIZED_SECTION_NAME, uncategorizedApps);
        page.addSection(uncategorizedSection);
    }

    private void updateSections(PageData page) {
        List<AppData> uncategorizedApps = new ArrayList<>(mInstalledApps);
        for (SectionData section : page.getSections()) {
            List<AppData> installedAppsInSection = new ArrayList<>();
            for (AppData app : section.getApps()) {
                if (isInstalled(app)) {
                    installedAppsInSection.add(app);
                    uncategorizedApps.remove(app);
                }
            }
            section.setApps(installedAppsInSection);
        }

        SectionData uncategorizedSection = page.getSection(UNCATEGORIZED_SECTION_NAME);
        if (uncategorizedSection != null) {
            uncategorizedSection.addApps(uncategorizedApps);
        }
    }

    private boolean isInstalled(AppData app) {
        return mInstalledApps.contains(app);
    }

    private List<PageData> loadPages() {
        List<PageData> pages = new ArrayList<>();
        String fileContent = readFile(mDatabasePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            JSONArray jsonPages = jsonObject.getJSONArray("pages");
            for (int i = 0; i < jsonPages.length(); ++i) {
                JSONObject jsonPage = jsonPages.getJSONObject(i);
                pages.add(loadPage(jsonPage));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return pages;
    }

    private PageData loadPage(JSONObject jsonPage) throws JSONException {
        List<SectionData> sections = new ArrayList<>();
        JSONArray jsonSections = jsonPage.getJSONArray("sections");
        for (int j = 0; j < jsonSections.length(); ++j) {
            JSONObject jsonSection = jsonSections.getJSONObject(j);
            sections.add(loadSection(jsonSection));
        }

        return new PageData(jsonPage.getString("name"), sections);
    }

    private SectionData loadSection(JSONObject jsonSection) throws JSONException {
        List<AppData> apps = new ArrayList<>();
        JSONArray jsonApps = jsonSection.getJSONArray("apps");
        for (int k = 0; k < jsonApps.length(); ++k) {
            JSONObject jsonApp = jsonApps.getJSONObject(k);
            AppData app = new AppData(
                    jsonApp.getString("package-name"),
                    jsonApp.getString("app-name"));
            apps.add(app);
        }
        return new SectionData(jsonSection.getString("name"), apps);
    }

    private String readFile(String filePath) {
        String fileContent = "";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            return fileContent;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder("");
            byte[] buffer = new byte[1024];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, n));
            }
            fileContent = stringBuilder.toString();
        } catch (IOException e) {
            // Ignore
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return fileContent;
    }

    private List<AppData> getInstalledApps() {
        List<AppData> packageNames = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedApps = mPackageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : installedApps) {
            packageNames.add(new AppData(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.loadLabel(mPackageManager).toString()));
        }
        return packageNames;
    }

}
