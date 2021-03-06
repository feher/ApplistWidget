package net.feheren_fekete.applist.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import net.feheren_fekete.applist.ApplistLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.Nullable;


public class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    public Bitmap shortcutPlaceholder() {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            // Single color bitmap will be created of 1x1 pixel
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void saveBitmap(Bitmap bitmap, String filePath) {
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
                return;
            }
        }

        try {
            outStream = new FileOutputStream(file);
            // From Stack Overflow: https://stackoverflow.com/a/59399265/3577903
            // "For anyone who stumbles upon it, like me, wondering why bitnap colors change once
            // you write it out in PNG. I have found out that using bitmap.setPreMultiplied(false)
            // before calling bitmap.compress(PNG, 100, fileout) preserves the color of your image
            // if it has alpha channel values. Setpremultiplied prevents the pixels written onto
            // disk from being multiplied by alpha channel, thus generating different pixel
            // color values."
            bitmap.setPremultiplied(false);
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
    public Bitmap loadBitmap(String filePath) {
        return BitmapFactory.decodeFile(filePath);
    }

}
