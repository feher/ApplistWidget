package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.appwidget.AppWidgetProviderInfo;

public class WidgetPickerData {

    private AppWidgetProviderInfo mAppWidgetProviderInfo;

    public WidgetPickerData(AppWidgetProviderInfo appWidgetProviderInfo) {
        mAppWidgetProviderInfo = appWidgetProviderInfo;
    }

    public AppWidgetProviderInfo getAppWidgetProviderInfo() {
        return mAppWidgetProviderInfo;
    }

}
