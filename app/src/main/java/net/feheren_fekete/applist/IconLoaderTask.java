package net.feheren_fekete.applist;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import net.feheren_fekete.applist.viewmodel.AppItem;

import java.io.File;
import java.lang.ref.WeakReference;

public class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
    private AppItem appItem;
    private WeakReference<ApplistAdapter.AppItemHolder> appItemHolderRef;
    private PackageManager packageManager;
    private WeakReference<IconCache> iconCacheRef;
    private String iconCacheDirPath;

    public IconLoaderTask(AppItem appItem,
                          ApplistAdapter.AppItemHolder appItemHolder,
                          PackageManager packageManager,
                          IconCache iconCache,
                          String iconCacheDirPath) {
        this.appItem = appItem;
        this.appItemHolderRef = new WeakReference<>(appItemHolder);
        this.packageManager = packageManager;
        this.iconCacheRef = new WeakReference<>(iconCache);
        this.iconCacheDirPath = iconCacheDirPath;
    }

    public boolean isLoadingFor(AppItem item) {
        return appItem.equals(item);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            Drawable iconDrawable = null;
            if (!isCancelled()) {
                String iconFilePath = iconCacheDirPath
                        + File.separator
                        + appItem.getPackageName() + "_" + appItem.getComponentName() + ".png";
                Bitmap iconBitmap = ImageUtils.loadBitmap(iconFilePath);
                if (iconBitmap != null) {
                    return iconBitmap;
                } else {
                    ComponentName componentName = new ComponentName(
                            appItem.getPackageName(), appItem.getComponentName());
                    iconDrawable = packageManager.getActivityIcon(componentName);
                    if (!isCancelled() && iconDrawable != null) {
                        iconBitmap = ImageUtils.drawableToBitmap(iconDrawable);
                        if (isStillValid()) {
                            ImageUtils.saveBitmap(iconBitmap, iconFilePath);
                        }
                        return iconBitmap;
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap iconBitmap) {
        ApplistAdapter.AppItemHolder appItemHolder = appItemHolderRef.get();
        IconCache iconCache = iconCacheRef.get();
        if (iconBitmap != null
                && isStillValid()
                && iconCache != null) {
            String key = iconCache.createKey(appItem);
            iconCache.addIcon(key, iconBitmap);
            appItemHolder.appIcon.setBackgroundColor(Color.TRANSPARENT);
            appItemHolder.appIcon.setImageBitmap(iconBitmap);
            appItemHolder.iconLoader = null;
        }
    }

    private boolean isStillValid() {
        ApplistAdapter.AppItemHolder appItemHolder = appItemHolderRef.get();
        return (!isCancelled()
                && appItemHolder != null
                && appItemHolder.iconLoader == this);
    }

}
