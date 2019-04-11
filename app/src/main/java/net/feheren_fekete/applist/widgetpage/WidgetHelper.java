package net.feheren_fekete.applist.widgetpage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.ShortcutHelper;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetData;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;
import net.feheren_fekete.applist.widgetpage.widgetpicker.WidgetPickerActivity;
import net.feheren_fekete.applist.widgetpage.widgetpicker.WidgetPickerData;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import bolts.Task;

import static org.koin.java.KoinJavaComponent.get;

public class WidgetHelper {

    private static final String TAG = WidgetHelper.class.getSimpleName();

    public static final int PICK_PAGE_FOR_PINNING_WIDGET_REQUEST = 1;
    public static final int PICK_PAGE_FOR_MOVING_WIDGET_REQUEST = 2;
    public static final String PAGE_PICK_REQUEST_KEY = WidgetHelper.class.getSimpleName() + ".PAGE_PICK_REQUEST_KEY";
    public static final String APP_WIDGET_PROVIDER_INFO_KEY = WidgetHelper.class.getSimpleName() + ".APP_WIDGET_PROVIDER_INFO_KEY";
    public static final String WIDGET_DATA_KEY = WidgetHelper.class.getSimpleName() + ".WIDGET_DATA_KEY";

    private static final int REQUEST_PICK_AND_BIND_APPWIDGET = 1000;
    private static final int REQUEST_BIND_APPWIDGET = 2000;
    private static final int REQUEST_CONFIGURE_APPWIDGET = 3000;

    private static final int DEFAULT_WIDGET_WIDTH = 300; // dp
    private static final int DEFAULT_WIDGET_HEIGHT = 200; // dp

    public static final class ShowPagePickerEvent {
        public final Bundle data;
        public ShowPagePickerEvent(Bundle data) {
            this.data = data;
        }
    }

    private WidgetModel mWidgetModel = get(WidgetModel.class);
    private ScreenUtils mScreenUtils = get(ScreenUtils.class);

    private WeakReference<Activity> mActivityRef;
    private WeakReference<AppWidgetManager> mAppWidgetManagerRef;
    private WeakReference<AppWidgetHost> mAppWidgetHostRef;
    private AppWidgetProviderInfo mPinnedAppWidgetProviderInfo;
    private long mPageId;
    private int mAllocatedAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

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
            handlePinWidgetRequest(activity, intent, appWidgetManager, appWidgetHost);
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void handlePinWidgetRequest(final Activity activity,
                                        Intent intent,
                                        final AppWidgetManager appWidgetManager,
                                        final AppWidgetHost appWidgetHost) {
        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final AppWidgetProviderInfo appWidgetProviderInfo = pinItemRequest.getAppWidgetProviderInfo(activity);
        Log.d(TAG, "PINNING WIDGET " + appWidgetProviderInfo.provider);

        mActivityRef = new WeakReference<>(activity);
        mAppWidgetManagerRef = new WeakReference<>(appWidgetManager);
        mAppWidgetHostRef = new WeakReference<>(appWidgetHost);
        mPinnedAppWidgetProviderInfo = appWidgetProviderInfo;

        Bundle data = new Bundle();
        data.putInt(PAGE_PICK_REQUEST_KEY, PICK_PAGE_FOR_PINNING_WIDGET_REQUEST);
        data.putParcelable(APP_WIDGET_PROVIDER_INFO_KEY, mPinnedAppWidgetProviderInfo);
        EventBus.getDefault().post(new ShowPagePickerEvent(data));
    }

    public boolean handlePagePicked(Context context,
                                    PageData pageData,
                                    Bundle pickRequestData) {
        final int requestCode = pickRequestData.getInt(PAGE_PICK_REQUEST_KEY);
        if (requestCode == PICK_PAGE_FOR_PINNING_WIDGET_REQUEST) {
            return pinWidgetToPage(pageData);
        } else if (requestCode == PICK_PAGE_FOR_MOVING_WIDGET_REQUEST) {
            return moveWidgetToPage(context, pageData, pickRequestData);
        }
        return false;
    }

    private boolean pinWidgetToPage(PageData pageData) {
        boolean handled = false;
        Activity activity = mActivityRef.get();
        AppWidgetHost appWidgetHost = mAppWidgetHostRef.get();
        if (activity != null && appWidgetHost != null) {
            if (pageData.getType() == PageData.TYPE_WIDGET_PAGE) {
                bindWidget(activity, appWidgetHost, pageData.getId(), mPinnedAppWidgetProviderInfo.provider);
                handled = true;
            } else {
                Toast.makeText(activity, R.string.page_picker_cannot_add_to_page, Toast.LENGTH_SHORT).show();
            }
        }
        return handled;
    }

