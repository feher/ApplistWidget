package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.utils.AppUtils;
import net.feheren_fekete.applist.utils.RunnableWithArg;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import androidx.annotation.Nullable;
import bolts.Continuation;
import bolts.Task;

// FIXME: Make th public methods synchronized? Can they be accesses from parallel threads?
public class ApplistModel {

    private static final String TAG = ApplistModel.class.getSimpleName();

    public static final int INVALID_ID = 0;

    private Handler mHandler;
    private Context mContext;
    private ApplistModelStorageV1 mApplistModelStorageV1;
    private ApplistModelStorageV2 mApplistModelStorageV2;
    private String mUncategorizedSectionName;
    private List<StartableData> mInstalledStartables;
    private List<PageData> mPages;

    public static final class PagesChangedEvent {}
    public static final class SectionsChangedEvent {}
    public static final class DataLoadedEvent {}

    public ApplistModel(Context context) {
        mHandler = new Handler();
        mContext = context;
        mApplistModelStorageV1 = new ApplistModelStorageV1(context);
        mApplistModelStorageV2 = new ApplistModelStorageV2(context);
        mUncategorizedSectionName = context.getResources().getString(R.string.uncategorized_group);
        mInstalledStartables = new ArrayList<>();
        mPages = new ArrayList<>();

        Task.callInBackground((Callable<Void>) () -> {
            loadData();
            return null;
        }).continueWith((Continuation<Void, Void>) task -> {
            updateInstalledApps();
            return null;
        });
    }

    private void loadData() {
        synchronized (this) {
            List<PageData> pages;
            if (mApplistModelStorageV1.exists()) {
                mInstalledStartables = mApplistModelStorageV1.loadInstalledApps();
                pages = mApplistModelStorageV1.loadPages();
                // Delete the old data files and store the new ones.
                mApplistModelStorageV1.delete();
                scheduleStoreData();
            } else {
                mInstalledStartables = mApplistModelStorageV2.loadInstalledStartables();
                pages = mApplistModelStorageV2.loadPages();
            }
            updatePages(pages);
            mPages = pages;
            EventBus.getDefault().post(new DataLoadedEvent());
        }
    }

