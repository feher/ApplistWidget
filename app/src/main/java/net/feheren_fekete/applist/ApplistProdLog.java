package net.feheren_fekete.applist;

import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import static org.koin.java.KoinJavaComponent.get;

public class ApplistProdLog implements ApplistLog {

    private FirebaseAnalytics mFirebaseAnalytics = get(FirebaseAnalytics.class);

    @Override
    public void log(String message, Throwable exception) {
        Crashlytics.logException(exception);
    }

    @Override
    public void log(Throwable exception) {
        Crashlytics.logException(exception);
    }

    @Override
    public void d(String tag, String message) {
        Crashlytics.log(Log.DEBUG, tag, message);
    }

    @Override
    public void analytics(String event, String origin) {
        Bundle bundle = new Bundle();
        bundle.putString("origin", origin);
        mFirebaseAnalytics.logEvent(event, bundle);
    }

}
