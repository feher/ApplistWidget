package net.feheren_fekete.applist;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import static org.koin.java.KoinJavaComponent.get;

public class ApplistLog {

    public static ApplistLog getInstance() {
        return get(ApplistLog.class);
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
