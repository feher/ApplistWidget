package net.feheren_fekete.applist.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.view.View;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.utils.ScreenUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import bolts.Task;

public class ScreenshotUtils {

    private Handler mHandler = new Handler();
    private Activity mActivity;
    private long mPageId;

    private static ScreenshotUtils sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            sInstance = new ScreenshotUtils();
        }
    }

    public static ScreenshotUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(ScreenshotUtils.class.getSimpleName() + " singleton is not initialized");
        }
    }

    private ScreenshotUtils() {
    }

    private Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot(mActivity, mPageId);
        }
    };

    public void scheduleScreenshot(Activity activity, long pageId, int delayMillis) {
        mActivity = activity;
        mPageId = pageId;
        mHandler.removeCallbacks(mScreenshotRunnable);
        mHandler.postDelayed(mScreenshotRunnable, delayMillis);
    }

    public String createScreenshotPath(Context context, long pageId) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "applist-page-" + pageId + ".png";
    }

    private void takeScreenshot(Activity activity, long pageId) {
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        View screenView = rootView.getRootView();

        screenView.setDrawingCacheEnabled(true);
        Point screenSize = ScreenUtils.getScreenSize(activity);
        final Bitmap bitmap = Bitmap.createScaledBitmap(
                screenView.getDrawingCache(),
                screenSize.x / 4,
                screenSize.y / 4,
                true);
        screenView.setDrawingCacheEnabled(false);

        final String filePath = createScreenshotPath(activity, pageId);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                File file = new File(filePath);
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
