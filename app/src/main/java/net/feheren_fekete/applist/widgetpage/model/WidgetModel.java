package net.feheren_fekete.applist.widgetpage.model;

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

import androidx.annotation.Nullable;
import bolts.Task;

public class WidgetModel {

    private static final String TAG = WidgetModel.class.getSimpleName();

    public static final class DataLoadedEvent {}

    public static final class WidgetsChangedEvent {}

    public static class BaseWidgetEvent {
        public final WidgetData widgetData;
        private BaseWidgetEvent(WidgetData widgetData) {
            this.widgetData = widgetData;
        }
    }

    public static final class WidgetAddedEvent extends BaseWidgetEvent {
        private WidgetAddedEvent(WidgetData widgetData) {
            super(widgetData);
        }
    }

    public static final class WidgetDeletedEvent extends BaseWidgetEvent {
        private WidgetDeletedEvent(WidgetData widgetData) {
            super(widgetData);
        }
    }

    public static final class WidgetChangedEvent extends BaseWidgetEvent {
        private WidgetChangedEvent(WidgetData widgetData) {
            super(widgetData);
        }
    }

    private String mWidgetsFilePath;
    private Handler mHandler = new Handler();
    private FileUtils mFileUtils = new FileUtils();
    private List<WidgetData> mWidgets = new ArrayList<>();

    public WidgetModel(Context context) {
        mWidgetsFilePath = context.getFilesDir().getAbsolutePath() + File.separator + "applist-widgets.json";

        Task.callInBackground((Callable<Void>) () -> {
            loadData();
            return null;
        });
    }

    private void loadData() {
        synchronized (this) {
            mWidgets.clear();
            String fileContent = mFileUtils.readFile(mWidgetsFilePath);
            if (!TextUtils.isEmpty(fileContent)) {
                try {
                    JSONObject jsonObject = new JSONObject(fileContent);

                    JSONArray jsonWidgets = jsonObject.getJSONArray("widgets");
                    for (int k = 0; k < jsonWidgets.length(); ++k) {
                        JSONObject jsonWidget = jsonWidgets.getJSONObject(k);
                        WidgetData widgetData = new WidgetData(
                                jsonWidget.getLong("id"),
                                jsonWidget.getInt("app-widget-id"),
                                jsonWidget.getString("package-name"),
                                jsonWidget.getString("class-name"),
                                jsonWidget.getLong("page-id"),
                                jsonWidget.getInt("position-x"),
                                jsonWidget.getInt("position-y"),
                                jsonWidget.getInt("width"),
                                jsonWidget.getInt("height"));
                        mWidgets.add(widgetData);
                    }

                } catch (JSONException e) {
                    ApplistLog.getInstance().log(e);
                }
            }
            EventBus.getDefault().post(new DataLoadedEvent());
        }
    }

    public List<WidgetData> getWidgets() {
        synchronized (this) {
            // Return a copy to avoid messing up the data by the caller.
            List<WidgetData> result = new ArrayList<>();
            for (WidgetData widgetData : mWidgets) {
                result.add(new WidgetData(widgetData));
            }
            return result;
        }
    }

    public List<WidgetData> getWidgets(long pageId) {
        synchronized (this) {
            // Return a copy to avoid messing up the data by the caller.
            List<WidgetData> result = new ArrayList<>();
            for (WidgetData widgetData : mWidgets) {
                if (widgetData.getPageId() == pageId) {
                    result.add(new WidgetData(widgetData));
                }
            }
            return result;
        }
    }

    public void addWidget(WidgetData widgetData) {
        synchronized (this) {
            mWidgets.add(widgetData);
            scheduleStoreData();
            EventBus.getDefault().post(new WidgetAddedEvent(widgetData));
        }
    }

    public void updateWidget(WidgetData widgetData, boolean sendEvent) {
        synchronized (this) {
            WidgetData existingWidgetData = getWidgetData(widgetData);
            if (existingWidgetData != null) {
                existingWidgetData.updateFrom(widgetData);
                scheduleStoreData();
                if (sendEvent) {
                    EventBus.getDefault().post(new WidgetChangedEvent(widgetData));
                }
            }
        }
    }

    public void bringWidgetToTop(WidgetData widgetData) {
        synchronized (this) {
            WidgetData existingWidgetData = getWidgetData(widgetData);
            if (existingWidgetData != null) {
                mWidgets.remove(existingWidgetData);
                mWidgets.add(existingWidgetData);
                scheduleStoreData();
                EventBus.getDefault().post(new WidgetChangedEvent(widgetData));
            }
        }
    }

    public void deleteWidget(WidgetData widgetData) {
        synchronized (this) {
            for (WidgetData widget : mWidgets) {
                if (widget.getId() == widgetData.getId()) {
                    mWidgets.remove(widget);
                    scheduleStoreData();
                    EventBus.getDefault().post(new WidgetDeletedEvent(widgetData));
                    return;
                }
            }
        }
    }

    public void deleteWidget(int appWidgetId) {
        synchronized (this) {
            for (WidgetData widget : mWidgets) {
                if (widget.getAppWidgetId() == appWidgetId) {
                    mWidgets.remove(widget);
                    scheduleStoreData();
                    EventBus.getDefault().post(new WidgetDeletedEvent(widget));
                    return;
                }
            }
        }
    }

    public List<Integer> deleteWidgetsOfPage(long pageId) {
        synchronized (this) {
            List<WidgetData> widgetsOfPage = new ArrayList<>();
            List<Integer> deletedWidgetIds = new ArrayList<>();
            for (WidgetData widget : mWidgets) {
                if (widget.getPageId() == pageId) {
                    widgetsOfPage.add(widget);
                    deletedWidgetIds.add(widget.getAppWidgetId());
                }
            }
            mWidgets.removeAll(widgetsOfPage);
            scheduleStoreData();
            EventBus.getDefault().post(new WidgetsChangedEvent());
            return deletedWidgetIds;
        }
    }

    @Nullable
    private WidgetData getWidgetData(WidgetData widgetData) {
        for (WidgetData widget : mWidgets) {
            if (widget.getId() == widgetData.getId()) {
                return widget;
            }
        }
        return null;
    }

    private void storeData() {
        synchronized (this) {
            String data = "";
            JSONObject jsonObject = new JSONObject();
            try {
                JSONArray jsonWidgets = new JSONArray();
                for (WidgetData widgetData : mWidgets) {
                    JSONObject jsonWidget = new JSONObject();
                    jsonWidget.put("id", widgetData.getId());
                    jsonWidget.put("app-widget-id", widgetData.getAppWidgetId());
                    jsonWidget.put("package-name", widgetData.getProviderPackage());
                    jsonWidget.put("class-name", widgetData.getProviderClass());
                    jsonWidget.put("page-id", widgetData.getPageId());
                    jsonWidget.put("position-x", widgetData.getPositionX());
                    jsonWidget.put("position-y", widgetData.getPositionY());
                    jsonWidget.put("width", widgetData.getWidth());
                    jsonWidget.put("height", widgetData.getHeight());
                    jsonWidgets.put(jsonWidget);
                }
                jsonObject.put("widgets", jsonWidgets);
                data = jsonObject.toString(2);
            } catch (JSONException e) {
                ApplistLog.getInstance().log(e);
                return;
            }

            mFileUtils.writeFile(mWidgetsFilePath, data);
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
