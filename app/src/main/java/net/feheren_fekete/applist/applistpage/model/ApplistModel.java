package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.Nullable;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.AppUtils;
import net.feheren_fekete.applist.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

// FIXME: Make th public methods synchronized? Can they be accesses from parallel threads?
public class ApplistModel {

    private static final String TAG = ApplistModel.class.getSimpleName();

    public static final int INVALID_ID = 0;

    private static ApplistModel sInstance;

    private FileUtils mFileUtils = new FileUtils();
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
            sInstance = new ApplistModel(context, packageManager);
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    sInstance.loadData();
                    return null;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    sInstance.updateInstalledApps();
                    return null;
                }
            });
        }
    }

    public static ApplistModel getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException("ApplistModel singleton is not initialized");
        }
    }

    private ApplistModel(Context context, PackageManager packageManager) {
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
            EventBus.getDefault().post(new DataLoadedEvent());
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

    private void storeData() {
        synchronized (this) {
            storePages(mPagesFilePath);
            storeInstalledApps(mInstalledAppsFilePath);
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

    public void setPage(String pageName, PageData newPage) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getName().equals(pageName)) {
                    // We don't change the page ID.
                    page.setName(newPage.getName());
                    page.setSections(newPage.getSections());
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                    return;
                }
            }
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

    public void sortApps() {
        synchronized (this) {
            for (PageData pageData : mPages) {
                for (SectionData sectionData : pageData.getSections()) {
                    sectionData.sortAppsAlphabetically();
                }
            }
            EventBus.getDefault().post(new SectionsChangedEvent());
            scheduleStoreData();
        }
    }

    public void sortAppsInPage(String pageName) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                for (SectionData sectionData : page.getSections()) {
                    sectionData.sortAppsAlphabetically();
                }
                EventBus.getDefault().post(new SectionsChangedEvent());
                scheduleStoreData();
            }
        }
    }

    public void sortAppsInSection(String pageName, String sectionName) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                SectionData sectionData = page.getSection(sectionName);
                if (sectionData != null) {
                    sectionData.sortAppsAlphabetically();
                    EventBus.getDefault().post(new SectionsChangedEvent());
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
                        jsonApp.put("component-name", app.getClassName());
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
            ApplistLog.getInstance().log(e);
            return;
        }

        mFileUtils.writeFile(filePath, data);
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
                jsonApp.put("component-name", app.getClassName());
                jsonApp.put("app-name", app.getAppName());
                jsonApps.put(jsonApp);
            }
            jsonObject.put("installed-apps", jsonApps);
            data = jsonObject.toString(2);
        } catch (JSONException e) {
            ApplistLog.getInstance().log(e);
            return;
        }

        mFileUtils.writeFile(filePath, data);
    }

    private void addUncategorizedSection(PageData page) {
        List<AppData> uncategorizedApps = new ArrayList<>();
        for (AppData app : mInstalledApps) {
            if (!page.hasApp(app)) {
                uncategorizedApps.add(app);
            }
        }

        Collections.sort(uncategorizedApps, new AppData.NameComparator());

        SectionData uncategorizedSection = new SectionData(
                createSectionId(), mUncategorizedSectionName, uncategorizedApps, false, false);
        page.addSection(uncategorizedSection);
    }

    private boolean updateSections(PageData page) {
        if (mInstalledApps.isEmpty()) {
            return false;
        }

        boolean isSectionChanged = false;
        ArrayList<AppData> uncategorizedApps = new ArrayList<>(mInstalledApps);
        SectionData uncategorizedSection = page.getSectionByRemovable(false);
        for (SectionData section : page.getSections()) {
            if (section != uncategorizedSection) {
                isSectionChanged |= updateSection(section, uncategorizedApps);
                uncategorizedApps.removeAll(section.getApps());
            }
        }

        if (uncategorizedSection != null) {
            isSectionChanged |= updateSection(uncategorizedSection, uncategorizedApps);
            uncategorizedApps.removeAll(uncategorizedSection.getApps());
            if (!uncategorizedApps.isEmpty()) {
                Collections.sort(uncategorizedApps, new AppData.NameComparator());
                uncategorizedSection.addApps(0, uncategorizedApps);
                isSectionChanged = true;
            }
        }

        return isSectionChanged;
    }

    private boolean updateSection(SectionData sectionData, List<AppData> availableApps) {
        boolean isSectionChanged = false;
        List<AppData> availableAppsInSection = new ArrayList<>();
        for (AppData app : sectionData.getApps()) {
            final int availableAppPos = availableApps.indexOf(app);
            final boolean isAvailable = (availableAppPos != -1);
            if (isAvailable) {
                // The app name may have changed. E.g. The user changed the system
                // language.
                AppData installedApp = availableApps.get(availableAppPos);
                if (!app.getAppName().equals(installedApp.getAppName())) {
                    isSectionChanged = true;
                }
                availableAppsInSection.add(installedApp);
            }
        }
        if (sectionData.getApps().size() != availableAppsInSection.size()) {
            isSectionChanged = true;
        }
        sectionData.setApps(availableAppsInSection);
        return isSectionChanged;
    }

    private List<PageData> loadPages(String filePath) {
        List<PageData> pages = new ArrayList<>();
        String fileContent = mFileUtils.readFile(filePath);
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
        String fileContent = mFileUtils.readFile(filePath);
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
