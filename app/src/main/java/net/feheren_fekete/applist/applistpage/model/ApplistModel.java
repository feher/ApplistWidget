package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.Intent;
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
import java.net.URISyntaxException;
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

    public static final String JSON_PAGES = "pages";
    public static final String JSON_PAGE_ID = "id";
    public static final String JSON_PAGE_NAME = "name";

    public static final String JSON_SECTIONS = "sections";
    public static final String JSON_SECTION_ID = "id";
    public static final String JSON_SECTION_NAME = "name";

    public static final String JSON_SECTION_IS_REMOVABLE = "is-removable";
    public static final String JSON_SECTION_IS_COLLAPSED = "is-collapsed";

    public static final String JSON_STARTABLES = "startables";
    public static final String JSON_STARTABLE_ID = "id";
    public static final String JSON_STARTABLE_TYPE = "type";
    public static final String JSON_STARTABLE_TYPE_APP = "app";
    public static final String JSON_STARTABLE_TYPE_SHORTCUT = "shortcut";
    public static final String JSON_STARTABLE_NAME = "name";
    public static final String JSON_APP_PACKAGE_NAME = "package-name";
    public static final String JSON_APP_CLASS_NAME = "class-name";
    public static final String JSON_SHORTCUT_INTENT = "intent";

    public static final String JSON_INSTALLED_APPS = "installed-apps";
    public static final String JSON_INSTALLED_APP_ID = "id";
    public static final String JSON_INSTALLED_APP_PACKAGE_NAME = "package-name";
    public static final String JSON_INSTALLED_APP_CLASS_NAME = "class-name";
    public static final String JSON_INSATLLED_APP_NAME = "app-name";

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
                            createSectionId(), sectionName, new ArrayList<StartableData>(), removable, false));
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

    public void sortStartables() {
        synchronized (this) {
            for (PageData pageData : mPages) {
                for (SectionData sectionData : pageData.getSections()) {
                    sectionData.sortStartablesAlphabetically();
                }
            }
            EventBus.getDefault().post(new SectionsChangedEvent());
            scheduleStoreData();
        }
    }

    public void sortStartablesInPage(String pageName) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                for (SectionData sectionData : page.getSections()) {
                    sectionData.sortStartablesAlphabetically();
                }
                EventBus.getDefault().post(new SectionsChangedEvent());
                scheduleStoreData();
            }
        }
    }

    public void sortStartablesInSection(String pageName, String sectionName) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                SectionData sectionData = page.getSection(sectionName);
                if (sectionData != null) {
                    sectionData.sortStartablesAlphabetically();
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                }
            }
        }
    }

    public void moveStartableToSection(String pageName, String sectionName, StartableData startableData) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                page.removeStartable(startableData);
                SectionData section = page.getSection(sectionName);
                if (section != null) {
                    section.addStartable(startableData);
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
                jsonPage.put(JSON_PAGE_ID, page.getId());
                jsonPage.put(JSON_PAGE_NAME, page.getName());

                JSONArray jsonSections = new JSONArray();
                for (SectionData section : page.getSections()) {
                    JSONObject jsonSection = new JSONObject();
                    jsonSection.put(JSON_SECTION_ID, section.getId());
                    jsonSection.put(JSON_SECTION_NAME, section.getName());
                    jsonSection.put(JSON_SECTION_IS_REMOVABLE, section.isRemovable());
                    jsonSection.put(JSON_SECTION_IS_COLLAPSED, section.isCollapsed());

                    JSONArray jsonStartables = new JSONArray();
                    for (StartableData startableData : section.getStartables()) {
                        if (startableData instanceof AppData) {
                            AppData app = (AppData) startableData;
                            JSONObject jsonApp = new JSONObject();
                            jsonApp.put(JSON_STARTABLE_ID, app.getId());
                            jsonApp.put(JSON_STARTABLE_TYPE, "app");
                            jsonApp.put(JSON_STARTABLE_NAME, app.getName());
                            jsonApp.put(JSON_APP_PACKAGE_NAME, app.getPackageName());
                            jsonApp.put(JSON_APP_CLASS_NAME, app.getClassName());
                            jsonStartables.put(jsonApp);
                        } else if (startableData instanceof ShortcutData) {
                            ShortcutData shortcut = (ShortcutData) startableData;
                            JSONObject jsonShortcut = new JSONObject();
                            jsonShortcut.put(JSON_STARTABLE_ID, shortcut.getId());
                            jsonShortcut.put(JSON_STARTABLE_TYPE, "shortcut");
                            jsonShortcut.put(JSON_STARTABLE_NAME, shortcut.getName());
                            jsonShortcut.put(JSON_SHORTCUT_INTENT, shortcut.getIntent());
                            jsonStartables.put(jsonShortcut);
                        }
                    }

                    jsonSection.put(JSON_STARTABLES, jsonStartables);
                    jsonSections.put(jsonSection);
                }

                jsonPage.put(JSON_SECTIONS, jsonSections);
                jsonPages.put(jsonPage);
            }

            jsonObject.put(JSON_PAGES, jsonPages);
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
                jsonApp.put(JSON_INSTALLED_APP_ID, app.getId());
                jsonApp.put(JSON_INSTALLED_APP_PACKAGE_NAME, app.getPackageName());
                jsonApp.put(JSON_INSTALLED_APP_CLASS_NAME, app.getClassName());
                jsonApp.put(JSON_INSATLLED_APP_NAME, app.getName());
                jsonApps.put(jsonApp);
            }
            jsonObject.put(JSON_INSTALLED_APPS, jsonApps);
            data = jsonObject.toString(2);
        } catch (JSONException e) {
            ApplistLog.getInstance().log(e);
            return;
        }

        mFileUtils.writeFile(filePath, data);
    }

    private void addUncategorizedSection(PageData page) {
        List<StartableData> uncategorizedApps = new ArrayList<>();
        for (AppData app : mInstalledApps) {
            if (!page.hasStartable(app)) {
                uncategorizedApps.add(app);
            }
        }

        Collections.sort(uncategorizedApps, new StartableData.NameComparator());

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
                Collections.sort(uncategorizedApps, new StartableData.NameComparator());
                uncategorizedSection.addApps(0, uncategorizedApps);
                isSectionChanged = true;
            }
        }

        return isSectionChanged;
    }

    private boolean updateSection(SectionData sectionData, List<AppData> availableApps) {
        boolean isSectionChanged = false;
        List<StartableData> availableItemsInSection = new ArrayList<>();
        for (StartableData startable : sectionData.getStartables()) {
            if (startable instanceof AppData) {
                AppData app = (AppData) startable;
                final int availableAppPos = availableApps.indexOf(app);
                final boolean isAvailable = (availableAppPos != -1);
                if (isAvailable) {
                    // The app name may have changed. E.g. The user changed the system
                    // language.
                    AppData installedApp = availableApps.get(availableAppPos);
                    if (!app.getName().equals(installedApp.getName())) {
                        isSectionChanged = true;
                    }
                    availableItemsInSection.add(installedApp);
                }
            } else {
                availableItemsInSection.add(startable);
            }
        }
        if (sectionData.getStartables().size() != availableItemsInSection.size()) {
            isSectionChanged = true;
        }
        sectionData.setStartables(availableItemsInSection);
        return isSectionChanged;
    }

    private List<PageData> loadPages(String filePath) {
        List<PageData> pages = new ArrayList<>();
        String fileContent = mFileUtils.readFile(filePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            JSONArray jsonPages = jsonObject.getJSONArray(JSON_PAGES);
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
        JSONArray jsonSections = jsonPage.getJSONArray(JSON_SECTIONS);
        for (int j = 0; j < jsonSections.length(); ++j) {
            JSONObject jsonSection = jsonSections.getJSONObject(j);
            sections.add(loadSection(jsonSection));
        }

        return new PageData(INVALID_ID, jsonPage.getString(JSON_PAGE_NAME), sections);
    }

    private SectionData loadSection(JSONObject jsonSection) throws JSONException {
        List<StartableData> startableDatas = new ArrayList<>();
        JSONArray jsonStartables = jsonSection.getJSONArray(JSON_STARTABLES);
        for (int k = 0; k < jsonStartables.length(); ++k) {
            JSONObject jsonStartable = jsonStartables.getJSONObject(k);
            final String type = jsonStartable.getString(JSON_STARTABLE_TYPE);
            if (JSON_STARTABLE_TYPE_APP.equals(type)) {
                AppData app = new AppData(
                        jsonStartable.getLong(JSON_STARTABLE_ID),
                        jsonStartable.getString(JSON_APP_PACKAGE_NAME),
                        jsonStartable.getString(JSON_APP_CLASS_NAME),
                        jsonStartable.getString(JSON_STARTABLE_NAME));
                startableDatas.add(app);
            } else if (type.equals(JSON_STARTABLE_TYPE_SHORTCUT)) {
                try {
                    ShortcutData shortcutData = new ShortcutData(
                            jsonStartable.getLong(JSON_STARTABLE_ID),
                            jsonStartable.getString(JSON_STARTABLE_NAME),
                            Intent.parseUri(jsonStartable.getString(JSON_SHORTCUT_INTENT), 0));
                    startableDatas.add(shortcutData);
                } catch (URISyntaxException e) {
                    ApplistLog.getInstance().log(e);
                }
            }
        }
        return new SectionData(
                jsonSection.getLong(JSON_SECTION_ID),
                jsonSection.getString(JSON_SECTION_NAME),
                startableDatas,
                loadJsonBoolean(jsonSection, JSON_SECTION_IS_REMOVABLE, true),
                loadJsonBoolean(jsonSection, JSON_SECTION_IS_COLLAPSED, false));
    }

    private List<AppData> loadInstalledApps(String filePath) {
        List<AppData> installedApps = new ArrayList<>();
        String fileContent = mFileUtils.readFile(filePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            JSONArray jsonInstalledApps = jsonObject.getJSONArray(JSON_INSTALLED_APPS);
            for (int k = 0; k < jsonInstalledApps.length(); ++k) {
                JSONObject jsonApp = jsonInstalledApps.getJSONObject(k);
                AppData app = new AppData(
                        jsonApp.getLong(JSON_INSTALLED_APP_ID),
                        jsonApp.getString(JSON_INSTALLED_APP_PACKAGE_NAME),
                        jsonApp.getString(JSON_INSTALLED_APP_CLASS_NAME),
                        jsonApp.getString(JSON_INSATLLED_APP_NAME));
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
