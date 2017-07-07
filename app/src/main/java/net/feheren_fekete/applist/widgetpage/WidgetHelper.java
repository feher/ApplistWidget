package net.feheren_fekete.applist.widgetpage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import net.feheren_fekete.applist.applistpage.ShortcutHelper;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetData;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;
import net.feheren_fekete.applist.widgetpage.widgetpicker.WidgetPickerActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import hugo.weaving.DebugLog;

public class WidgetHelper {

    private static final String TAG = WidgetHelper.class.getSimpleName();

    private static final int REQUEST_PICK_AND_BIND_APPWIDGET = 1000;
    private static final int REQUEST_BIND_APPWIDGET = 2000;
    private static final int REQUEST_CONFIGURE_APPWIDGET = 3000;

    private static final int DEFAULT_WIDGET_WIDTH = 300; // dp
    private static final int DEFAULT_WIDGET_HEIGHT = 200; // dp

    // TODO: Inject
    private LauncherModel mLauncherModel = LauncherModel.getInstance();
    private WidgetModel mWidgetModel = WidgetModel.getInstance();
    private ScreenUtils mScreenUtils = ScreenUtils.getInstance();

    private WeakReference<Activity> mActivityRef;
    private AppWidgetManager mAppWidgetManager;
    private long mPageId;

