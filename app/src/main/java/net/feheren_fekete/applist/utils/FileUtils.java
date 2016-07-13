package net.feheren_fekete.applist.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    public static String getIconCacheDirPath(Context context) {
        String iconCacheDirPath = context.getCacheDir().toString()
                + File.separator
                + "IconCache";
        return iconCacheDirPath;
    }

    public static void deleteFiles(String dirPath, final String fileNamePrefix) {
        File dir = new File(dirPath);
        File[] matchingFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith(fileNamePrefix);
            }
        });
        for (File file : matchingFiles) {
            Log.d(TAG, "Deleting cached icon: " + file.getAbsolutePath());
            file.delete();
        }
    }

}
