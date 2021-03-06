package net.feheren_fekete.applist.launcher.pagepicker;

import android.appwidget.AppWidgetProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorFragment;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.WidgetHelper;
import net.feheren_fekete.applist.widgetpage.WidgetUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import static org.koin.java.KoinJavaComponent.get;

public class PagePickerFragment extends Fragment {

    private static final String FRAGMENT_ARG_REQUEST_DATA = PagePickerFragment.class.getSimpleName() + ".FRAGMENT_ARG_REQUEST_DATA";
    private static final String FRAGMENT_ARG_TITLE = PagePickerFragment.class.getCanonicalName() + ".FRAGMENT_ARG_TITLE";
    private static final String FRAGMENT_ARG_MESSAGE = PagePickerFragment.class.getCanonicalName() + ".FRAGMENT_ARG_MESSAGE";

    private final ScreenUtils mScreenUtils = get(ScreenUtils.class);
    private final ScreenshotUtils mScreenshotUtils = get(ScreenshotUtils.class);
    private final WidgetUtils mWidgetUtils = get(WidgetUtils.class);

    private Bundle mRequestData;

    public static PagePickerFragment newInstance(String title,
                                                 String message,
                                                 Bundle requestData) {
        PagePickerFragment pagePickerFragment = new PagePickerFragment();
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARG_TITLE, title);
        args.putString(FRAGMENT_ARG_MESSAGE, message);
        args.putBundle(FRAGMENT_ARG_REQUEST_DATA, requestData);
        pagePickerFragment.setArguments(args);
        return pagePickerFragment;
    }

    public PagePickerFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_picker_fragment, container, false);

        Bundle args = getArguments();
        if (args == null) {
            ApplistLog.getInstance().log(new RuntimeException("Missing arguments"));
            return view;
        }

//        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
//        final int topPadding = mScreenUtils.getStatusBarHeight(getContext());
//        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
//        final int bottomPadding = mScreenUtils.hasNavigationBar(getContext()) ? mScreenUtils.getNavigationBarHeight(getContext()) : 0;
//        view.findViewById(R.id.page_picker_fragment_layout).setPadding(0, topPadding, 0, bottomPadding);

        Toolbar toolbar = view.findViewById(R.id.page_picker_fragment_toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(args.getString(FRAGMENT_ARG_TITLE, ""));
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mRequestData = args.getBundle(FRAGMENT_ARG_REQUEST_DATA);
        if (mRequestData == null) {
            return view;
        }

        AppWidgetProviderInfo appWidgetProviderInfo = mRequestData.getParcelable(WidgetHelper.APP_WIDGET_PROVIDER_INFO_KEY);
        if (appWidgetProviderInfo == null) {
            ApplistLog.getInstance().log(new RuntimeException("Missing APP_WIDGET_PROVIDER_INFO_KEY"));
            return view;
        }
        Drawable widgetIcon = mWidgetUtils.getIcon(getContext(), appWidgetProviderInfo);
        Drawable widgetPreview = mWidgetUtils.getPreviewImage(getContext(), appWidgetProviderInfo);
        String widgetLabel = mWidgetUtils.getLabel(getContext(), appWidgetProviderInfo);

        TextView label = view.findViewById(R.id.page_picker_fragment_widget_label);
        label.setText(widgetLabel);
        if (widgetIcon != null) {
            widgetIcon.mutate();
            final int iconSize = Math.round(mScreenUtils.dpToPx(24));
            widgetIcon.setBounds(0, 0, iconSize, iconSize);
        }
        label.setCompoundDrawables(widgetIcon, null, null, null);

        ImageView preview = view.findViewById(R.id.page_picker_fragment_widget_preview);
        preview.setImageDrawable(widgetPreview != null ? widgetPreview : widgetIcon);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.page_picker_fragment_fragment_container,
                         PageEditorFragment.Companion.newInstance(false, true, mRequestData))
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScreenshotUtils.cancelScheduledScreenshot();
        Bundle args = getArguments();
        if (args == null) {
            ApplistLog.getInstance().log(new RuntimeException("Missing arguments"));
            return;
        }
        Toast toast = Toast.makeText(
                getContext(),
                args.getString(FRAGMENT_ARG_MESSAGE, ""),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