    private static WidgetHelper sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            sInstance = new WidgetHelper();
        }
    }

    public static WidgetHelper getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException(ShortcutHelper.class.getSimpleName() + " singleton is not initialized");
        }
    }

    private WidgetHelper() {
    }

    public boolean handleIntent(Activity activity,
                                Intent intent,
                                AppWidgetManager appWidgetManager,
                                MyAppWidgetHost appWidgetHost) {
        final String action = intent.getAction();
        if (LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET.equals(action)) {
            handleWidgetRequest(activity, intent, appWidgetManager, appWidgetHost);
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void handleWidgetRequest(final Activity activity,
                                     Intent intent,
                                     final AppWidgetManager appWidgetManager,
                                     final AppWidgetHost appWidgetHost) {
        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final AppWidgetProviderInfo appWidgetProviderInfo = pinItemRequest.getAppWidgetProviderInfo(activity);
        Log.d(TAG, "PINNING WIDGET " + appWidgetProviderInfo.provider);

        AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setMessage("Pin this? " + appWidgetProviderInfo.provider)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pinWidget(activity, appWidgetManager, appWidgetHost, appWidgetProviderInfo.provider);
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Nothing.
                    }
                })
                .setCancelable(true)
                .create();
        alertDialog.show();
    }

    private void pinWidget(final Activity activity,
                           final AppWidgetManager appWidgetManager,
                           final AppWidgetHost appWidgetHost,
                           final ComponentName appWidgetProvider) {
        Task.callInBackground(new Callable<PageData>() {
            @Override
            public PageData call() throws Exception {
                PageData result = null;
                List<PageData> pageDatas = mLauncherModel.getPages();
                for (final PageData pageData : pageDatas) {
                    if (pageData.getType() == PageData.TYPE_WIDGET_PAGE) {
                        result = pageData;
                    }
                }
                if (result == null) {
                    result = new PageData(System.currentTimeMillis(), PageData.TYPE_WIDGET_PAGE, false);
                    mLauncherModel.addPage(result);
                }
                return result;
            }
        }).continueWith(new Continuation<PageData, Void>() {
            @Override
            public Void then(Task<PageData> task) throws Exception {
                PageData pageData = task.getResult();
                if (pageData != null && pageData.getType() == PageData.TYPE_WIDGET_PAGE) {
                    bindWidget(
                            activity, appWidgetManager, appWidgetHost,
                            pageData.getId(),
                            appWidgetProvider);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    @DebugLog
    private void bindWidget(Activity activity,
                           AppWidgetManager appWidgetManager,
                           AppWidgetHost appWidgetHost,
                           long pageId,
                           ComponentName widgetProvider) {
        mActivityRef = new WeakReference<>(activity);
        mAppWidgetManager = appWidgetManager;
        mPageId = pageId;

        int appWidgetId = appWidgetHost.allocateAppWidgetId();

        Intent bindIntent = new Intent(activity, WidgetPickerActivity.class);
        bindIntent.setAction(WidgetPickerActivity.ACTION_BIND_WIDGET);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_PROVIDER, widgetProvider);
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_WIDTH, DEFAULT_WIDGET_WIDTH);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_HEIGHT, DEFAULT_WIDGET_HEIGHT);

        activity.startActivityForResult(bindIntent, REQUEST_BIND_APPWIDGET);
    }

    public void pickWidget(Activity activity, AppWidgetManager appWidgetManager, AppWidgetHost appWidgetHost, long pageId) {
        mActivityRef = new WeakReference<>(activity);
        mAppWidgetManager = appWidgetManager;
        mPageId = pageId;

        int appWidgetId = appWidgetHost.allocateAppWidgetId();

//        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
//        addEmptyData(pickIntent);

        Intent pickIntent = new Intent(activity, WidgetPickerActivity.class);
        pickIntent.setAction(WidgetPickerActivity.ACTION_PICK_AND_BIND_WIDGET);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_WIDTH, DEFAULT_WIDGET_WIDTH);
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_HEIGHT, DEFAULT_WIDGET_HEIGHT);

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        final int topPadding = mScreenUtils.getStatusBarHeight(activity);
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        final int bottomPadding = mScreenUtils.hasNavigationBar(activity)
                ? mScreenUtils.getNavigationBarHeight(activity) : 0;
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_TOP_PADDING, topPadding);
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_BOTTOM_PADDING, bottomPadding);

        activity.startActivityForResult(pickIntent, REQUEST_PICK_AND_BIND_APPWIDGET);
    }

    // This is needed only when using AppWidgetManager.ACTION_APPWIDGET_PICK.
    private void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    @DebugLog
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data,
                                        AppWidgetHost appWidgetHost) {
        if (requestCode != REQUEST_PICK_AND_BIND_APPWIDGET
                && requestCode != REQUEST_BIND_APPWIDGET
                && requestCode != REQUEST_CONFIGURE_APPWIDGET) {
            return false;
        }

        Context context = getContext();
        if (context == null || resultCode == Activity.RESULT_CANCELED) {
            cancelPendingWidget(data, appWidgetHost);
        } else {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_AND_BIND_APPWIDGET
                        || requestCode == REQUEST_BIND_APPWIDGET) {
                    if (addWidgetToModel(data)) {
                        configureWidget(data);
                    }
                }
            }
        }
        return true;
    }

    @DebugLog
    private boolean configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) {
            return true;
        }
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CONFIGURE_APPWIDGET);
            return false;
        }
        return true;
    }

    @DebugLog
    private boolean addWidgetToModel(Intent data) {
        final int appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) {
            return false;
        }
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        final Point screenSize = mScreenUtils.getScreenSizeDp(getContext());
        final WidgetData widgetData = new WidgetData(
                System.currentTimeMillis(),
                appWidgetId,
                appWidgetInfo.provider.getPackageName(),
                appWidgetInfo.provider.getClassName(),
                mPageId,
                (screenSize.x / 2) - (DEFAULT_WIDGET_WIDTH / 2),
                (screenSize.y / 2) - (DEFAULT_WIDGET_HEIGHT / 2),
                DEFAULT_WIDGET_WIDTH,
                DEFAULT_WIDGET_HEIGHT);

        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.addWidget(widgetData);
                return null;
            }
        });
        return true;
    }

    private void cancelPendingWidget(Intent data, AppWidgetHost appWidgetHost) {
        Bundle extras = data.getExtras();
        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) {
            return;
        }
        appWidgetHost.deleteAppWidgetId(appWidgetId);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.deleteWidget(appWidgetId);
                return null;
            }
        });
    }

    private Context getContext() {
        if (mActivityRef != null) {
            Activity activity = mActivityRef.get();
            if (activity != null) {
                return activity;
            }
        }
        return null;
    }

    private boolean startActivityForResult(Intent intent, int requestCode) {
        if (mActivityRef != null) {
            Activity activity = mActivityRef.get();
            if (activity != null) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        }
        return false;
    }

}
