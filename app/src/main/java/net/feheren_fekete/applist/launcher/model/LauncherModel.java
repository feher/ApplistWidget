package net.feheren_fekete.applist.launcher.model;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public class LauncherModel {

    public static final class DataLoadedEvent {}

    public static final class PageAddedEvent {
        public final PageData pageData;
        public PageAddedEvent(PageData pageData) {
            this.pageData = pageData;
        }
    }

    private static LauncherModel sInstance;

    private String mPagesFilePath;
    private Handler mHandler = new Handler();
    private FileUtils mFileUtils = new FileUtils();
    private List<PageData> mPages = new ArrayList<>();

    public static void initInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LauncherModel(context);
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    sInstance.loadData();
                    return null;
                }
            });
        }
    }

    public static LauncherModel getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(LauncherModel.class.getSimpleName() + " singleton is not initialized");
        }
    }

    private LauncherModel(Context context) {
        mPagesFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-pages.json";
    }

    public void loadData() {
        synchronized (this) {
            mPages.clear();
            String fileContent = mFileUtils.readFile(mPagesFilePath);
            if (!TextUtils.isEmpty(fileContent)) {
                try {
                    JSONObject jsonObject = new JSONObject(fileContent);

                    JSONArray jsonWidgets = jsonObject.getJSONArray("pages");
                    for (int k = 0; k < jsonWidgets.length(); ++k) {
                        JSONObject jsonWidget = jsonWidgets.getJSONObject(k);
                        PageData widgetData = new PageData(
                                jsonWidget.getLong("id"),
                                jsonWidget.getInt("type"),
                                jsonWidget.getBoolean("is-main-page"));
                        mPages.add(widgetData);
                    }
                } catch (JSONException e) {
                    ApplistLog.getInstance().log(e);
                }
            }
            // Always add at least an Applist page.
            if (mPages.isEmpty()) {
                mPages.add(new PageData(System.currentTimeMillis(), PageData.TYPE_APPLIST_PAGE, true));
                scheduleStoreData();
            }
            EventBus.getDefault().post(new DataLoadedEvent());
        }
    }

    public List<PageData> getPages() {
        synchronized (this) {
            // Return a copy to avoid messing up the data by the caller.
            List<PageData> result = new ArrayList<>();
            for (PageData pageData : mPages) {
                result.add(new PageData(pageData));
            }
            return result;
        }
    }

    public void addPage(PageData pageData) {
        synchronized (this) {
            mPages.add(pageData);
            scheduleStoreData();
            EventBus.getDefault().post(new PageAddedEvent(pageData));
        }
    }

    private void storeData() {
        synchronized (this) {
            String data = "";
            JSONObject jsonObject = new JSONObject();
            try {
                JSONArray jsonWidgets = new JSONArray();
                for (PageData pageData : mPages) {
                    JSONObject jsonWidget = new JSONObject();
                    jsonWidget.put("id", pageData.getId());
                    jsonWidget.put("type", pageData.getType());
                    jsonWidget.put("is-main-page", pageData.isMainPage());
                    jsonWidgets.put(jsonWidget);
                }
                jsonObject.put("pages", jsonWidgets);
                data = jsonObject.toString(2);
            } catch (JSONException e) {
                ApplistLog.getInstance().log(e);
                return;
            }

            mFileUtils.writeFile(mPagesFilePath, data);
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
