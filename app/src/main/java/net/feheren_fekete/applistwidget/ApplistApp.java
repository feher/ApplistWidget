package net.feheren_fekete.applistwidget;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import net.feheren_fekete.applistwidget.model.DataModel;

import java.util.concurrent.Callable;

import bolts.Task;

public class ApplistApp extends MultiDexApplication {

    private DataModel mDataModel;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDataModel = new DataModel(this, getPackageManager());
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mDataModel.loadData();
                return null;
            }
        });
    }

    public DataModel getDataModel() {
        return mDataModel;
    }
}
