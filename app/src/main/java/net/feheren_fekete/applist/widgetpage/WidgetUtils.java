package net.feheren_fekete.applist.widgetpage;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

import net.feheren_fekete.applist.ApplistLog;

import androidx.annotation.Nullable;

public class WidgetUtils {

    @Nullable
    public Drawable getIcon(Context context, AppWidgetProviderInfo appWidgetProviderInfo) {
        Drawable result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                result = appWidgetProviderInfo.loadIcon(context, 0);
            } catch (OutOfMemoryError e) {
                ApplistLog.getInstance().log(e);
            }
        } else {
            try {
                PackageManager manager = context.getPackageManager();
                Resources resources = manager.getResourcesForApplication(appWidgetProviderInfo.provider.getPackageName());
                result = resources.getDrawable(appWidgetProviderInfo.icon);
            } catch (Exception e) {
                ApplistLog.getInstance().log(e);
            }
        }
        return result;
    }

    @Nullable
    public Drawable getPreviewImage(Context context, AppWidgetProviderInfo appWidgetProviderInfo) {
        Drawable result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                result = appWidgetProviderInfo.loadPreviewImage(context, 0);
            } catch (OutOfMemoryError e) {
                ApplistLog.getInstance().log(e);
            }
        } else {
            try {
                PackageManager manager = context.getPackageManager();
                Resources resources = manager.getResourcesForApplication(appWidgetProviderInfo.provider.getPackageName());
                result = resources.getDrawable(appWidgetProviderInfo.icon);
            } catch (Exception e) {
                ApplistLog.getInstance().log(e);
            }
        }
        return result;
    }

    @Nullable
    public String getLabel(Context context, AppWidgetProviderInfo appWidgetProviderInfo) {
        String result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            result = appWidgetProviderInfo.loadLabel(context.getPackageManager());
        } else {
            result = appWidgetProviderInfo.label;
        }
        return result;
    }


}
