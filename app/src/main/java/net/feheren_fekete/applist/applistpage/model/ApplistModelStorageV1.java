package net.feheren_fekete.applist.applistpage.model;

import android.content.Context;

import net.feheren_fekete.applist.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApplistModelStorageV1 {

    private static final String JSON_PAGES = "pages";
    private static final String JSON_PAGE_ID = "id";
    private static final String JSON_PAGE_NAME = "name";

    private static final String JSON_SECTIONS = "sections";
    private static final String JSON_SECTION_ID = "id";
    private static final String JSON_SECTION_NAME = "name";

    private static final String JSON_SECTION_IS_REMOVABLE = "is-removable";
    private static final String JSON_SECTION_IS_COLLAPSED = "is-collapsed";

    private static final String JSON_APPS = "apps";
    private static final String JSON_APPS_ID = "id";
    private static final String JSON_APP_PACKAGE_NAME = "package-name";
    private static final String JSON_APP_CLASS_NAME = "component-name";
    private static final String JSON_APP_VERSION_CODE = "version-code";
    private static final String JSON_APP_NAME = "app-name";

    private static final String JSON_INSTALLED_APPS = "installed-apps";
    private static final String JSON_INSTALLED_APP_ID = "id";
    private static final String JSON_INSTALLED_APP_PACKAGE_NAME = "package-name";
    private static final String JSON_INSTALLED_APP_CLASS_NAME = "component-name";
    private static final String JSON_INSTALLED_APP_VERSION_CODE = "version-code";
    private static final String JSON_INSATLLED_APP_NAME = "app-name";

    private FileUtils mFileUtils = new FileUtils();

    private String mPagesFilePath;
    private String mInstalledAppsFilePath;

    public ApplistModelStorageV1(Context context) {
        mPagesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-pages.json";
        mInstalledAppsFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-installed-apps.json";
    }

    public boolean exists() {
        File file = new File(mPagesFilePath);
        return file.exists();
    }

    public void delete() {
        new File(mPagesFilePath).delete();
        new File(mInstalledAppsFilePath).delete();
    }

    public List<StartableData> loadInstalledApps() {
        List<StartableData> installedApps = new ArrayList<>();
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
                        jsonApp.optLong(JSON_INSTALLED_APP_VERSION_CODE),
                        jsonApp.getString(JSON_INSATLLED_APP_NAME),
                        "");
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

        return new PageData(ApplistModel.INVALID_ID, jsonPage.getString(JSON_PAGE_NAME), sections);
    }

    private SectionData loadSection(JSONObject jsonSection) throws JSONException {
        List<StartableData> appDatas = new ArrayList<>();
        JSONArray jsonApps = jsonSection.getJSONArray(JSON_APPS);
        for (int k = 0; k < jsonApps.length(); ++k) {
            JSONObject jsonApp = jsonApps.getJSONObject(k);
            AppData app = new AppData(
                    jsonApp.getLong(JSON_APPS_ID),
                    jsonApp.getString(JSON_APP_PACKAGE_NAME),
                    jsonApp.getString(JSON_APP_CLASS_NAME),
                    jsonApp.optLong(JSON_APP_VERSION_CODE),
                    jsonApp.getString(JSON_APP_NAME),
                    "");
            appDatas.add(app);
        }
        return new SectionData(
                jsonSection.getLong(JSON_SECTION_ID),
                jsonSection.getString(JSON_SECTION_NAME),
                appDatas,
                jsonSection.optBoolean(JSON_SECTION_IS_REMOVABLE, true),
                jsonSection.optBoolean(JSON_SECTION_IS_COLLAPSED, false));
    }

}
