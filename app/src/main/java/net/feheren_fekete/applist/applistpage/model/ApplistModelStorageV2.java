package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ApplistModelStorageV2 {

    private static final String JSON_PAGES = "pages";
    private static final String JSON_PAGE_ID = "id";
    private static final String JSON_PAGE_NAME = "name";

    private static final String JSON_SECTIONS = "sections";
    private static final String JSON_SECTION_ID = "id";
    private static final String JSON_SECTION_NAME = "name";

    private static final String JSON_SECTION_IS_REMOVABLE = "is-removable";
    private static final String JSON_SECTION_IS_COLLAPSED = "is-collapsed";

    private static final String JSON_INSTALLED_STARTABLES = "installed-apps";
    private static final String JSON_STARTABLES = "startables";
    private static final String JSON_STARTABLE_ID = "id";
    private static final String JSON_STARTABLE_TYPE = "type";
    private static final String JSON_STARTABLE_TYPE_APP = "app";
    private static final String JSON_STARTABLE_TYPE_SHORTCUT = "shortcut";
    private static final String JSON_STARTABLE_TYPE_APP_SHORTCUT = "app-shortcut";
    private static final String JSON_STARTABLE_NAME = "name";
    private static final String JSON_STARTABLE_CUSTOM_NAME = "custom-name";
    private static final String JSON_APP_PACKAGE_NAME = "package-name";
    private static final String JSON_APP_CLASS_NAME = "class-name";
    private static final String JSON_SHORTCUT_INTENT = "intent";
    private static final String JSON_APP_SHORTCUT_ID = "shortcut-id";

    private FileUtils mFileUtils = new FileUtils();

    private String mPagesFilePath;
    private String mInstalledStartablesFilePath;
    private String mShortcutIconsDirPath;

    public ApplistModelStorageV2(Context context) {
        mPagesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-pages-v2.json";
        mInstalledStartablesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-installed-startables-v2.json";
        mShortcutIconsDirPath = context.getFilesDir().getAbsolutePath() + File.separator + "shortcut-icons-v2";
    }

    public boolean exists() {
        File file = new File(mPagesFilePath);
        return file.exists();
    }

    public String getShortcutIconFilePath(long shortcutId) {
        return mShortcutIconsDirPath + File.separator + "shortcut-icon-" + shortcutId + ".png";
    }

    public void storeShortcutIcon(StartableData startableData, Bitmap shortcutIcon) {
        ImageUtils.saveBitmap(
                shortcutIcon,
                getShortcutIconFilePath(startableData.getId()));
    }

    public void deleteShortcutIcon(long shortcutId) {
        new File(getShortcutIconFilePath(shortcutId)).delete();
    }

    public List<StartableData> loadInstalledStartables() {
        List<StartableData> installedStartables = new ArrayList<>();
        String fileContent = mFileUtils.readFile(mInstalledStartablesFilePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);
            JSONArray jsonInstalledStartables = jsonObject.getJSONArray(JSON_INSTALLED_STARTABLES);
            for (int k = 0; k < jsonInstalledStartables.length(); ++k) {
                JSONObject jsonStartable = jsonInstalledStartables.getJSONObject(k);
                installedStartables.add(loadStartable(jsonStartable));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return installedStartables;
    }

    public List<PageData> loadPages() {
        List<PageData> pages = new ArrayList<>();
        String fileContent = mFileUtils.readFile(mPagesFilePath);
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

        return new PageData(jsonPage.getLong(JSON_PAGE_ID), jsonPage.getString(JSON_PAGE_NAME), sections);
    }

    private SectionData loadSection(JSONObject jsonSection) throws JSONException {
        List<StartableData> startableDatas = new ArrayList<>();
        JSONArray jsonStartables = jsonSection.getJSONArray(JSON_STARTABLES);
        for (int k = 0; k < jsonStartables.length(); ++k) {
            JSONObject jsonStartable = jsonStartables.getJSONObject(k);
            startableDatas.add(loadStartable(jsonStartable));
        }
        return new SectionData(
                jsonSection.getLong(JSON_SECTION_ID),
                jsonSection.getString(JSON_SECTION_NAME),
                startableDatas,
                jsonSection.optBoolean(JSON_SECTION_IS_REMOVABLE, true),
                jsonSection.optBoolean(JSON_SECTION_IS_COLLAPSED, false));
    }

    private StartableData loadStartable(JSONObject jsonStartable) throws JSONException {
        final String type = jsonStartable.getString(JSON_STARTABLE_TYPE);
        if (JSON_STARTABLE_TYPE_APP.equals(type)) {
            AppData appData = new AppData(
                    jsonStartable.getLong(JSON_STARTABLE_ID),
                    jsonStartable.getString(JSON_APP_PACKAGE_NAME),
                    jsonStartable.getString(JSON_APP_CLASS_NAME),
                    jsonStartable.getString(JSON_STARTABLE_NAME),
                    jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME));
            return appData;
        } else if (type.equals(JSON_STARTABLE_TYPE_SHORTCUT)) {
            try {
                ShortcutData shortcutData = new ShortcutData(
                        jsonStartable.getLong(JSON_STARTABLE_ID),
                        jsonStartable.getString(JSON_STARTABLE_NAME),
                        jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME),
                        Intent.parseUri(jsonStartable.getString(JSON_SHORTCUT_INTENT), 0));
                return shortcutData;
            } catch (URISyntaxException e) {
                ApplistLog.getInstance().log(e);
                throw new JSONException(e.getMessage());
            }
        } else if (type.equals(JSON_STARTABLE_TYPE_APP_SHORTCUT)) {
            AppShortcutData appShortcutData = new AppShortcutData(
                    jsonStartable.getLong(JSON_STARTABLE_ID),
                    jsonStartable.getString(JSON_STARTABLE_NAME),
                    jsonStartable.optString(JSON_STARTABLE_CUSTOM_NAME),
                    jsonStartable.getString(JSON_APP_PACKAGE_NAME),
                    jsonStartable.getString(JSON_APP_SHORTCUT_ID));
            return appShortcutData;
        } else {
            throw new RuntimeException("Unknown type startable " + type);
        }
    }

    public void storePages(List<PageData> pages) {
        String data = "";
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonPages = new JSONArray();
            for (PageData page : pages) {
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

                    JSONArray jsonStartables = createStartablesArray(section.getStartables());
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

        mFileUtils.writeFile(mPagesFilePath, data);
    }

    public void storeInstalledStartables(List<StartableData> installedStartables) {
        String data = "";
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonApps = createStartablesArray(installedStartables);
            jsonObject.put(JSON_INSTALLED_STARTABLES, jsonApps);
            data = jsonObject.toString(2);
        } catch (JSONException e) {
            ApplistLog.getInstance().log(e);
            return;
        }

        mFileUtils.writeFile(mInstalledStartablesFilePath, data);
    }

    private JSONArray createStartablesArray(List<StartableData> startableDatas) throws JSONException {
        JSONArray jsonStartables = new JSONArray();
        for (StartableData startableData : startableDatas) {
            if (startableData instanceof AppData) {
                AppData app = (AppData) startableData;
                JSONObject jsonApp = new JSONObject();
                jsonApp.put(JSON_STARTABLE_ID, app.getId());
                jsonApp.put(JSON_STARTABLE_TYPE, JSON_STARTABLE_TYPE_APP);
                jsonApp.put(JSON_STARTABLE_NAME, app.getName());
                jsonApp.put(JSON_STARTABLE_CUSTOM_NAME, app.getCustomName());
                jsonApp.put(JSON_APP_PACKAGE_NAME, app.getPackageName());
                jsonApp.put(JSON_APP_CLASS_NAME, app.getClassName());
                jsonStartables.put(jsonApp);
            } else if (startableData instanceof ShortcutData) {
                ShortcutData shortcut = (ShortcutData) startableData;
                JSONObject jsonShortcut = new JSONObject();
                jsonShortcut.put(JSON_STARTABLE_ID, shortcut.getId());
                jsonShortcut.put(JSON_STARTABLE_TYPE, JSON_STARTABLE_TYPE_SHORTCUT);
                jsonShortcut.put(JSON_STARTABLE_NAME, shortcut.getName());
                jsonShortcut.put(JSON_STARTABLE_CUSTOM_NAME, shortcut.getCustomName());
                jsonShortcut.put(JSON_SHORTCUT_INTENT, shortcut.getIntent().toUri(0));
                jsonStartables.put(jsonShortcut);
            } else if (startableData instanceof AppShortcutData) {
                AppShortcutData appShortcut = (AppShortcutData) startableData;
                JSONObject jsonAppShortcut = new JSONObject();
                jsonAppShortcut.put(JSON_STARTABLE_ID, appShortcut.getId());
                jsonAppShortcut.put(JSON_STARTABLE_TYPE, JSON_STARTABLE_TYPE_APP_SHORTCUT);
                jsonAppShortcut.put(JSON_STARTABLE_NAME, appShortcut.getName());
                jsonAppShortcut.put(JSON_STARTABLE_CUSTOM_NAME, appShortcut.getCustomName());
                jsonAppShortcut.put(JSON_APP_PACKAGE_NAME, appShortcut.getPackageName());
                jsonAppShortcut.put(JSON_APP_SHORTCUT_ID, appShortcut.getShortcutId());
                jsonStartables.put(jsonAppShortcut);
            }
        }
        return jsonStartables;
    }

}
