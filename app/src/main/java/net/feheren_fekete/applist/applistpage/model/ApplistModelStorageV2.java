package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;
import android.content.Intent;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.utils.FileUtils;

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

    private static final String JSON_STARTABLES = "startables";
    private static final String JSON_STARTABLE_ID = "id";
    private static final String JSON_STARTABLE_TYPE = "type";
    private static final String JSON_STARTABLE_TYPE_APP = "app";
    private static final String JSON_STARTABLE_TYPE_SHORTCUT = "shortcut";
    private static final String JSON_STARTABLE_NAME = "name";
    private static final String JSON_APP_PACKAGE_NAME = "package-name";
    private static final String JSON_APP_CLASS_NAME = "class-name";
    private static final String JSON_SHORTCUT_INTENT = "intent";

    private static final String JSON_INSTALLED_APPS = "installed-apps";
    private static final String JSON_INSTALLED_APP_ID = "id";
    private static final String JSON_INSTALLED_APP_PACKAGE_NAME = "package-name";
    private static final String JSON_INSTALLED_APP_CLASS_NAME = "class-name";
    private static final String JSON_INSATLLED_APP_NAME = "app-name";

    private FileUtils mFileUtils = new FileUtils();

    private String mPagesFilePath;
    private String mInstalledAppsFilePath;

    public ApplistModelStorageV2(Context context) {
        mPagesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-pages-v2.json";
        mInstalledAppsFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-installed-apps-v2.json";
    }

    public boolean exists() {
        File file = new File(mPagesFilePath);
        return file.exists();
    }

    public List<AppData> loadInstalledApps() {
        List<AppData> installedApps = new ArrayList<>();
        String fileContent = mFileUtils.readFile(mInstalledAppsFilePath);
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

        mFileUtils.writeFile(mPagesFilePath, data);
    }

    public void storeInstalledApps(List<AppData> installedApps) {
        String data = "";
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonApps = new JSONArray();
            for (AppData app : installedApps) {
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

        mFileUtils.writeFile(mInstalledAppsFilePath, data);
    }

}
