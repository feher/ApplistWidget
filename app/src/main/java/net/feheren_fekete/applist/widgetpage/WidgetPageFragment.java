package net.feheren_fekete.applist.widgetpage;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.LauncherUtils;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorActivity;
import net.feheren_fekete.applist.launcher.pagepicker.PagePickerActivity;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetData;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

import static org.koin.java.KoinJavaComponent.get;

public class WidgetPageFragment extends Fragment {

    private static final int NO_BORDER = 0;
    private static final int TOP_BORDER = 1;
    private static final int BOTTOM_BORDER = 1 << 1;
    private static final int LEFT_BORDER = 1 << 2;
    private static final int RIGHT_BORDER = 1 << 3;

    public static final class WidgetMoveStartedEvent {}
    public static final class WidgetMoveFinishedEvent {}

    private static final class WidgetItem {
        public WidgetData widgetData;
        public MyAppWidgetHostView appWidgetHostView;
    }

    private ApplistLog mApplistLog = get(ApplistLog.class);
    private WidgetModel mWidgetModel = get(WidgetModel.class);
    private ScreenshotUtils mScreenshotUtils = get(ScreenshotUtils.class);
    private ScreenUtils mScreenUtils = get(ScreenUtils.class);
    private LauncherUtils mLauncherUtils = get(LauncherUtils.class);
    private WidgetHelper mWidgetHelper = get(WidgetHelper.class);
    private AppWidgetManager mAppWidgetManager = get(AppWidgetManager.class);
    private AppWidgetHost mAppWidgetHost = get(AppWidgetHost.class);

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

