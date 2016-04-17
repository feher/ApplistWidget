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

import java.lang.ref.WeakReference;

public class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
    private AppItem appItem;
    private WeakReference<ApplistAdapter.AppItemHolder> appItemHolderRef;
    private PackageManager packageManager;
    private WeakReference<IconCache> iconCacheRef;

    public IconLoaderTask(AppItem appItem,
                          ApplistAdapter.AppItemHolder appItemHolder,
                          PackageManager packageManager,
                          IconCache iconCache) {
        this.appItem = appItem;
        this.appItemHolderRef = new WeakReference<>(appItemHolder);
        this.packageManager = packageManager;
        this.iconCacheRef = new WeakReference<>(iconCache);
    }

    public boolean isLoadingFor(AppItem item) {
        return appItem.equals(item);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            Drawable iconDrawable = null;
            if (!isCancelled()) {
                iconDrawable = packageManager.getActivityIcon(
                        new ComponentName(appItem.getPackageName(), appItem.getComponentName()));
            } else {
                return null;
            }
            if (!isCancelled() && iconDrawable != null) {
                return drawableToBitmap(iconDrawable);
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
                && appItemHolder != null
                && iconCache != null
                && appItemHolder.iconLoader == this
                && !isCancelled()) {
            String key = iconCache.createKey(appItem);
            iconCache.addIcon(key, iconBitmap);
            appItemHolder.appIcon.setBackgroundColor(Color.TRANSPARENT);
            appItemHolder.appIcon.setImageBitmap(iconBitmap);
            appItemHolder.iconLoader = null;
        }

    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
