package net.feheren_fekete.applist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    public static Bitmap drawableToBitmap(Drawable drawable) {
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

    public static void saveBitmap(Bitmap bitmap, String filePath) {
        OutputStream outStream = null;

        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            file = new File(filePath);
        }

        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            try {
                parentDir.mkdirs();
            } catch (SecurityException e) {
                ApplistLog.getInstance().log("Cannot create dir", e);
            }
        }

        try {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
        } catch (Exception e) {
            ApplistLog.getInstance().log("Cannot save bitmap!", e);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

    @Nullable
    public static Bitmap loadBitmap(String filePath) {
        return BitmapFactory.decodeFile(filePath);
    }

}