    public void updateInstalledApps() {
        List<AppData> newInstalledApps = AppUtils.getInstalledApps(mContext);
        synchronized (this) {
            List<StartableData> oldInstalledApps = new ArrayList<>();
            List<StartableData> oldInstalledShortcuts = new ArrayList<>();
            for (StartableData startableData : mInstalledStartables) {
                if (startableData instanceof AppData) {
                    oldInstalledApps.add(startableData);
                }
                if (startableData instanceof ShortcutData
                        || startableData instanceof AppShortcutData) {
                    oldInstalledShortcuts.add(startableData);
                }
            }

            // Replace old installed apps with new installed apps.
            mInstalledStartables.removeAll(oldInstalledApps);
            mInstalledStartables.addAll(newInstalledApps);

            // Remove uninstalled shortcuts (i.e. shortcuts with uninstalled apps).
            List<StartableData> uninstalledShortcuts = new ArrayList<>();
            for (StartableData startableData : oldInstalledShortcuts) {
                final String shortcutPackage = startableData.getPackageName();
                boolean hasApp = false;
                for (AppData appData : newInstalledApps) {
                    if (shortcutPackage.equals(appData.getPackageName())) {
                        hasApp = true;
                        break;
                    }
                }
                if (!hasApp) {
                    uninstalledShortcuts.add(startableData);
                }
            }
            mInstalledStartables.removeAll(uninstalledShortcuts);

            boolean isSectionChanged = updatePages(mPages);
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
            }

            scheduleStoreData();
        }
    }

    private void storeData() {
        synchronized (this) {
            mApplistModelStorageV2.storePages(mPages);
            mApplistModelStorageV2.storeInstalledStartables(mInstalledStartables);
        }
    }

    public void withPage(long pageId, RunnableWithArg<PageData> runnable) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getId() == pageId) {
                    runnable.run(page);
                }
            }
        }
    }

    private @Nullable PageData getPage(long pageId) {
        for (PageData page : mPages) {
            if (page.getId() == pageId) {
                return page;
            }
        }
        return null;
    }

    public void setPage(long pageId, PageData newPage) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getId() == pageId) {
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

    public void setPageName(long pageId, String newPageName) {
        synchronized (this) {
            PageData page = getPage(pageId);
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

    public List<Long> getPageIds() {
        synchronized (this) {
            List<Long> pageIds = new ArrayList<>();
            for (PageData page : mPages) {
                pageIds.add(page.getId());
            }
            return pageIds;
        }
    }

    public void removeSection(long pageId, long sectionId) {
        synchronized (this) {
            for (PageData p : mPages) {
                if (p.getId() == pageId) {
                    p.removeSection(sectionId);
                    updateSections(p);
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                    return;
                }
            }
        }
    }

    @Nullable
    public SectionData addNewSection(long pageId, String sectionName, boolean removable) {
        synchronized (this) {
            for (PageData page : mPages) {
                if (page.getId() == pageId) {
                    SectionData section = new SectionData(
                            createSectionId(), sectionName,
                            new ArrayList<>(), removable, false);
                    page.addSection(section);
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                    return section;
                }
            }
            return null;
        }
    }

    public List<String> getSectionNames(long pageId) {
        synchronized (this) {
            List<String> sectionNames = new ArrayList<>();
            PageData page = getPage(pageId);
            if (page != null) {
                for (SectionData section : page.getSections()) {
                    sectionNames.add(section.getName());
                }
            }
            return sectionNames;
        }
    }

    public void setSectionName(long pageId, long sectionId, String newSectionName) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page != null) {
                if (page.renameSection(sectionId, newSectionName)) {
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                }
            }
        }
    }

    public void setSectionCollapsed(long pageId, long sectionId, boolean collapsed) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page != null) {
                SectionData section = page.getSection(sectionId);
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

    public void setAllSectionsCollapsed(long pageId, boolean collapsed, boolean save) {
        synchronized (this) {
            boolean isSectionChanged = false;
            PageData page = getPage(pageId);
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

    public void setAllSectionsCollapsed(long pageId,
                                        Map<Long, Boolean> collapsedStates,
                                        boolean save) {
        synchronized (this) {
            boolean isSectionChanged = false;
            PageData page = getPage(pageId);
            if (page != null) {
                for (SectionData section : page.getSections()) {
                    if (!section.isEmpty()) {
                        boolean oldState = section.isCollapsed();
                        boolean newState = collapsedStates.get(section.getId());
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

    public void setSectionOrder(long pageId, List<Long> orderedSectionIds, boolean save) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page != null) {
                List<SectionData> orderedSections = new ArrayList<>();
                for (Long sectionId : orderedSectionIds) {
                    orderedSections.add(page.getSection(sectionId));
                }
                page.setSections(orderedSections);
                EventBus.getDefault().post(new SectionsChangedEvent());
                if (save) {
                    scheduleStoreData();
                }
            }
        }
    }

    public void setStartableCustomName(long pageId, long startableId, String name) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page == null) {
                return;
            }
            StartableData startable = page.getStartable(startableId);
            if (startable == null) {
                return;
            }
            startable.setCustomName(name);
            EventBus.getDefault().post(new SectionsChangedEvent());
            scheduleStoreData();
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

    public void sortStartablesInPage(long pageId) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page != null) {
                for (SectionData sectionData : page.getSections()) {
                    sectionData.sortStartablesAlphabetically();
                }
                EventBus.getDefault().post(new SectionsChangedEvent());
                scheduleStoreData();
            }
        }
    }

    public void sortStartablesInSection(long pageId, long sectionId) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page != null) {
                SectionData sectionData = page.getSection(sectionId);
                if (sectionData != null) {
                    sectionData.sortStartablesAlphabetically();
                    EventBus.getDefault().post(new SectionsChangedEvent());
                    scheduleStoreData();
                }
            }
        }
    }

    public void moveStartableToSection(long pageId, long sectionId, long startableId) {
        synchronized (this) {
            PageData page = getPage(pageId);
            if (page == null) {
                return;
            }
            SectionData fromSection = page.getSectionOfStartable(startableId);
            if (fromSection == null) {
                return;
            }
            StartableData startable = fromSection.getStartable(startableId);
            if (startable == null) {
                return;
            }
            fromSection.removeStartable(startableId);
            SectionData toSection = page.getSection(sectionId);
            if (toSection == null) {
                return;
            }
            toSection.addStartable(startable);
            EventBus.getDefault().post(new SectionsChangedEvent());
            scheduleStoreData();
        }
    }

    public boolean hasInstalledAppShortcut(String packageName, String shortcutId) {
        synchronized (this) {
            for (StartableData startableData : mInstalledStartables) {
                if (startableData instanceof AppShortcutData) {
                    AppShortcutData appShortcutData = (AppShortcutData) startableData;
                    if (appShortcutData.getPackageName().equals(packageName)
                            && appShortcutData.getShortcutId().equals(shortcutId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public void addInstalledShortcut(StartableData startableData, Bitmap shortcutIcon) {
        synchronized (this) {
            mApplistModelStorageV2.storeShortcutIcon(startableData, shortcutIcon);
            mInstalledStartables.add(startableData);

            boolean isSectionChanged = updatePages(mPages);
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
            }

            scheduleStoreData();
        }
    }

    public void removeInstalledShortcut(long shortcutId) {
        synchronized (this) {
            for (StartableData startableData : mInstalledStartables) {
                if (startableData.getId() == shortcutId) {
                    mInstalledStartables.remove(startableData);
                    break;
                }
            }
            mApplistModelStorageV2.deleteShortcutIcon(shortcutId);

            boolean isSectionChanged = updatePages(mPages);
            if (isSectionChanged) {
                EventBus.getDefault().post(new SectionsChangedEvent());
            }

            scheduleStoreData();
        }
    }

    public String getShortcutIconPath(ShortcutData shortcutData) {
        return mApplistModelStorageV2.getShortcutIconFilePath(shortcutData.getId());
    }

    public String getShortcutIconPath(AppShortcutData appShortcutData) {
        return mApplistModelStorageV2.getShortcutIconFilePath(appShortcutData.getId());
    }

    private long createPageId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
    }

    private long createSectionId() {
        return String.valueOf(System.currentTimeMillis()).hashCode();
    }

    private void addUncategorizedSection(PageData page) {
        List<StartableData> uncategorizedItems = new ArrayList<>();
        for (StartableData startableData : mInstalledStartables) {
            if (!page.hasStartable(startableData)) {
                uncategorizedItems.add(startableData);
            }
        }

        Collections.sort(uncategorizedItems, new StartableData.NameComparator());

        SectionData uncategorizedSection = new SectionData(
                createSectionId(), mUncategorizedSectionName, uncategorizedItems, false, false);
        page.addSection(uncategorizedSection);
    }

    private boolean updatePages(List<PageData> pages) {
        boolean isSectionChanged = false;
        for (PageData page : pages) {
            if (updateSections(page)) {
                isSectionChanged = true;
            }
        }
        return isSectionChanged;
    }

    private boolean updateSections(PageData page) {
        if (mInstalledStartables.isEmpty()) {
            return false;
        }

        boolean isSectionChanged = false;
        ArrayList<StartableData> uncategorizedItems = new ArrayList<>(mInstalledStartables);
        SectionData uncategorizedSection = getUncategorizedSection(page);
        for (SectionData section : page.getSections()) {
            if (section != uncategorizedSection) {
                isSectionChanged |= updateSection(section, uncategorizedItems);
                uncategorizedItems.removeAll(section.getStartables());
            }
        }

        if (uncategorizedSection != null) {
            isSectionChanged |= updateSection(uncategorizedSection, uncategorizedItems);
            uncategorizedItems.removeAll(uncategorizedSection.getStartables());
            if (!uncategorizedItems.isEmpty()) {
                Collections.sort(uncategorizedItems, new StartableData.NameComparator());
                uncategorizedSection.addStartables(0, uncategorizedItems);
                isSectionChanged = true;
            }
        }

        return isSectionChanged;
    }

    private SectionData getUncategorizedSection(PageData pageData) {
        return pageData.getSectionByRemovable(false);
    }

    private boolean updateSection(SectionData sectionData, List<StartableData> availableItems) {
        boolean isSectionChanged = false;
        List<StartableData> availableItemsInSection = new ArrayList<>();
        for (StartableData startableData : sectionData.getStartables()) {
            final int availableItemPos = availableItems.indexOf(startableData);
            final boolean isAvailable = (availableItemPos != -1);
            if (isAvailable) {
                StartableData installedStartable = availableItems.get(availableItemPos);

                // The app name may have changed. E.g. The user changed the system
                // language.
                if (!startableData.getName().equals(installedStartable.getName())) {
                    isSectionChanged = true;
                }

                // Keep the custom name.
                installedStartable.setCustomName(startableData.getCustomName());

                availableItemsInSection.add(installedStartable);
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