        mWidgetContainer = view.findViewById(R.id.launcher_page_fragment_widget_container);
        mWidgetContainer.setOnLongClickListener(v -> {
            openPageMenu();
            return true;
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWidgetHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateScreenFromModel();
    }

    @Override
    public void onResume() {
        super.onResume();

        // We do this check in case we missed some eventbus events while we were NOT in resumed
        // state.
        if (haveWidgetsChangedInModel()) {
            updateScreenFromModel();
        }

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
    }

    public void handleUp(MotionEvent event) {
        if (mWidgetMenuTarget != null) {
            updateWidgetOnScreen(mWidgetMenuTarget.appWidgetHostView, mWidgetMenuTarget.widgetData);
        }
    }

    public boolean handleScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        if (event1 == null || event2 == null) {
            return false;
        }
        boolean handled = false;
        if (mWidgetMenuTarget != null) {
            if (mOriginalFingerPos.x != event1.getRawX() || mOriginalFingerPos.y != event1.getRawY()) {
                mOriginalFingerPos.set(event1.getRawX(), event1.getRawY());
                mPreviousFingerPos.set(event1.getRawX(), event1.getRawY());
            }

            if (isLocationInsideWidget(
                    mWidgetMenuTarget,
                    mPreviousFingerPos.x,
                    mPreviousFingerPos.y,
                    mWidgetTouchBorderWidth)) {
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
        if (mWidgetMenuTarget == null) {
            mApplistLog.log(new IllegalStateException("mWidgetMenuTarget == null"));
            return false;
        }
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
        Intent intent = new Intent(requireContext(), PageEditorActivity.class);
        startActivity(intent);
    }

    private boolean haveWidgetsChangedInModel() {
        List<WidgetData> widgetDatas = mWidgetModel.getWidgets(getPageId());
        if (widgetDatas.size() != mWidgets.size()) {
            return true;
        }
        for (WidgetData widgetData : widgetDatas) {
            boolean exists = false;
            for (WidgetItem widgetItem : mWidgets) {
                if (widgetItem.widgetData.getId() == widgetData.getId()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return true;
            }
        }
        return false;
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
        if (hostView == null) {
            mApplistLog.log(
                    new RuntimeException(
                            "Cannot create view for widget: "
                                    + "pkg=" + widgetData.getProviderPackage()
                                    + "class=" + widgetData.getProviderClass()));
            return;
        }
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                Math.round(mScreenUtils.dpToPx(widgetData.getWidth())),
                Math.round(mScreenUtils.dpToPx(widgetData.getHeight())));
        layoutParams.leftMargin = Math.round(mScreenUtils.dpToPx(widgetData.getPositionX()));
        layoutParams.topMargin = Math.round(mScreenUtils.dpToPx(widgetData.getPositionY()));
        hostView.setLayoutParams(layoutParams);

        final WidgetItem widgetItem = new WidgetItem();
        widgetItem.widgetData = widgetData;
        widgetItem.appWidgetHostView = hostView;
        widgetItem.appWidgetHostView.setGestureListener(new WidgetGestureListener(widgetItem));

        updateWidgetOnScreen(hostView, widgetData);

        mWidgets.add(widgetItem);
        try {
            mWidgetContainer.addView(hostView);
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.widget_page_add_widget_error, Toast.LENGTH_LONG).show();
            mApplistLog.log(e);
        }
        mWidgetContainer.invalidate();
    }

    private void updateWidgetOnScreen(AppWidgetHostView appWidgetHostView, WidgetData widgetData) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetData.getWidth());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetData.getHeight());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetData.getWidth());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetData.getHeight());
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        appWidgetHostView.updateAppWidgetOptions(options);
        mScreenshotUtils.scheduleScreenshot(getActivity(), getPageId(), ScreenshotUtils.DELAY_SHORT);
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

        mScreenshotUtils.scheduleScreenshot(getActivity(), getPageId(), ScreenshotUtils.DELAY_SHORT);
    }

    private void moveWidgetToOtherPage(WidgetItem widgetItem) {
        Context c = getContext();
        if (c == null) {
            return;
        }

        AppWidgetProviderInfo appWidgetProviderInfo = mAppWidgetManager.getAppWidgetInfo(
                widgetItem.widgetData.getAppWidgetId());
        Bundle data = new Bundle();
        data.putInt(WidgetHelper.PAGE_PICK_REQUEST_KEY, WidgetHelper.PICK_PAGE_FOR_MOVING_WIDGET_REQUEST);
        data.putParcelable(WidgetHelper.WIDGET_DATA_KEY, widgetItem.widgetData);
        data.putParcelable(WidgetHelper.APP_WIDGET_PROVIDER_INFO_KEY, appWidgetProviderInfo);

        Intent pagePickerIntent = new Intent(c, PagePickerActivity.class);
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_TITLE,
                c.getString(R.string.page_picker_move_widget_title));
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_MESSAGE,
                c.getString(R.string.page_picker_message));
        pagePickerIntent.putExtra(
                PagePickerActivity.EXTRA_REQUEST_DATA,
                data);
        startActivity(pagePickerIntent);
    }

    private void bringWidgetToTop(final WidgetItem widgetItem) {
        Task.callInBackground((Callable<Void>) () -> {
            mWidgetModel.bringWidgetToTop(widgetItem.widgetData);
            return null;
        });
    }

    private void removeWidgetFromModel(final WidgetItem widgetItem) {
        Task.callInBackground((Callable<Void>) () -> {
            mWidgetModel.deleteWidget(widgetItem.widgetData);
            return null;
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
                .setItems(R.array.widget_page_menu, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Add widget
                            mApplistLog.analytics(ApplistLog.SHOW_WIDGET_PICKER, ApplistLog.WIDGET_PAGE_MENU);
                            mWidgetHelper.pickWidget(getActivity(), getPageId());
                            break;
                        case 1:
                            // Edit pages
                            mApplistLog.analytics(ApplistLog.SHOW_PAGE_EDITOR, ApplistLog.WIDGET_PAGE_MENU);
                            showPageEditor();
                            break;
                        case 2:
                            // Change wallpaper
                            mApplistLog.analytics(ApplistLog.CHANGE_WALLPAPER, ApplistLog.WIDGET_PAGE_MENU);
                            mLauncherUtils.changeWallpaper(getActivity());
                            break;
                    }
                    mPageMenu = null;
                })
                .setCancelable(true)
                .setOnDismissListener(dialog -> mPageMenu = null)
                .create();
        mPageMenu.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mPageMenu.show();
    }

    private void openWidgetMenu(WidgetItem widgetItem) {
        mWidgetMenu = new AlertDialog.Builder(getContext())
                .setItems(R.array.widget_menu, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            mApplistLog.analytics(ApplistLog.ADJUST_WIDGET, ApplistLog.WIDGET_MENU);
                            mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_RESIZING);
                            EventBus.getDefault().post(new WidgetMoveStartedEvent());
                            break;
                        case 1:
                            mApplistLog.analytics(ApplistLog.MOVE_WIDGET_TO_PAGE, ApplistLog.WIDGET_MENU);
                            mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                            moveWidgetToOtherPage(mWidgetMenuTarget);
                            break;
                        case 2:
                            mApplistLog.analytics(ApplistLog.RAISE_WIDGET, ApplistLog.WIDGET_MENU);
                            mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                            bringWidgetToTop(mWidgetMenuTarget);
                            break;
                        case 3:
                            mApplistLog.analytics(ApplistLog.DELETE_WIDGET, ApplistLog.WIDGET_MENU);
                            mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL);
                            removeWidgetFromModel(mWidgetMenuTarget);
                            break;
                    }
                    mWidgetMenu = null;
                })
                .setCancelable(true)
                .setOnCancelListener(dialog -> mWidgetMenuTarget.appWidgetHostView.setState(MyAppWidgetHostView.STATE_NORMAL))
                .setOnDismissListener(dialog -> mWidgetMenu = null)
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
            if (isLocationInsideWidget(widgetItem, locationX, locationY, 0)) {
                return widgetItem;
            }
        }
        return null;
    }

    private boolean isLocationInsideWidget(WidgetItem widgetItem, float locationX, float locationY, int margin) {
        widgetItem.appWidgetHostView.getLocationOnScreen(mTempLocation);
        mTempRect1.set(
                mTempLocation[0] - margin,
                mTempLocation[1] - margin,
                mTempLocation[0] + widgetItem.appWidgetHostView.getWidth() + margin,
                mTempLocation[1] + widgetItem.appWidgetHostView.getHeight() + margin);
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
        widgetItem.widgetData.setPositionX(Math.round(mScreenUtils.pxToDp(layoutParams.leftMargin)));
        widgetItem.widgetData.setPositionY(Math.round(mScreenUtils.pxToDp(layoutParams.topMargin)));
        widgetItem.widgetData.setWidth(Math.round(mScreenUtils.pxToDp(layoutParams.width)));
        widgetItem.widgetData.setHeight(Math.round(mScreenUtils.pxToDp(layoutParams.height)));

        mWidgetContainer.invalidate();

        Task.callInBackground((Callable<Void>) () -> {
            mWidgetModel.updateWidget(widgetItem.widgetData, false);
            return null;
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
