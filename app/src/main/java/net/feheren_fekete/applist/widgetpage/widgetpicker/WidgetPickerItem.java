package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;

import net.feheren_fekete.applist.ApplistLog;

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
    public Drawable getIcon(Context context) {
        if (mIcon == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mIcon = appWidgetProviderInfo.loadIcon(context, 0);
            } else {
                try {
                    PackageManager manager = context.getPackageManager();
                    Resources resources = manager.getResourcesForApplication(appWidgetProviderInfo.provider.getPackageName());
                    mIcon = resources.getDrawable(appWidgetProviderInfo.icon);
                } catch (Exception e) {
                    ApplistLog.getInstance().log(e);
                }
            }
        }
        return mIcon;
    }

    @Nullable
    public Drawable getPreviewImage(Context context) {
        if (mPreviewImage == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPreviewImage = appWidgetProviderInfo.loadPreviewImage(context, 0);
            } else {
                try {
                    PackageManager manager = context.getPackageManager();
                    Resources resources = manager.getResourcesForApplication(appWidgetProviderInfo.provider.getPackageName());
                    mPreviewImage = resources.getDrawable(appWidgetProviderInfo.icon);
                } catch (Exception e) {
                    ApplistLog.getInstance().log(e);
                }
            }
        }
        return mPreviewImage;
    }

    @Nullable
    public String getLabel(Context context) {
        if (mLabel == null) {
            AppWidgetProviderInfo appWidgetProviderInfo = getWidgetPickerData().getAppWidgetProviderInfo();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLabel = appWidgetProviderInfo.loadLabel(context.getPackageManager());
            } else {
                mLabel = appWidgetProviderInfo.label;
            }
        }
        return mLabel;
    }

}
