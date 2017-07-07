package net.feheren_fekete.applist.widgetpage.widgetpicker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.widgetpage.WidgetUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public class WidgetPickerActivity extends AppCompatActivity implements WidgetPickerViewHolder.Listener {

    public static final String ACTION_PICK_AND_BIND_WIDGET = WidgetPickerActivity.class.getCanonicalName() + ".ACTION_PICK_AND_BIND_WIDGET";
    public static final String ACTION_BIND_WIDGET = WidgetPickerActivity.class.getCanonicalName() + ".ACTION_BIND_WIDGET";

    public static final String EXTRA_WIDGET_PROVIDER = WidgetPickerActivity.class.getCanonicalName() + ".EXTRA_WIDGET_PROVIDER";

    public static final String EXTRA_TOP_PADDING = WidgetPickerActivity.class.getCanonicalName() + ".EXTRA_TOP_PADDING";
    public static final String EXTRA_BOTTOM_PADDING = WidgetPickerActivity.class.getCanonicalName() + ".EXTRA_BOTTOM_PADDING";
    public static final String EXTRA_WIDGET_WIDTH = WidgetPickerActivity.class.getCanonicalName() + ".EXTRA_WIDGET_WIDTH";
    public static final String EXTRA_WIDGET_HEIGHT = WidgetPickerActivity.class.getCanonicalName() + ".EXTRA_WIDGET_HEIGHT";

    private static final int REQUEST_BIND_APPWIDGET = 1;

    // TODO: Inject
    private final WidgetUtils mWidgetUtils = WidgetUtils.getInstance();

    private WidgetPickerModel mWidgetPickerModel;
    private WidgetPickerAdapter mWidgetPickerAdapter;
    private RecyclerView mRecyclerView;

    private int mAppWidgetId;
    private int mWidgetWidth;
    private int mWidgetHeight;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);

        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mWidgetWidth = getIntent().getIntExtra(EXTRA_WIDGET_WIDTH, -1);
        mWidgetHeight = getIntent().getIntExtra(EXTRA_WIDGET_HEIGHT, -1);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID
                || mWidgetWidth == -1
                || mWidgetHeight == -1) {
            finish();
            return;
        }

        if (ACTION_BIND_WIDGET.equals(getIntent().getAction())) {
            ComponentName widgetProvider = getIntent().getParcelableExtra(EXTRA_WIDGET_PROVIDER);
            bindWidget(widgetProvider);
        } else {
            setContentView(R.layout.widget_picker_activity);

            final int topPadding = getIntent().getIntExtra(EXTRA_TOP_PADDING, 0);
            final int bottomPadding = getIntent().getIntExtra(EXTRA_BOTTOM_PADDING, 0);
            findViewById(R.id.widget_picker_activity_layout).setPadding(0, topPadding, 0, bottomPadding);

            Toolbar toolbar = (Toolbar) findViewById(R.id.widget_picker_activity_toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(R.string.widget_picker_title);

            mWidgetPickerModel = new WidgetPickerModel(this);
            mWidgetPickerAdapter = new WidgetPickerAdapter(this);
            mRecyclerView = (RecyclerView) findViewById(R.id.widget_picker_activity_widget_list);
            mRecyclerView.setLayoutManager(new GridLayoutManager(mRecyclerView.getContext(), 2));
            mRecyclerView.setAdapter(mWidgetPickerAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mWidgetPickerModel.loadData();
                return null;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadedEvent(WidgetPickerModel.DataLoadedEvent event) {
        List<WidgetPickerData> widgetPickerDatas = mWidgetPickerModel.getWidgets();
        List<WidgetPickerItem> widgetPickerItems = new ArrayList<>();
        for (WidgetPickerData widgetPickerData : widgetPickerDatas) {
            widgetPickerItems.add(new WidgetPickerItem(widgetPickerData));
        }
        Collections.sort(widgetPickerItems, new Comparator<WidgetPickerItem>() {
            @Override
            public int compare(WidgetPickerItem a, WidgetPickerItem b) {
                final String labelA = a.getLabel(WidgetPickerActivity.this, mWidgetUtils);
                final String labelB = b.getLabel(WidgetPickerActivity.this, mWidgetUtils);
                if (labelB == null) {
                    return 1;
                }
                if (labelA == null) {
                    return -1;
                }
                return labelA.compareTo(labelB);
            }
        });
        mWidgetPickerAdapter.setItems(widgetPickerItems);
    }

    @Override
    public void onWidgetTapped(int position) {
        WidgetPickerItem widgetPickerItem = mWidgetPickerAdapter.getItem(position);
        bindWidget(widgetPickerItem.getWidgetPickerData().getAppWidgetProviderInfo().provider);
    }

    private void bindWidget(ComponentName widgetProvider) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, mWidgetWidth);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, mWidgetHeight);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, mWidgetWidth);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, mWidgetHeight);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        boolean success = appWidgetManager.bindAppWidgetIdIfAllowed(mAppWidgetId, widgetProvider, options);
        if (success) {
            finishWithSuccess();
        } else {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widgetProvider);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, android.os.Process.myUserHandle());
            // This is the options bundle discussed above
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
            startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_BIND_APPWIDGET) {
                finishWithSuccess();
                return;
            }
        }
        finish();
    }

    private void finishWithSuccess() {
        Intent resultData = new Intent();
        resultData.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(Activity.RESULT_OK, resultData);
        finish();
    }

}