    private boolean moveWidgetToPage(Context context,
                                    final PageData pageData,
                                    Bundle pagePickRequestData) {
        boolean handled = false;
        if (pageData.getType() == PageData.TYPE_WIDGET_PAGE) {
            final WidgetData widgetData = pagePickRequestData.getParcelable(WIDGET_DATA_KEY);
            if (widgetData.getPageId() != pageData.getId()) {
                widgetData.setPageId(pageData.getId());
                Task.callInBackground(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mWidgetModel.updateWidget(widgetData, true);
                        return null;
                    }
                });
                handled = true;
            } else {
                Toast.makeText(context, R.string.page_picker_already_on_page, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, R.string.page_picker_cannot_add_to_page, Toast.LENGTH_SHORT).show();
        }
        return handled;
    }

    private void bindWidget(Activity activity,
                           AppWidgetHost appWidgetHost,
                           long pageId,
                           ComponentName widgetProvider) {
        mPageId = pageId;

        mAllocatedAppWidgetId = appWidgetHost.allocateAppWidgetId();

        Intent bindIntent = new Intent(activity, WidgetPickerActivity.class);
        bindIntent.setAction(WidgetPickerActivity.ACTION_BIND_WIDGET);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_PROVIDER, widgetProvider);
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAllocatedAppWidgetId);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_WIDTH, DEFAULT_WIDGET_WIDTH);
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_HEIGHT, DEFAULT_WIDGET_HEIGHT);

        activity.startActivityForResult(bindIntent, REQUEST_BIND_APPWIDGET);
    }

    public void pickWidget(Activity activity, AppWidgetManager appWidgetManager, AppWidgetHost appWidgetHost, long pageId) {
        mActivityRef = new WeakReference<>(activity);
        mAppWidgetManagerRef = new WeakReference<>(appWidgetManager);
        mPageId = pageId;

        mAllocatedAppWidgetId = appWidgetHost.allocateAppWidgetId();

//        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
//        addEmptyData(pickIntent);

        Intent pickIntent = new Intent(activity, WidgetPickerActivity.class);
        pickIntent.setAction(WidgetPickerActivity.ACTION_PICK_AND_BIND_WIDGET);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAllocatedAppWidgetId);
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

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data,
                                        AppWidgetHost appWidgetHost) {
        if (requestCode != REQUEST_PICK_AND_BIND_APPWIDGET
                && requestCode != REQUEST_BIND_APPWIDGET
                && requestCode != REQUEST_CONFIGURE_APPWIDGET) {
            return false;
        }

        if (data == null) {
            cancelPendingWidget(appWidgetHost);
            return true;
        }

        Bundle extras = data.getExtras();
        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget(appWidgetHost);
            return true;
        }

        Context context = getContext();
        AppWidgetManager appWidgetManager = mAppWidgetManagerRef.get();
        if (context == null || appWidgetManager == null || resultCode == Activity.RESULT_CANCELED) {
            cancelPendingWidget(appWidgetId, appWidgetHost);
        } else {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_AND_BIND_APPWIDGET
                        || requestCode == REQUEST_BIND_APPWIDGET) {
                    addWidgetToModel(appWidgetId, appWidgetManager);
                    final boolean isConfigurationOk = configureWidget(appWidgetId, appWidgetManager);
                    if (!isConfigurationOk) {
                        cancelPendingWidget(appWidgetId, appWidgetHost);
                    }
                }
            }
        }
        return true;
    }

    private boolean configureWidget(int appWidgetId, AppWidgetManager appWidgetManager) {
        AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            return startActivityForResult(intent, REQUEST_CONFIGURE_APPWIDGET);
        }
        return true;
    }

    private void addWidgetToModel(int appWidgetId, AppWidgetManager appWidgetManager) {
        final AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
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
    }

    private void cancelPendingWidget(AppWidgetHost appWidgetHost) {
        if (mAllocatedAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget(mAllocatedAppWidgetId, appWidgetHost);
        }
    }

    private void cancelPendingWidget(final int appWidgetId, AppWidgetHost appWidgetHost) {
        appWidgetHost.deleteAppWidgetId(appWidgetId);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.deleteWidget(appWidgetId);
                return null;
            }
        });
        if (mAllocatedAppWidgetId == appWidgetId) {
            mAllocatedAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        } else {
            ApplistLog.getInstance().log(new RuntimeException(
                    "Allocated app widget ID does not match: " + mAllocatedAppWidgetId + " != " + appWidgetId));
        }
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
                try {
                    activity.startActivityForResult(intent, requestCode);
                } catch (ActivityNotFoundException | SecurityException e) {
                    ApplistLog.getInstance().log(e);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

}
