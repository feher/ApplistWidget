package net.feheren_fekete.applist.widgetpage

import android.annotation.TargetApi
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.launcher.pagepicker.PagePickerActivity
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData
import net.feheren_fekete.applist.utils.ScreenUtils
import net.feheren_fekete.applist.widgetpage.model.WidgetData
import net.feheren_fekete.applist.widgetpage.model.WidgetModel
import net.feheren_fekete.applist.widgetpage.widgetpicker.WidgetPickerActivity
import org.koin.java.KoinJavaComponent.get
import java.lang.ref.WeakReference
import java.util.*

class WidgetHelper {
    class ShowPagePickerEvent(val data: Bundle)

    private val applistLog = get(ApplistLog::class.java)
    private val widgetModel = get(WidgetModel::class.java)
    private val screenUtils = get(ScreenUtils::class.java)
    private val appWidgetManager = get(AppWidgetManager::class.java)
    private val appWidgetHost = get(AppWidgetHost::class.java)

    private var activityRef: WeakReference<Activity>? = null
    private var pinnedAppWidgetProviderInfo: AppWidgetProviderInfo? = null
    private var pageId: Long = 0
    private var allocatedAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    fun handleIntent(activity: Activity, intent: Intent): Boolean {
        val action = intent.action
        if (LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET == action) {
            handlePinWidgetRequest(activity, intent)
            return true
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun handlePinWidgetRequest(activity: Activity, intent: Intent) {
        val pinItemRequest =
            intent.getParcelableExtra<PinItemRequest>(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
        val appWidgetProviderInfo =
            pinItemRequest?.getAppWidgetProviderInfo(activity)
        if (appWidgetProviderInfo == null) {
            applistLog.log(RuntimeException("AppWidgetProviderInfo is null"))
            return
        }
        Log.d(TAG, "PINNING WIDGET " + appWidgetProviderInfo.provider)
        activityRef = WeakReference(activity)
        pinnedAppWidgetProviderInfo = appWidgetProviderInfo
        val data = Bundle()
        data.putInt(PAGE_PICK_REQUEST_KEY, PICK_PAGE_FOR_PINNING_WIDGET_REQUEST)
        data.putParcelable(APP_WIDGET_PROVIDER_INFO_KEY, pinnedAppWidgetProviderInfo)
        val pagePickerIntent = Intent(activity, PagePickerActivity::class.java)
        pagePickerIntent.putExtra(
            PagePickerActivity.EXTRA_TITLE,
            activity.getString(R.string.page_picker_pin_widget_title)
        )
        pagePickerIntent.putExtra(
            PagePickerActivity.EXTRA_MESSAGE,
            activity.getString(R.string.page_picker_message)
        )
        pagePickerIntent.putExtra(
            PagePickerActivity.EXTRA_REQUEST_DATA,
            data
        )
        activity.startActivity(pagePickerIntent)
    }

    fun handlePagePicked(
        context: Context,
        pageData: LauncherPageData,
        pickRequestData: Bundle
    ): Boolean {
        val requestCode = pickRequestData.getInt(PAGE_PICK_REQUEST_KEY)
        if (requestCode == PICK_PAGE_FOR_PINNING_WIDGET_REQUEST) {
            return pinWidgetToPage(pageData)
        } else if (requestCode == PICK_PAGE_FOR_MOVING_WIDGET_REQUEST) {
            return moveWidgetToPage(context, pageData, pickRequestData)
        }
        return false
    }

    private fun pinWidgetToPage(pageData: LauncherPageData): Boolean {
        val activity = activityRef?.get() ?: return false
        if (pageData.type == LauncherPageData.TYPE_WIDGET_PAGE) {
            bindWidget(
                activity,
                appWidgetHost,
                pageData.id,
                pinnedAppWidgetProviderInfo!!.provider
            )
            return true
        } else {
            Toast.makeText(
                activity,
                R.string.page_picker_cannot_add_to_page,
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    private fun moveWidgetToPage(
        context: Context,
        pageData: LauncherPageData,
        pagePickRequestData: Bundle
    ): Boolean {
        var handled = false
        if (pageData.type == LauncherPageData.TYPE_WIDGET_PAGE) {
            val widgetData: WidgetData? = pagePickRequestData.getParcelable(WIDGET_DATA_KEY)
            if (widgetData == null) {
                applistLog.log(RuntimeException("WidgetData is null"))
                return false
            }
            if (widgetData.pageId != pageData.id) {
                widgetData.pageId = pageData.id
                GlobalScope.launch(Dispatchers.IO) {
                    widgetModel.updateWidget(widgetData, true)
                }
                Toast.makeText(context, R.string.page_picker_moved_widget, Toast.LENGTH_SHORT)
                    .show()
                handled = true
            } else {
                Toast.makeText(context, R.string.page_picker_already_on_page, Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, R.string.page_picker_cannot_add_to_page, Toast.LENGTH_SHORT)
                .show()
        }
        return handled
    }

    private fun bindWidget(
        activity: Activity,
        appWidgetHost: AppWidgetHost,
        destinationPageId: Long,
        widgetProvider: ComponentName
    ) {
        pageId = destinationPageId
        allocatedAppWidgetId = appWidgetHost.allocateAppWidgetId()
        val bindIntent = Intent(activity, WidgetPickerActivity::class.java)
        bindIntent.action = WidgetPickerActivity.ACTION_BIND_WIDGET
        bindIntent.putExtra(WidgetPickerActivity.EXTRA_WIDGET_PROVIDER, widgetProvider)
        bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedAppWidgetId)
        bindIntent.putExtra(
            WidgetPickerActivity.EXTRA_WIDGET_WIDTH,
            DEFAULT_WIDGET_WIDTH
        )
        bindIntent.putExtra(
            WidgetPickerActivity.EXTRA_WIDGET_HEIGHT,
            DEFAULT_WIDGET_HEIGHT
        )
        activity.startActivityForResult(bindIntent, REQUEST_BIND_APPWIDGET)
    }

    fun pickWidget(activity: Activity, destinationPageId: Long) {
        activityRef = WeakReference(activity)
        pageId = destinationPageId
        allocatedAppWidgetId = appWidgetHost.allocateAppWidgetId()

//        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
//        addEmptyData(pickIntent);
        val pickIntent = Intent(activity, WidgetPickerActivity::class.java)
        pickIntent.action = WidgetPickerActivity.ACTION_PICK_AND_BIND_WIDGET
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedAppWidgetId)
        pickIntent.putExtra(
            WidgetPickerActivity.EXTRA_WIDGET_WIDTH,
            DEFAULT_WIDGET_WIDTH
        )
        pickIntent.putExtra(
            WidgetPickerActivity.EXTRA_WIDGET_HEIGHT,
            DEFAULT_WIDGET_HEIGHT
        )

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        val topPadding = screenUtils.getStatusBarHeight(activity)
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        val bottomPadding =
            if (screenUtils.hasNavigationBar(activity)) screenUtils.getNavigationBarHeight(
                activity
            ) else 0
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_TOP_PADDING, topPadding)
        pickIntent.putExtra(WidgetPickerActivity.EXTRA_BOTTOM_PADDING, bottomPadding)
        activity.startActivityForResult(
            pickIntent,
            REQUEST_PICK_AND_BIND_APPWIDGET
        )
    }

    // This is needed only when using AppWidgetManager.ACTION_APPWIDGET_PICK.
    private fun addEmptyData(pickIntent: Intent) {
        val customInfo =
            ArrayList<AppWidgetProviderInfo>()
        pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo
        )
        val customExtras = ArrayList<Bundle>()
        pickIntent.putParcelableArrayListExtra(
            AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras
        )
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_PICK_AND_BIND_APPWIDGET
            && requestCode != REQUEST_BIND_APPWIDGET
            && requestCode != REQUEST_CONFIGURE_APPWIDGET
        ) {
            return false
        }
        val extras = data?.extras
        if (extras == null) {
            cancelPendingWidget()
            return true
        }
        val appWidgetId = extras.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget()
            return true
        }
        val context = context
        if (context == null || resultCode == Activity.RESULT_CANCELED) {
            cancelPendingWidget(appWidgetId)
        } else {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_AND_BIND_APPWIDGET
                    || requestCode == REQUEST_BIND_APPWIDGET
                ) {
                    addWidgetToModel(appWidgetId)
                    val isConfigurationOk = configureWidget(appWidgetId)
                    if (!isConfigurationOk) {
                        cancelPendingWidget(appWidgetId)
                    }
                }
            }
        }
        return true
    }

    private fun configureWidget(appWidgetId: Int): Boolean {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = appWidgetInfo.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            return startActivityForResult(
                intent,
                REQUEST_CONFIGURE_APPWIDGET
            )
        }
        return true
    }

    private fun addWidgetToModel(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val screenSize = screenUtils.getScreenSizeDp(context)
        val widgetData = WidgetData(
            System.currentTimeMillis(),
            appWidgetId,
            appWidgetInfo.provider.packageName,
            appWidgetInfo.provider.className,
            pageId,
            screenSize.x / 2 - DEFAULT_WIDGET_WIDTH / 2,
            screenSize.y / 2 - DEFAULT_WIDGET_HEIGHT / 2,
            DEFAULT_WIDGET_WIDTH,
            DEFAULT_WIDGET_HEIGHT
        )
        GlobalScope.launch(Dispatchers.IO) {
            widgetModel.addWidget(widgetData)
        }
    }

    private fun cancelPendingWidget() {
        if (allocatedAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingWidget(allocatedAppWidgetId)
        }
    }

    private fun cancelPendingWidget(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
        GlobalScope.launch(Dispatchers.IO) {
            widgetModel.deleteWidget(appWidgetId)
        }
        if (allocatedAppWidgetId == appWidgetId) {
            allocatedAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        } else {
            ApplistLog.getInstance().log(
                RuntimeException(
                    "Allocated app widget ID does not match: $allocatedAppWidgetId != $appWidgetId"
                )
            )
        }
    }

    private val context: Context?
        get() = activityRef?.get()

    private fun startActivityForResult(intent: Intent, requestCode: Int): Boolean {
        val activity = activityRef?.get() ?: return false
        return try {
            activity.startActivityForResult(intent, requestCode)
            true
        } catch (e: ActivityNotFoundException) {
            ApplistLog.getInstance().log(e)
            false
        } catch (e: SecurityException) {
            ApplistLog.getInstance().log(e)
            false
        }
    }

    companion object {
        private val TAG = WidgetHelper::class.java.simpleName

        const val PICK_PAGE_FOR_PINNING_WIDGET_REQUEST = 1
        const val PICK_PAGE_FOR_MOVING_WIDGET_REQUEST = 2

        @JvmField
        val PAGE_PICK_REQUEST_KEY =
            WidgetHelper::class.java.simpleName + ".PAGE_PICK_REQUEST_KEY"

        @JvmField
        val APP_WIDGET_PROVIDER_INFO_KEY =
            WidgetHelper::class.java.simpleName + ".APP_WIDGET_PROVIDER_INFO_KEY"

        @JvmField
        val WIDGET_DATA_KEY =
            WidgetHelper::class.java.simpleName + ".WIDGET_DATA_KEY"

        private const val REQUEST_PICK_AND_BIND_APPWIDGET = 1000
        private const val REQUEST_BIND_APPWIDGET = 2000
        private const val REQUEST_CONFIGURE_APPWIDGET = 3000
        private const val DEFAULT_WIDGET_WIDTH = 300 // dp
        private const val DEFAULT_WIDGET_HEIGHT = 200 // dp
    }
}