package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import net.feheren_fekete.applistwidget.model.ApplistApp;
import net.feheren_fekete.applistwidget.model.ApplistPage;
import net.feheren_fekete.applistwidget.model.ApplistSection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplistModel {
    private PackageManager mPackageManager;
    private String mDatabasePath;

    public ApplistModel(Context context, PackageManager packageManager) {
        mPackageManager = packageManager;
        mDatabasePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist.json";
    }

    public List<ApplistPage> loadAllData() {
        List<ApplistApp> installedApps = getAllApps();
        List<ApplistPage> pages = loadPages();
        for (ApplistPage page : pages) {
            addOtherSection(page, installedApps);
        }
        return pages;
    }

    private void addOtherSection(ApplistPage page, List<ApplistApp> installedApps) {
        List<ApplistApp> otherApps = new ArrayList<>();
        for (ApplistApp app : installedApps) {
            if (!page.contains(app)) {
                otherApps.add(app);
            }
        }
        ApplistSection otherSection = new ApplistSection("Other", otherApps);
        page.addSection(otherSection);
    }

    private List<ApplistPage> loadPages() {
        List<ApplistPage> pages = new ArrayList<>();

        String fileContent = readFile(mDatabasePath);
        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            JSONArray jsonPages = jsonObject.getJSONArray("pages");
            for (int i = 0; i < jsonPages.length(); ++i) {
                JSONObject jsonPage = jsonPages.getJSONObject(i);

                List<ApplistSection> sections = new ArrayList<>();
                JSONArray jsonSections = jsonPage.getJSONArray("sections");
                for (int j = 0; j < jsonSections.length(); ++j) {
                    JSONObject jsonSection = jsonSections.getJSONObject(j);

                    List<ApplistApp> apps = new ArrayList<>();
                    JSONArray jsonApps = jsonSection.getJSONArray("apps");
                    for (int k = 0; k < jsonApps.length(); ++k) {
                        JSONObject jsonApp = jsonApps.getJSONObject(k);
                        ApplistApp app = new ApplistApp(jsonApp.getString("package-name"));
                        apps.add(app);
                    }

                    ApplistSection section = new ApplistSection(jsonSection.getString("name"), apps);
                    sections.add(section);
                }

                ApplistPage page = new ApplistPage(jsonPage.getString("name"), sections);
                pages.add(page);
            }
        } catch (JSONException e) {
            return Collections.emptyList();
        }

        return pages;
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

    public List<ApplistApp> getAllApps() {
        List<ApplistApp> packageNames = new ArrayList<>();
        List<ApplicationInfo> installedApps = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : installedApps) {
            packageNames.add(new ApplistApp(appInfo.packageName));
        }
        return packageNames;
    }

}
