package net.feheren_fekete.applist.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.View;

import net.feheren_fekete.applist.ApplistLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import bolts.Task;

public class ScreenshotUtils {

    public String createScreenshotPath(Context context, long pageId) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "applist-page-" + pageId + ".png";
    }

    public void takeScreenshot(Activity activity, long pageId) {
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
