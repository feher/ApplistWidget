package net.feheren_fekete.applist.applistpage;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import net.feheren_fekete.applist.utils.ImageUtils;
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import bolts.Task;

public class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
    private AppItem appItem;
    private WeakReference<ApplistAdapter.StartableItemHolder> appItemHolderRef;
    private PackageManager packageManager;
    private WeakReference<IconCache> iconCacheRef;
    private String cachedIconPath;

    public IconLoaderTask(AppItem appItem,
                          ApplistAdapter.StartableItemHolder startableItemHolder,
                          PackageManager packageManager,
                          IconCache iconCache,
                          String iconCacheDirPath) {
        this.appItem = appItem;
        this.appItemHolderRef = new WeakReference<>(startableItemHolder);
        this.packageManager = packageManager;
        this.iconCacheRef = new WeakReference<>(iconCache);
        this.cachedIconPath = iconCacheDirPath
                + File.separator
                + appItem.getPackageName() + "_" + appItem.getClassName() + ".png";
    }

    public boolean isLoadingFor(AppItem item) {
        return appItem.equals(item);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            Drawable iconDrawable = null;
            if (!isCancelled()) {
                Bitmap iconBitmap = ImageUtils.loadBitmap(cachedIconPath);
                if (iconBitmap != null) {
                    return iconBitmap;
                } else {
                    ComponentName componentName = new ComponentName(
                            appItem.getPackageName(), appItem.getClassName());
                    iconDrawable = packageManager.getActivityIcon(componentName);
                    if (!isCancelled() && iconDrawable != null) {
                        iconBitmap = ImageUtils.drawableToBitmap(iconDrawable);
                        if (isStillValid()) {
                            final Bitmap finalIconBitmap = iconBitmap;
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    ImageUtils.saveBitmap(finalIconBitmap, cachedIconPath);
                                    return null;
                                }
                            });
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
        ApplistAdapter.StartableItemHolder startableItemHolder = appItemHolderRef.get();
        IconCache iconCache = iconCacheRef.get();
        if (iconBitmap != null
                && isStillValid()
                && iconCache != null) {
            String key = iconCache.createKey(appItem);
            iconCache.addIcon(key, iconBitmap);
            startableItemHolder.appIcon.setBackgroundColor(Color.TRANSPARENT);
            startableItemHolder.appIcon.setImageBitmap(iconBitmap);
            startableItemHolder.iconLoader = null;
        }
    }

    private boolean isStillValid() {
        ApplistAdapter.StartableItemHolder startableItemHolder = appItemHolderRef.get();
        return (!isCancelled()
                && startableItemHolder != null
                && startableItemHolder.iconLoader == this);
    }

}
