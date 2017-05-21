package net.feheren_fekete.applist;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

public class ApplistLog {

    private static ApplistLog sInstance;

    public static void initInstance() {
        if (sInstance != null) {
            throw new RuntimeException("Singleton is already initialized");
        }
        sInstance = new ApplistLog();
    }

    public static ApplistLog getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("Singleton is not initialized");
        }
        return sInstance;
    }

    public void log(String message, Throwable exception) {
        Crashlytics.logException(exception);
    }

    public void log(Throwable exception) {
        Crashlytics.logException(exception);
    }

    public void d(String tag, String message) {
        Crashlytics.log(Log.DEBUG, tag, message);
    }

}
