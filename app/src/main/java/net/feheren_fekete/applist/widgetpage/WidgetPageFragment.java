package net.feheren_fekete.applist.widgetpage;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import net.feheren_fekete.applist.MainActivity;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetData;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;
import net.feheren_fekete.applist.utils.ScreenUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public class WidgetPageFragment extends Fragment {

    private static final int REQUEST_PICK_APPWIDGET = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 2;
    private static final int REQUEST_BIND_APPWIDGET = 3;

    private static final int NO_BORDER = 0;
    private static final int TOP_BORDER = 1;
    private static final int BOTTOM_BORDER = 1 << 1;
    private static final int LEFT_BORDER = 1 << 2;
    private static final int RIGHT_BORDER = 1 << 3;

    private static final int DEFAULT_WIDGET_WIDTH = 200;
    private static final int DEFAULT_WIDGET_HEIGHT = 300;

    public static final class ShowPageEditorEvent {}
    public static final class WidgetMoveStartedEvent {}
    public static final class WidgetMoveFinishedEvent {}

    private static final class WidgetItem {
        public WidgetData widgetData;
        public MyAppWidgetHostView appWidgetHostView;
    }

    // TODO: Inject these singletons.
    private WidgetModel mWidgetModel = WidgetModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private ScreenUtils mScreenUtils = ScreenUtils.getInstance();

    private Handler mHandler = new Handler();
    private AppWidgetManager mAppWidgetManager;
    private MyAppWidgetHost mAppWidgetHost;
    private ViewGroup mWidgetContainer;
    private List<WidgetItem> mWidgets = new ArrayList<>();
    private @Nullable AlertDialog mPageMenu;
    private @Nullable AlertDialog mWidgetMenu;
    private @Nullable WidgetItem mWidgetMenuTarget;
    private int mWidgetTouchBorderWidth; // px
    private int mMinWidgetSize; // px
    private int[] mTempLocation = new int[2];
    private RectF mTempRect1 = new RectF();
    private PointF mOriginalFingerPos = new PointF();
    private PointF mPreviousFingerPos = new PointF();

    public static WidgetPageFragment newInstance(long pageId) {
        WidgetPageFragment fragment = new WidgetPageFragment();

        Bundle args = new Bundle();
        args.putLong("pageId", pageId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWidgetTouchBorderWidth = getContext().getResources().getDimensionPixelSize(R.dimen.widget_border_width_touch);
        mMinWidgetSize = getContext().getResources().getDimensionPixelSize(R.dimen.widget_min_size);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_page_fragment, container, false);

        mAppWidgetManager = ((MainActivity) getActivity()).getAppWidgetManager();
        mAppWidgetHost = ((MainActivity) getActivity()).getAppWidgetHost();

        mWidgetContainer = (ViewGroup) view.findViewById(R.id.launcher_page_fragment_widget_container);
        mWidgetContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openPageMenu();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                if (bindWidget(data)) {
                    if (configureWidget(data)) {
                        addWidgetToModelDelayed(data);
                    }
                }
            } else if (requestCode == REQUEST_BIND_APPWIDGET) {
                if (configureWidget(data)) {
                    addWidgetToModelDelayed(data);
                }
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                addWidgetToModelDelayed(data);
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateScreenFromModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadedEvent(WidgetModel.DataLoadedEvent event) {
        updateScreenFromModel();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetsChangedEvent(WidgetModel.WidgetsChangedEvent event) {
        updateScreenFromModel();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetAddedEvent(WidgetModel.WidgetAddedEvent event) {
        if (event.widgetData.getPageId() == getPageId()) {
            addWidgetToScreen(event.widgetData);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetDeletedEvent(WidgetModel.WidgetDeletedEvent event) {
        if (event.widgetData.getPageId() == getPageId()) {
            WidgetItem widgetItem = getWidgetItem(event.widgetData);
            removeWidgetFromScreen(widgetItem, true, true);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetChangedEvent(WidgetModel.WidgetChangedEvent event) {
        updateScreenFromModel();
    }

    public void handleDown(MotionEvent event) {
        if (mWidgetMenuTarget != null) {
            if (isLocationInsideWidget(mWidgetMenuTarget, event.getRawX(), event.getRawY())) {
                mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_RESIZING);
            }
        }
    }

    public void handleUp(MotionEvent event) {
        if (mWidgetMenuTarget != null) {
            mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_SELECTED);
            updateWidgetOptions(mWidgetMenuTarget.appWidgetHostView, mWidgetMenuTarget.widgetData);
            mScreenshotUtils.scheduleScreenshot(getActivity(), getPageId(), 500);
        }
    }

    public boolean handleScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        boolean handled = false;
        if (mWidgetMenuTarget != null) {
            if (mOriginalFingerPos.x != event1.getRawX() || mOriginalFingerPos.y != event1.getRawY()) {
                mOriginalFingerPos.set(event1.getRawX(), event1.getRawY());
                mPreviousFingerPos.set(event1.getRawX(), event1.getRawY());
            }

            if (isLocationInsideWidget(mWidgetMenuTarget, mPreviousFingerPos.x, mPreviousFingerPos.y)) {
                mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_RESIZING);
                updateWidgetLocation(
                        mWidgetMenuTarget,
                        mPreviousFingerPos.x, mPreviousFingerPos.y,
                        event2.getRawX(), event2.getRawY());
                handled = true;
            }

            mPreviousFingerPos.set(event2.getRawX(), event2.getRawY());
        }
        return handled;
    }

    public boolean handleSingleTap(MotionEvent event) {
        mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
        EventBus.getDefault().post(new WidgetMoveFinishedEvent());
        return true;
    }

    @Nullable
    private WidgetItem getWidgetItem(WidgetData widgetData) {
        for (WidgetItem widgetItem : mWidgets) {
            if (widgetItem.widgetData.getId() == widgetData.getId()) {
                return widgetItem;
            }
        }
        return null;
    }

    private long getPageId() {
        return getArguments().getLong("pageId");
    }

    private void showPageEditor() {
        EventBus.getDefault().post(new ShowPageEditorEvent());
    }

    private void pickWidget() {
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    private void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    private boolean bindWidget(Intent data) {
        // If we use ACTION_APPWIDGET_PICK we don't need to bind manually.
        // It's needed only if we implement our own custom widget picker.
        return true;
//        int appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
//        if (appWidgetId == -1) {
//            return true;
//        }
//        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
//
//        Bundle options = new Bundle();
//        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, DEFAULT_WIDGET_WIDTH);
//        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, DEFAULT_WIDGET_HEIGHT);
//        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, DEFAULT_WIDGET_WIDTH);
//        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, DEFAULT_WIDGET_HEIGHT);
//        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
//
//        boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
//                appWidgetId, appWidgetInfo.provider, options);
//        if (success) {
//            return true;
//        } else {
//            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);
////            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, android.os.Process.myUserHandle());
//            // This is the options bundle discussed above
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
//            startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
//            return false;
//        }
    }

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
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            return false;
        }
        return true;
    }

    private void updateScreenFromModel() {
        removeAllWidgetsFromScreen();
        List<WidgetData> widgetDatas = mWidgetModel.getWidgets(getPageId());
        for (WidgetData widgetData : widgetDatas) {
            addWidgetToScreen(widgetData);
        }
    }

    private void addWidgetToScreen(WidgetData widgetData) {
        int appWidgetId = widgetData.getAppWidgetId();
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        MyAppWidgetHostView hostView = (MyAppWidgetHostView) mAppWidgetHost.createView(
                getContext().getApplicationContext(), appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                Math.round(mScreenUtils.dpToPx(getContext(), widgetData.getWidth())),
                Math.round(mScreenUtils.dpToPx(getContext(), widgetData.getHeight())));
        layoutParams.leftMargin = Math.round(mScreenUtils.dpToPx(getContext(), widgetData.getPositionX()));
        layoutParams.topMargin = Math.round(mScreenUtils.dpToPx(getContext(), widgetData.getPositionY()));
        hostView.setLayoutParams(layoutParams);

        updateWidgetOptions(hostView, widgetData);

        final WidgetItem widgetItem = new WidgetItem();
        widgetItem.widgetData = widgetData;
        widgetItem.appWidgetHostView = hostView;
        widgetItem.appWidgetHostView.setGestureListener(new WidgetGestureListener(widgetItem));

        mWidgets.add(widgetItem);
        mWidgetContainer.addView(hostView);
        mWidgetContainer.invalidate();

        mScreenshotUtils.scheduleScreenshot(getActivity(), getPageId(), 500);
    }

    private void updateWidgetOptions(AppWidgetHostView appWidgetHostView, WidgetData widgetData) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetData.getWidth());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetData.getHeight());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetData.getWidth());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetData.getHeight());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        appWidgetHostView.updateAppWidgetOptions(options);
    }

    private void removeAllWidgetsFromScreen() {
        for (WidgetItem widgetItem : mWidgets) {
            removeWidgetFromScreen(widgetItem, false, false);
        }
        mWidgets.clear();
    }

    private void removeWidgetFromScreen(WidgetItem widgetItem, boolean removeFromList, boolean permanentlyDeleted) {
        if (permanentlyDeleted) {
            mAppWidgetHost.deleteAppWidgetId(widgetItem.appWidgetHostView.getAppWidgetId());
        }
        mWidgetContainer.removeView(widgetItem.appWidgetHostView);
        mWidgetContainer.invalidate();
        if (removeFromList) {
            mWidgets.remove(widgetItem);
        }

        mScreenshotUtils.scheduleScreenshot(getActivity(), getPageId(), 500);
    }

    private void addWidgetToModelDelayed(final Intent data) {
        // The widget picker and configuration activity sent this fragment to the PAUSED state.
        // But we want to be in the RESUMED state. Otherwise we don't receive EventBus events
        // from the WidgetModel.
        // So delay the model update to the next event loop.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addWidgetToModel(data);
            }
        }, 500);
    }

    private void addWidgetToModel(Intent data) {
        final int appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (appWidgetId == -1) {
            return;
        }
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        final WidgetData widgetData = new WidgetData(
                System.currentTimeMillis(),
                appWidgetId,
                appWidgetInfo.provider.getPackageName(),
                appWidgetInfo.provider.getClassName(),
                getPageId(),
                0, 0, DEFAULT_WIDGET_WIDTH, DEFAULT_WIDGET_HEIGHT);

        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.addWidget(widgetData);
                return null;
            }
        });
    }

    private void bringWidgetToTop(final WidgetItem widgetItem) {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.bringWidgetToTop(widgetItem.widgetData);
                return null;
            }
        });
    }

    private void removeWidgetFromModel(final WidgetItem widgetItem) {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.deleteWidget(widgetItem.widgetData);
                return null;
            }
        });
    }

    private class WidgetGestureListener extends GestureDetector.SimpleOnGestureListener {
        private WidgetItem mWidgetItem;
        public WidgetGestureListener(WidgetItem widgetItem) {
            mWidgetItem = widgetItem;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            openWidgetMenu(mWidgetItem);
        }
    }

    private void openPageMenu() {
        mPageMenu = new AlertDialog.Builder(getContext())
                .setItems(R.array.launcher_page_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Add widget
                                pickWidget();
                                break;
                            case 1:
                                // Edit pages
                                showPageEditor();
                                break;
                        }
                        mPageMenu = null;
                    }
                })
                .setCancelable(true)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mPageMenu = null;
                    }
                })
                .create();
        mPageMenu.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mPageMenu.show();
    }

    private void openWidgetMenu(WidgetItem widgetItem) {
        mWidgetMenu = new AlertDialog.Builder(getContext())
                .setItems(R.array.widget_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_SELECTED);
                                EventBus.getDefault().post(new WidgetMoveStartedEvent());
                                break;
                            case 1:
                                mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                                bringWidgetToTop(mWidgetMenuTarget);
                                break;
                            case 2:
                                mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                                removeWidgetFromModel(mWidgetMenuTarget);
                                break;
                        }
                        mWidgetMenu = null;
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mWidgetMenu = null;
                    }
                })
                .create();
        mWidgetMenu.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mWidgetMenu.show();
        mWidgetMenuTarget = widgetItem;
        mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_SELECTED);
    }

    @Nullable
    private WidgetItem findWidgetAtLocation(float locationX, float locationY) {
        for (int i = mWidgets.size() - 1; i >= 0; --i) {
            final WidgetItem widgetItem = mWidgets.get(i);
            if (isLocationInsideWidget(widgetItem, locationX, locationY)) {
                return widgetItem;
            }
        }
        return null;
    }

    private boolean isLocationInsideWidget(WidgetItem widgetItem, float locationX, float locationY) {
        widgetItem.appWidgetHostView.getLocationOnScreen(mTempLocation);
        mTempRect1.set(
                mTempLocation[0],
                mTempLocation[1],
                mTempLocation[0] + widgetItem.appWidgetHostView.getWidth(),
                mTempLocation[1] + widgetItem.appWidgetHostView.getHeight());
        return mTempRect1.contains(locationX, locationY);
    }

    private void updateWidgetLocation(final WidgetItem widgetItem,
                                      float location1X, float location1Y,
                                      float location2X, float location2Y) {
        final int touchedBorder = isLocationOnTouchBorder(widgetItem, location1X, location1Y);
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) widgetItem.appWidgetHostView.getLayoutParams();
        if (touchedBorder != NO_BORDER) {
            // Resizing
            final int deltaX = Math.round(location2X - location1X);
            final int deltaY = Math.round(location2Y - location1Y);
            if ((touchedBorder & LEFT_BORDER) != 0) {
                layoutParams.leftMargin += deltaX;
                layoutParams.width -= deltaX;
            }
            if ((touchedBorder & TOP_BORDER) != 0) {
                layoutParams.topMargin += deltaY;
                layoutParams.height -= deltaY;
            }
            if ((touchedBorder & RIGHT_BORDER) != 0) {
                layoutParams.width += deltaX;
            }
            if ((touchedBorder & BOTTOM_BORDER) != 0) {
                layoutParams.height += deltaY;
            }
            layoutParams.leftMargin = Math.max(0, layoutParams.leftMargin);
            layoutParams.topMargin = Math.max(0, layoutParams.topMargin);
            layoutParams.width = Math.max(mMinWidgetSize, layoutParams.width);
            layoutParams.height = Math.max(mMinWidgetSize, layoutParams.height);
        } else {
            // Moving
            final float distanceX = location2X - location1X;
            final float distanceY = location2Y - location1Y;
            layoutParams.leftMargin += distanceX;
            layoutParams.topMargin += distanceY;
        }
        widgetItem.appWidgetHostView.setLayoutParams(layoutParams);
        Context context = getContext();
        widgetItem.widgetData.setPositionX(Math.round(mScreenUtils.pxToDp(context, layoutParams.leftMargin)));
        widgetItem.widgetData.setPositionY(Math.round(mScreenUtils.pxToDp(context, layoutParams.topMargin)));
        widgetItem.widgetData.setWidth(Math.round(mScreenUtils.pxToDp(context, layoutParams.width)));
        widgetItem.widgetData.setHeight(Math.round(mScreenUtils.pxToDp(context, layoutParams.height)));

        mWidgetContainer.invalidate();

        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetModel.updateWidget(widgetItem.widgetData);
                return null;
            }
        });
    }

    private int isLocationOnTouchBorder(WidgetItem widgetItem, float locationX, float locationY) {
        int result = NO_BORDER;

        widgetItem.appWidgetHostView.getLocationOnScreen(mTempLocation);
        final int widgetLeft = mTempLocation[0];
        final int widgetTop = mTempLocation[1];
        final int widgetRight = widgetLeft + widgetItem.appWidgetHostView.getWidth();
        final int widgetBottom = widgetTop + widgetItem.appWidgetHostView.getHeight();

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidth,
                widgetTop - mWidgetTouchBorderWidth,
                widgetRight + mWidgetTouchBorderWidth,
                widgetTop + mWidgetTouchBorderWidth);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= TOP_BORDER;
        }

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidth,
                widgetBottom - mWidgetTouchBorderWidth,
                widgetRight + mWidgetTouchBorderWidth,
                widgetBottom + mWidgetTouchBorderWidth);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= BOTTOM_BORDER;
        }

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidth,
                widgetTop - mWidgetTouchBorderWidth,
                widgetLeft + mWidgetTouchBorderWidth,
                widgetBottom + mWidgetTouchBorderWidth);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= LEFT_BORDER;
        }

        mTempRect1.set(
                widgetRight - mWidgetTouchBorderWidth,
                widgetTop - mWidgetTouchBorderWidth,
                widgetRight + mWidgetTouchBorderWidth,
                widgetBottom + mWidgetTouchBorderWidth);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= RIGHT_BORDER;
        }

        return result;
    }

}
