package net.feheren_fekete.applist.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.view.View;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.utils.ScreenUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import bolts.Task;

import static org.koin.java.KoinJavaComponent.get;

public class ScreenshotUtils {

    public static final int DELAY_SHORT = 200;
    public static final int DELAY_LONG = 1000;

    private LauncherStateManager mLauncherStateManager = get(LauncherStateManager.class);
    private ScreenUtils mScreenUtils = get(ScreenUtils.class);

    private Handler mHandler = new Handler();
    private WeakReference<Activity> mActivityRef = new WeakReference<>(null);
    private long mPageId;

    private Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot(mActivityRef, mPageId);
        }
    };

    public void scheduleScreenshot(Activity activity, long pageId, int delayMillis) {
        if (mLauncherStateManager.isPageVisible(pageId)) {
            mActivityRef = new WeakReference<>(activity);
            mPageId = pageId;
            mHandler.removeCallbacks(mScreenshotRunnable);
            mHandler.postDelayed(mScreenshotRunnable, delayMillis);
        }
    }

    public void cancelScheduledScreenshot() {
        mActivityRef.clear();
        mPageId = PageData.INVALID_PAGE_ID;
        mHandler.removeCallbacks(mScreenshotRunnable);
    }

    public String createScreenshotPath(Context context, long pageId) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "applist-page-" + pageId + ".png";
    }

    private void takeScreenshot(WeakReference<Activity> activityRef, long pageId) {
        Activity activity = activityRef.get();
        if (activity == null || pageId == PageData.INVALID_PAGE_ID || !mLauncherStateManager.isPageVisible(pageId)) {
            return;
        }

        final View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        final View screenView = rootView.getRootView();

        screenView.setDrawingCacheEnabled(true);
        final Bitmap fullSizedBitmap = screenView.getDrawingCache();
        if (fullSizedBitmap == null) {
            ApplistLog.getInstance().log(new RuntimeException("Cannot get drawing cache"));
            return;
        }
        final Point screenSize = mScreenUtils.getScreenSize(activity);
        final Bitmap bitmap = Bitmap.createScaledBitmap(
                fullSizedBitmap,
                Math.round(screenSize.x / 4.0f),
                Math.round(screenSize.y / 4.0f),
                true);
        screenView.setDrawingCacheEnabled(false);

        final String filePath = createScreenshotPath(activity, pageId);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final File file = new File(filePath);
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    ApplistLog.getInstance().log(e);
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            // Ignore.
                        }
                    }
                }
                return null;
            }
        });
    }

    public void deleteScreenshot(String screenshotPath) {
        File file = new File(screenshotPath);
        try {
            file.delete();
        } catch (SecurityException e) {
            ApplistLog.getInstance().log(e);
        }
    }

}
