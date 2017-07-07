package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import net.feheren_fekete.applist.widgetpage.WidgetUtils;

public class WidgetPickerItem {

    private WidgetPickerData mWidgetPickerData;
    private @Nullable Drawable mIcon;
    private @Nullable Drawable mPreviewImage;
    private @Nullable String mLabel;

    public WidgetPickerItem(WidgetPickerData widgetPickerData) {
        mWidgetPickerData = widgetPickerData;
    }

    public WidgetPickerData getWidgetPickerData() {
        return mWidgetPickerData;
    }

    @Nullable
    public Drawable getIcon(Context context, WidgetUtils widgetUtils) {
        if (mIcon == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            mIcon = widgetUtils.getIcon(context, appWidgetProviderInfo);
        }
        return mIcon;
    }

    @Nullable
    public Drawable getPreviewImage(Context context, WidgetUtils widgetUtils) {
        if (mPreviewImage == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            mPreviewImage = widgetUtils.getPreviewImage(context, appWidgetProviderInfo);
        }
        return mPreviewImage;
    }

    @Nullable
    public String getLabel(Context context, WidgetUtils widgetUtils) {
        if (mLabel == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            mLabel = widgetUtils.getLabel(context, appWidgetProviderInfo);
        }
        return mLabel;
    }

}
