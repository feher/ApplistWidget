package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class WidgetPickerModel {

    public static final class DataLoadedEvent {}

    private Context mContext;
    private List<WidgetPickerData> mWidgets = new ArrayList<>();

    public WidgetPickerModel(Context context) {
        mContext = context;
    }

    public void loadData() {
        synchronized (this) {
            mWidgets.clear();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            List<AppWidgetProviderInfo> appWidgetProviderInfos = appWidgetManager.getInstalledProviders();
            for (AppWidgetProviderInfo appWidgetProviderInfo : appWidgetProviderInfos) {
                mWidgets.add(new WidgetPickerData(appWidgetProviderInfo));
            }
            EventBus.getDefault().post(new DataLoadedEvent());
        }
    }

    public List<WidgetPickerData> getWidgets() {
        synchronized (this) {
            return mWidgets;
        }
    }

}
