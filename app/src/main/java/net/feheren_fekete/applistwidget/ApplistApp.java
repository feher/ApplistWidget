package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import net.feheren_fekete.applistwidget.model.DataModel;

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistApp extends MultiDexApplication {

    private static final String TAG = ApplistApp.class.getSimpleName();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        DataModel.initInstance(this, getPackageManager());

        final DataModel dataModel = DataModel.getInstance();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                dataModel.loadData();
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                dataModel.updateInstalledApps();
                                return null;
                            }
                        });
                    }
                }, 1000);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

}

