package net.feheren_fekete.applist.utils;

import android.content.Context;
import android.util.Log;

import net.feheren_fekete.applist.ApplistLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    public String getIconCacheDirPath(Context context) {
        String iconCacheDirPath = context.getCacheDir().toString()
                + File.separator
                + "IconCache";
        return iconCacheDirPath;
    }

    public void deleteFiles(String dirPath, final String fileNamePrefix) {
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

    public String readFile(String filePath) {
        String fileContent = "";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            return fileContent;
        }
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(fis, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return fileContent;
        }

        try {
            StringBuilder stringBuilder = new StringBuilder("");
            char[] buffer = new char[1024];
            int n;
            while ((n = isr.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, n));
            }
            fileContent = stringBuilder.toString();
        } catch (IOException e) {
            ApplistLog.getInstance().log(e);
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return fileContent;
    }

    public void writeFile(String filePath, String content) {
        BufferedWriter bw = null;
        try {
            FileOutputStream fw = new FileOutputStream(filePath);
            OutputStreamWriter osw = new OutputStreamWriter(fw, "UTF-8");
            bw = new BufferedWriter(osw);
            bw.write(content);
        } catch (IOException e) {
            ApplistLog.getInstance().log(e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    ApplistLog.getInstance().log(e);
                }
            }
        }
    }


}
