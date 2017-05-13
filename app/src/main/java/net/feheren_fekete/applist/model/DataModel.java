package net.feheren_fekete.applist.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.AppUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Task;

// FIXME: Make th public methods synchronized? Can they be accesses from parallel threads?
public class DataModel {

    private static final String TAG = DataModel.class.getSimpleName();

    public static final int INVALID_ID = 0;

    private static DataModel sInstance;

    private Handler mHandler;
    private PackageManager mPackageManager;
    private String mUncategorizedSectionName;
    private String mPagesFilePath;
    private String mInstalledAppsFilePath;
    private List<AppData> mInstalledApps;
    private List<PageData> mPages;

    public static final class PagesChangedEvent {}
    public static final class SectionsChangedEvent {}
    public static final class DataLoadedEvent {}

    public static void initInstance(Context context, PackageManager packageManager) {
        if (sInstance == null) {
            sInstance = new DataModel(context, packageManager);
        }
    }

    public static DataModel getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException("DataModel singleton is not initialized");
        }
    }

    private DataModel(Context context, PackageManager packageManager) {
        mHandler = new Handler();
        mPackageManager = packageManager;
        mUncategorizedSectionName = context.getResources().getString(R.string.uncategorized_group);
        mPagesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-pages.json";
        mInstalledAppsFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-installed-apps.json";
        mInstalledApps = new ArrayList<>();
        mPages = new ArrayList<>();
    }

    public void loadData() {
        synchronized (this) {
            mInstalledApps = loadInstalledApps(mInstalledAppsFilePath);
            List<PageData> pages = loadPages(mPagesFilePath);
            for (PageData page : pages) {
                updateSections(page);
            }
            mPages = pages;
            EventBus.getDefault().postSticky(new DataLoadedEvent());
        }
    }

    public void updateInstalledApps() {
        List<AppData> installedApps = AppUtils.getInstalledApps(mPackageManager);
        synchronized (this) {
            mInstalledApps = installedApps;
            boolean isSectionChanged = false;
            for (PageData page : mPages) {
                if (updateSections(page)) {
                    isSectionChanged = true;
                }
            }
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
            }

            scheduleStoreData();
        }
    }

    public void storeData() {
        synchronized (this) {
            storePages(mPagesFilePath);
            storeInstalledApps(mInstalledAppsFilePath);
        }
    }

    public void storePages() {
        synchronized (this) {
            storePages(mPagesFilePath);
        }
    }

    public @Nullable PageData getPage(String pageName) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getName().equals(pageName)) {
                    return page;
                }
            }
            return null;
        }
    }

    public int getPageCount() {
        synchronized (this) {
            return mPages.size();
        }
    }

    public void addNewPage(String pageName) {
        synchronized (this) {
            PageData page = new PageData(createPageId(), pageName, new ArrayList<SectionData>());
            addUncategorizedSection(page);
            // Always add to the beginning of the list.
            mPages.add(0, page);
            EventBus.getDefault().post(new PagesChangedEvent());
            scheduleStoreData();
        }
    }

    public void setPageName(String oldPageName, String newPageName) {
        synchronized (this) {
            PageData page = getPage(oldPageName);
            if (page != null) {
                page.setName(newPageName);
                EventBus.getDefault().post(new PagesChangedEvent());
                scheduleStoreData();
            }
        }
    }

    public void removePage(String pageName) {
        synchronized (this) {
            List<PageData> remainingPages = new ArrayList<>();
            for (PageData p : mPages) {
                if (!p.getName().equals(pageName)) {
                    remainingPages.add(p);
                }
            }
            mPages = remainingPages;
            EventBus.getDefault().post(new PagesChangedEvent());
            scheduleStoreData();
        }
    }

    public void removeAllPages() {
        synchronized (this) {
            mPages = new ArrayList<>();
            EventBus.getDefault().post(new PagesChangedEvent());
            scheduleStoreData();
        }
    }

    public List<String> getPageNames() {
        synchronized (this) {
            List<String> pageNames = new ArrayList<>();
            for (PageData page : mPages) {
                pageNames.add(page.getName());
            }
            return pageNames;
        }
    }

    public void removeSection(String pageName, String sectionName) {
        synchronized (this) {
            for (PageData p : mPages) {
                if (p.getName().equals(pageName)) {
                    p.removeSection(sectionName);
                    updateSections(p);
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                    return;
                }
            }
        }
    }

    public void addNewSection(String pageName, String sectionName, boolean removable) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getName().equals(pageName)) {
                    page.addSection(new SectionData(
                            createSectionId(), sectionName, new ArrayList<AppData>(), removable, false));
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                    return;
                }
            }
        }
    }

    public List<String> getSectionNames(String pageName) {
        synchronized (this) {
            List<String> sectionNames = new ArrayList<>();
            PageData page = getPage(pageName);
            if (page != null) {
                for (SectionData section : page.getSections()) {
                    sectionNames.add(section.getName());
                }
            }
            return sectionNames;
        }
    }

    public void setSectionName(String pageName, String oldSectionName, String newSectionName) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                if (page.renameSection(oldSectionName, newSectionName)) {
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                }
            }
        }
    }

    public void setSectionCollapsed(String pageName, String sectionName, boolean collapsed) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                SectionData section = page.getSection(sectionName);
                if (section != null && !section.isEmpty()) {
                    boolean oldState = section.isCollapsed();
                    if (oldState != collapsed) {
                        section.setCollapsed(collapsed);
                        EventBus.getDefault().post(new SectionsChangedEvent());
                        scheduleStoreData();
                    }
                }
            }
        }
    }

    public void setAllSectionsCollapsed(String pageName, boolean collapsed, boolean save) {
        synchronized (this) {
            boolean isSectionChanged = false;
            PageData page = getPage(pageName);
            if (page != null) {
                for (SectionData section : page.getSections()) {
                    if (!section.isEmpty()) {
                        boolean oldState = section.isCollapsed();
                        if (oldState != collapsed) {
                            section.setCollapsed(collapsed);
                            isSectionChanged = true;
                        }
                    }
                }
            }
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
                if (save) {
                    scheduleStoreData();
                }
            }
        }
    }

    public void setAllSectionsCollapsed(String pageName,
                                        Map<String, Boolean> collapsedStates,
                                        boolean save) {
        synchronized (this) {
            boolean isSectionChanged = false;
            PageData page = getPage(pageName);
            if (page != null) {
                for (SectionData section : page.getSections()) {
                    if (!section.isEmpty()) {
                        boolean oldState = section.isCollapsed();
                        boolean newState = collapsedStates.get(section.getName());
                        if (oldState != newState) {
                            section.setCollapsed(newState);
                            isSectionChanged = true;
                        }
                    }
                }
            }
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
                if (save) {
                    scheduleStoreData();
                }
            }
        }
    }

    public void setSectionOrder(String pageName, List<String> orderedSectionNames, boolean save) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                List<SectionData> orderedSections = new ArrayList<>();
                for (String sectionName : orderedSectionNames) {
                    orderedSections.add(page.getSection(sectionName));
                }
                page.setSections(orderedSections);
                EventBus.getDefault().post(new SectionsChangedEvent());
                if (save) {
                    scheduleStoreData();
                }
            }
        }
    }

    public void moveAppToSection(String pageName, String sectionName, AppData app) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                page.removeApp(app);
                SectionData section = page.getSection(sectionName);
                if (section != null) {
                    section.addApp(app);
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                }
            }
        }
    }

    private long createPageId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
    }

    private long createSectionId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
    }

    private void storePages(String filePath) {
        String data = "";
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonPages = new JSONArray();
            for (PageData page : mPages) {
                JSONObject jsonPage = new JSONObject();
                jsonPage.put("id", page.getId());
                jsonPage.put("name", page.getName());

                JSONArray jsonSections = new JSONArray();
                for (SectionData section : page.getSections()) {
                    JSONObject jsonSection = new JSONObject();
                    jsonSection.put("id", section.getId());
                    jsonSection.put("name", section.getName());
                    jsonSection.put("is-removable", section.isRemovable());
                    jsonSection.put("is-collapsed", section.isCollapsed());

                    JSONArray jsonApps = new JSONArray();
                    for (AppData app : section.getApps()) {
                        JSONObject jsonApp = new JSONObject();
                        jsonApp.put("id", app.getId());
                        jsonApp.put("package-name", app.getPackageName());
                        jsonApp.put("component-name", app.getComponentName());
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
            data = jsonObject.toString(2);
        } catch (JSONException e) {
            Log.e(TAG, "Cannot construct JSON", e);
            return;
        }

        writeFile(filePath, data);
    }

    private void storeInstalledApps(String filePath) {
        String data = "";
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonApps = new JSONArray();
            for (AppData app : mInstalledApps) {
                JSONObject jsonApp = new JSONObject();
                jsonApp.put("id", app.getId());
                jsonApp.put("package-name", app.getPackageName());
                jsonApp.put("component-name", app.getComponentName());
                jsonApp.put("app-name", app.getAppName());
                jsonApps.put(jsonApp);
            }
            jsonObject.put("installed-apps", jsonApps);
            data = jsonObject.toString(2);
        } catch (JSONException e) {
            Log.e(TAG, "Cannot construct JSON", e);
            return;
        }

        writeFile(filePath, data);
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

        SectionData uncategorizedSection = new SectionData(
                createSectionId(), mUncategorizedSectionName, uncategorizedApps, false, false);
        page.addSection(uncategorizedSection);
    }

    private boolean updateSections(PageData page) {
        if (mInstalledApps.isEmpty()) {
            return false;
        }

        boolean isSectionChanged = false;
        List<AppData> uncategorizedApps = new ArrayList<>(mInstalledApps);
        for (SectionData section : page.getSections()) {
            List<AppData> installedAppsInSection = new ArrayList<>();
            for (AppData app : section.getApps()) {
                final int installedAppPos = mInstalledApps.indexOf(app);
                final boolean isInstalled = (installedAppPos != -1);
                if (isInstalled) {
                    // The app name may have changed. E.g. The user changed the system
                    // language.
                    AppData installedApp = mInstalledApps.get(installedAppPos);
                    if (!app.getAppName().equals(installedApp.getAppName())) {
                        isSectionChanged = true;
                    }
                    installedAppsInSection.add(installedApp);
                    uncategorizedApps.remove(installedApp);
                }
            }
            if (section.getApps().size() != installedAppsInSection.size()) {
                isSectionChanged = true;
            }
            section.setApps(installedAppsInSection);
        }

        SectionData uncategorizedSection = page.getSectionByRemovable(false);
        if (uncategorizedSection != null) {
            uncategorizedSection.addApps(uncategorizedApps);
            // TODO: Compare the old and new uncategorized section app-by-app.
            isSectionChanged = true;
        }

        return isSectionChanged;
    }

    private List<PageData> loadPages(String filePath) {
        List<PageData> pages = new ArrayList<>();
        String fileContent = readFile(filePath);
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

        return new PageData(INVALID_ID, jsonPage.getString("name"), sections);
    }

    private SectionData loadSection(JSONObject jsonSection) throws JSONException {
        List<AppData> apps = new ArrayList<>();
        JSONArray jsonApps = jsonSection.getJSONArray("apps");
        for (int k = 0; k < jsonApps.length(); ++k) {
            JSONObject jsonApp = jsonApps.getJSONObject(k);
            AppData app = new AppData(
                    jsonApp.getLong("id"),
                    jsonApp.getString("package-name"),
                    jsonApp.getString("component-name"),
                    jsonApp.getString("app-name"));
            apps.add(app);
        }
        return new SectionData(
                jsonSection.getLong("id"),
                jsonSection.getString("name"),
                apps,
                loadJsonBoolean(jsonSection, "is-removable", true),
                loadJsonBoolean(jsonSection, "is-collapsed", false));
    }

    private List<AppData> loadInstalledApps(String filePath) {
        List<AppData> installedApps = new ArrayList<>();
        String fileContent = readFile(filePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            JSONArray jsonInstalledApps = jsonObject.getJSONArray("installed-apps");
            for (int k = 0; k < jsonInstalledApps.length(); ++k) {
                JSONObject jsonApp = jsonInstalledApps.getJSONObject(k);
                AppData app = new AppData(
                        jsonApp.getLong("id"),
                        jsonApp.getString("package-name"),
                        jsonApp.getString("component-name"),
                        jsonApp.getString("app-name"));
                installedApps.add(app);
            }

        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return installedApps;
    }

    private boolean loadJsonBoolean(JSONObject json, String name, boolean defaultValue) {
        try {
            return json.getBoolean(name);
        } catch (JSONException e) {
            return defaultValue;
        }
    }

    private String loadJsonString(JSONObject json, String name, String defaultValue) {
        try {
            return json.getString(name);
        } catch (JSONException e) {
            return defaultValue;
        }
    }

    private String readFile(String filePath) {
        String fileContent = "";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            return fileContent;
        }
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(fis, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return fileContent;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder("");
            char[] buffer = new char[1024];
            int n;
            while ((n = isr.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, n));
            }
            fileContent = stringBuilder.toString();
        } catch (IOException e) {
            // Ignore
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return fileContent;
    }

    private void writeFile(String filePath, String content) {
        BufferedWriter bw = null;
        try {
            FileOutputStream fw = new FileOutputStream(filePath);
            OutputStreamWriter osw = new OutputStreamWriter(fw, "UTF-8");
            bw = new BufferedWriter(osw);
            bw.write(content);
        } catch (IOException e) {
            // TODO: Report error.
            Log.e(TAG, "Cannot write file", e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // TODO: Report error
                    Log.e(TAG, "Cannot close file", e);
                }
            }
        }
    }

    private Runnable mStoreDataRunnable = new Runnable() {
        @Override
        public void run() {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    storeData();
                    return null;
                }
            });
        }
    };

    private void scheduleStoreData() {
        mHandler.removeCallbacks(mStoreDataRunnable);
        mHandler.postDelayed(mStoreDataRunnable, 500);
    }

}
