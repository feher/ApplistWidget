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
import net.feheren_fekete.applist.launcher.pagepicker.PagePickerActivity;
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetData;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;
import net.feheren_fekete.applist.widgetpage.widgetpicker.WidgetPickerActivity;

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
    private AppWidgetManager mAppWidgetManager = get(AppWidgetManager.class);
    private AppWidgetHost mAppWidgetHost = get(AppWidgetHost.class);

    private WeakReference<Activity> mActivityRef;
    private AppWidgetProviderInfo mPinnedAppWidgetProviderInfo;
    private long mPageId;
    private int mAllocatedAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public boolean handleIntent(Activity activity,
                                Intent intent) {
        final String action = intent.getAction();
        if (LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET.equals(action)) {
            handlePinWidgetRequest(activity, intent);
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void handlePinWidgetRequest(final Activity activity,
                                        Intent intent) {
        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final AppWidgetProviderInfo appWidgetProviderInfo = pinItemRequest.getAppWidgetProviderInfo(activity);
        Log.d(TAG, "PINNING WIDGET " + appWidgetProviderInfo.provider);

        mActivityRef = new WeakReference<>(activity);
        mPinnedAppWidgetProviderInfo = appWidgetProviderInfo;

        Bundle data = new Bundle();
        data.putInt(PAGE_PICK_REQUEST_KEY, PICK_PAGE_FOR_PINNING_WIDGET_REQUEST);
        data.putParcelable(APP_WIDGET_PROVIDER_INFO_KEY, mPinnedAppWidgetProviderInfo);

        Intent pagePickerIntent = new Intent(activity, PagePickerActivity.class);
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_TITLE,
                activity.getString(R.string.page_picker_pin_widget_title));
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_MESSAGE,
                activity.getString(R.string.page_picker_message));
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_REQUEST_DATA,
                data);
        activity.startActivity(pagePickerIntent);
    }

    public boolean handlePagePicked(Context context,
                                    LauncherPageData pageData,
                                    Bundle pickRequestData) {
        final int requestCode = pickRequestData.getInt(PAGE_PICK_REQUEST_KEY);
        if (requestCode == PICK_PAGE_FOR_PINNING_WIDGET_REQUEST) {
            return pinWidgetToPage(pageData);
        } else if (requestCode == PICK_PAGE_FOR_MOVING_WIDGET_REQUEST) {
            return moveWidgetToPage(context, pageData, pickRequestData);
        }
        return false;
    }

    private boolean pinWidgetToPage(LauncherPageData pageData) {
        boolean handled = false;
        Activity activity = mActivityRef.get();
        if (activity != null) {
            if (pageData.getType() == LauncherPageData.TYPE_WIDGET_PAGE) {
                bindWidget(activity, mAppWidgetHost, pageData.getId(), mPinnedAppWidgetProviderInfo.provider);
                handled = true;
            } else {
                Toast.makeText(activity, R.string.page_picker_cannot_add_to_page, Toast.LENGTH_SHORT).show();
            }
        }
        return handled;
    }

    private boolean moveWidgetToPage(Context context,
                                    final LauncherPageData pageData,
                                    Bundle pagePickRequestData) {
        boolean handled = false;
        if (pageData.getType() == LauncherPageData.TYPE_WIDGET_PAGE) {
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

    public void pickWidget(Activity activity, long pageId) {
        mActivityRef = new WeakReference<>(activity);
        mPageId = pageId;

        mAllocatedAppWidgetId = mAppWidgetHost.allocateAppWidgetId();

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

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PICK_AND_BIND_APPWIDGET
                && requestCode != REQUEST_BIND_APPWIDGET
                && requestCode != REQUEST_CONFIGURE_APPWIDGET) {
            return false;
        }

        if (data == null) {
            cancelPendingWidget();
            return true;
        }

        Bundle extras = data.getExtras();
        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget();
            return true;
        }

        Context context = getContext();
        if (context == null || resultCode == Activity.RESULT_CANCELED) {
            cancelPendingWidget(appWidgetId);
        } else {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_AND_BIND_APPWIDGET
                        || requestCode == REQUEST_BIND_APPWIDGET) {
                    addWidgetToModel(appWidgetId);
                    final boolean isConfigurationOk = configureWidget(appWidgetId);
                    if (!isConfigurationOk) {
                        cancelPendingWidget(appWidgetId);
                    }
                }
            }
        }
        return true;
    }

    private boolean configureWidget(int appWidgetId) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            return startActivityForResult(intent, REQUEST_CONFIGURE_APPWIDGET);
        }
        return true;
    }

    private void addWidgetToModel(int appWidgetId) {
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
    }

    private void cancelPendingWidget() {
        if (mAllocatedAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget(mAllocatedAppWidgetId);
        }
    }

    private void cancelPendingWidget(final int appWidgetId) {
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        Task.callInBackground((Callable<Void>) () -> {
            mWidgetModel.deleteWidget(appWidgetId);
            return null;
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
