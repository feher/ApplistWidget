package net.feheren_fekete.applist;

import android.util.Log;

public class ApplistDevLog implements ApplistLog {

    @Override
    public void log(String message, Throwable exception) {
        Log.d("LOG", message, exception);
    }

    @Override
    public void log(Throwable exception) {
        Log.d("LOG", "Exception", exception);
    }

    @Override
    public void d(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void analytics(String event, String origin) {
        Log.d("LOG", "Event: " + event + ", Origin: "+ origin);
    }

}
