package net.feheren_fekete.applist.launcherpage;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.model.WidgetData;
import net.feheren_fekete.applist.model.WidgetModel;
import net.feheren_fekete.applist.utils.ScreenUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class LauncherPageFragment extends Fragment {

    private static final int REQUEST_PICK_APPWIDGET = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 2;

    private static final int NO_BORDER = 0;
    private static final int TOP_BORDER = 1;
    private static final int BOTTOM_BORDER = 1 << 1;
    private static final int LEFT_BORDER = 1 << 2;
    private static final int RIGHT_BORDER = 1 << 3;

    private static final class WidgetItem {
        public WidgetData widgetData;
        public AppWidgetHostView appWidgetHostView;
    }

    private WidgetModel mWidgetModel;
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;
    private ViewGroup mWidgetContainer;
    private List<WidgetItem> mWidgets = new ArrayList<>();
    private @Nullable PopupMenu mWidgetMenu;
    private @Nullable WidgetItem mWidgetMenuTarget;
    private int[] mTempLocation = new int[2];
    private RectF mTempRect1 = new RectF();
    private RectF mTempRect2 = new RectF();
    private boolean mIsMovingWidget;

    public static LauncherPageFragment newInstance(int pageNumber) {
        LauncherPageFragment fragment = new LauncherPageFragment();

        Bundle args = new Bundle();
        args.putInt("pageNumber", pageNumber);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWidgetModel = new WidgetModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_page_fragment, container, false);

        mAppWidgetManager = AppWidgetManager.getInstance(getContext().getApplicationContext());
        mAppWidgetHost = new AppWidgetHost(getContext().getApplicationContext(), 1234567);

        mWidgetContainer = (ViewGroup) view.findViewById(R.id.launcher_page_fragment_container);

        view.findViewById(R.id.launcher_page_fragment_add_widget_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWidget();
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            int appWidgetId =
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
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

    @Override
    public void onStop() {
        super.onStop();
        mAppWidgetHost.stopListening();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDataLoadedEvent(WidgetModel.DataLoadedEvent event) {
        // TODO: update/populate mWidgets
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetAddedEvent(WidgetModel.WidgetAddedEvent event) {
        // TODO: update/populate mWidgets
    }

    public int getPageNumber() {
        return getArguments().getInt("pageNumber");
    }

    public void handleLongTap(MotionEvent event) {
        WidgetItem widgetItem = findWidgetAtLocation(event.getRawX(), event.getRawY());
        if (widgetItem != null) {
            openWidgetMenu(widgetItem);
        }
    }

    private PointF mOriginalFingerPos = new PointF();
    private PointF mPreviousFingerPos = new PointF();

    public boolean handleScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        boolean handled = false;
        if (mIsMovingWidget && mWidgetMenuTarget != null) {
            if (mOriginalFingerPos.x != event1.getRawX() || mOriginalFingerPos.y != event1.getRawY()) {
                mOriginalFingerPos.set(event1.getRawX(), event2.getRawY());
                mPreviousFingerPos.set(event1.getRawX(), event2.getRawY());
            }

            if (isLocationInsideWidget(mWidgetMenuTarget, mPreviousFingerPos.x, mPreviousFingerPos.y)) {
                updateWidgetLocation(
                        mWidgetMenuTarget,
                        mPreviousFingerPos.x, mPreviousFingerPos.y,
                        event2.getRawX(), event2.getRawY());
                handled = true;
            } else {
                mIsMovingWidget = false;
            }

            mPreviousFingerPos.set(event2.getRawX(), event2.getRawY());
        }
        return handled;
    }

    private void selectWidget() {
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
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

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        if (!mWidgets.isEmpty()) {
            removeWidget(0);
        }

        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = mAppWidgetHost.createView(getContext().getApplicationContext(), appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                Math.round(ScreenUtils.dpToPx(getContext(), 200)),
                Math.round(ScreenUtils.dpToPx(getContext(), 300)));
        layoutParams.topMargin = 0;
        layoutParams.leftMargin = 0;
        hostView.setLayoutParams(layoutParams);

        WidgetItem widgetItem = new WidgetItem();
        widgetItem.widgetData = new WidgetData(
                appWidgetInfo.provider.getPackageName(),
                appWidgetInfo.provider.getClassName(),
                getPageNumber(),
                0, 0, 200, 300);
        widgetItem.appWidgetHostView = hostView;
        mWidgets.add(widgetItem);

        mWidgetContainer.addView(hostView);
        mWidgetModel.addWidget(widgetItem.widgetData);
    }

    private void removeWidget(int widgetIndex) {
        WidgetItem widgetItem = mWidgets.get(widgetIndex);
        mAppWidgetHost.deleteAppWidgetId(widgetItem.appWidgetHostView.getAppWidgetId());
        mWidgetContainer.removeView(widgetItem.appWidgetHostView);
        mWidgets.remove(widgetIndex);
        mWidgetModel.deleteWidget(widgetItem.widgetData);
    }

    private void openWidgetMenu(WidgetItem widgetItem) {
        mWidgetMenu = new PopupMenu(getContext(), widgetItem.appWidgetHostView);
        mWidgetMenu.setOnMenuItemClickListener(mWidgetMenuClickListener);
        mWidgetMenu.inflate(R.menu.widget_menu);
        mWidgetMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                mWidgetMenu = null;
            }
        });
        mWidgetMenu.show();
        mWidgetMenuTarget = widgetItem;
    }

    private PopupMenu.OnMenuItemClickListener mWidgetMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            boolean handled = false;
            switch (menuItem.getItemId()) {
                case R.id.widget_move_resize:
                    mIsMovingWidget = true;
                    handled = true;
                    break;
                case R.id.widget_remove:
                    handled = true;
                    break;
            }
            mWidgetMenu = null;
            return handled;
        }
    };

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

    private int mWidgetTouchBorderWidthHalf = 25; // px
    private int mMinWidgetSize = 100; // px
    private void updateWidgetLocation(WidgetItem widgetItem,
                                      float location1X, float location1Y,
                                      float location2X, float location2Y) {
        final int touchedBorder = isLocationOnTouchBorder(widgetItem, location1X, location1Y);
        if (touchedBorder != NO_BORDER) {
            // Resizing
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) widgetItem.appWidgetHostView.getLayoutParams();
            if ((touchedBorder & LEFT_BORDER) != 0) {
                layoutParams.leftMargin += Math.round(location2X - location1X);
                layoutParams.leftMargin = Math.max(0, layoutParams.leftMargin);
            }
            if ((touchedBorder & TOP_BORDER) != 0) {
                layoutParams.topMargin += Math.round(location2Y - location1Y);
                layoutParams.topMargin = Math.max(0, layoutParams.topMargin);
            }
            if ((touchedBorder & RIGHT_BORDER) != 0) {
                layoutParams.width += Math.round(location2X - location1X);
                layoutParams.width = Math.max(mMinWidgetSize, layoutParams.width);
            }
            if ((touchedBorder & BOTTOM_BORDER) != 0) {
                layoutParams.height += Math.round(location2Y - location1Y);
                layoutParams.height = Math.max(mMinWidgetSize, layoutParams.height);
            }
            widgetItem.appWidgetHostView.setLayoutParams(layoutParams);
            mWidgetContainer.invalidate();
        } else {
            // Moving
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) widgetItem.appWidgetHostView.getLayoutParams();
            final float distanceX = location2X - location1X;
            final float distanceY = location2Y - location1Y;
            layoutParams.leftMargin += distanceX;
            layoutParams.topMargin += distanceY;
            widgetItem.appWidgetHostView.setLayoutParams(layoutParams);
            mWidgetContainer.invalidate();
        }
    }

    private int isLocationOnTouchBorder(WidgetItem widgetItem, float locationX, float locationY) {
        int result = NO_BORDER;

        widgetItem.appWidgetHostView.getLocationOnScreen(mTempLocation);
        final int widgetLeft = mTempLocation[0];
        final int widgetTop = mTempLocation[1];
        final int widgetRight = mTempLocation[0] + widgetItem.appWidgetHostView.getWidth();
        final int widgetBottom = mTempLocation[1] + widgetItem.appWidgetHostView.getHeight();

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidthHalf,
                widgetTop - mWidgetTouchBorderWidthHalf,
                widgetRight + mWidgetTouchBorderWidthHalf,
                widgetTop + mWidgetTouchBorderWidthHalf);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= TOP_BORDER;
        }

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidthHalf,
                widgetBottom - mWidgetTouchBorderWidthHalf,
                widgetRight + mWidgetTouchBorderWidthHalf,
                widgetBottom + mWidgetTouchBorderWidthHalf);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= BOTTOM_BORDER;
        }

        mTempRect1.set(
                widgetLeft - mWidgetTouchBorderWidthHalf,
                widgetTop - mWidgetTouchBorderWidthHalf,
                widgetLeft + mWidgetTouchBorderWidthHalf,
                widgetBottom + mWidgetTouchBorderWidthHalf);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= LEFT_BORDER;
        }

        mTempRect1.set(
                widgetRight - mWidgetTouchBorderWidthHalf,
                widgetTop - mWidgetTouchBorderWidthHalf,
                widgetRight + mWidgetTouchBorderWidthHalf,
                widgetBottom + mWidgetTouchBorderWidthHalf);
        if (mTempRect1.contains(locationX, locationY)) {
            result |= RIGHT_BORDER;
        }

        return result;
    }

}
