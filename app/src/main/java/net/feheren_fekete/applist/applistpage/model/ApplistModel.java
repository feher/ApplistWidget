package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.Nullable;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.AppUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.utils.ImageUtils;

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

    private static ApplistModel sInstance;

    private Handler mHandler;
    private PackageManager mPackageManager;
    private ApplistModelStorageV1 mApplistModelStorageV1;
    private ApplistModelStorageV2 mApplistModelStorageV2;
    private String mUncategorizedSectionName;
    private String mShortcutIconsDirPath;
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
        mApplistModelStorageV1 = new ApplistModelStorageV1(context);
        mApplistModelStorageV2 = new ApplistModelStorageV2(context);
        mUncategorizedSectionName = context.getResources().getString(R.string.uncategorized_group);
        mShortcutIconsDirPath = context.getFilesDir().getAbsolutePath() + File.separator + "shortcut-icons";
        mInstalledApps = new ArrayList<>();
        mPages = new ArrayList<>();
    }

    public void loadData() {
        synchronized (this) {
            List<PageData> pages;
            if (mApplistModelStorageV1.exists()) {
                mInstalledApps = mApplistModelStorageV1.loadInstalledApps();
                pages = mApplistModelStorageV1.loadPages();
                mApplistModelStorageV1.delete();
            } else {
                mInstalledApps = mApplistModelStorageV2.loadInstalledApps();
                pages = mApplistModelStorageV2.loadPages();
            }
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
            mApplistModelStorageV2.storePages(mPages);
            mApplistModelStorageV2.storeInstalledApps(mInstalledApps);
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

    public void createShortcut(String pageName, ShortcutData shortcutData, Bitmap shortcutIcon) {
        synchronized (this) {
            PageData page = getPage(pageName);
            if (page != null) {
                ImageUtils.saveBitmap(
                        shortcutIcon,
                        mShortcutIconsDirPath + File.separator + "shortcut-icon-" + shortcutData.getId() + ".png");
                SectionData sectionData = getUncategorizedSection(page);
                sectionData.addStartable(shortcutData);
                EventBus.getDefault().post(new SectionsChangedEvent());
                scheduleStoreData();
            }
        }
    }

    private long createPageId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
    }

    private long createSectionId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
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
        SectionData uncategorizedSection = getUncategorizedSection(page);
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

    private SectionData getUncategorizedSection(PageData pageData) {
        return pageData.getSectionByRemovable(false);
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
