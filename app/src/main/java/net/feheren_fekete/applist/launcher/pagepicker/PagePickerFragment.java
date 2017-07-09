package net.feheren_fekete.applist.launcher.pagepicker;

import android.appwidget.AppWidgetProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorFragment;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.WidgetHelper;
import net.feheren_fekete.applist.widgetpage.WidgetUtils;

public class PagePickerFragment extends Fragment {

    private static final String FRAGMENT_ARG_REQUEST_DATA = PagePickerFragment.class.getSimpleName() + ".FRAGMENT_ARG_REQUEST_DATA";
    private static final String FRAGMENT_ARG_TITLE = PagePickerFragment.class.getCanonicalName() + ".FRAGMENT_ARG_TITLE";
    private static final String FRAGMENT_ARG_MESSAGE = PagePickerFragment.class.getCanonicalName() + ".FRAGMENT_ARG_MESSAGE";

    // TODO: Inject
    private final ScreenUtils mScreenUtils = ScreenUtils.getInstance();
    private final ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private final WidgetUtils mWidgetUtils = WidgetUtils.getInstance();

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

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        final int topPadding = mScreenUtils.getStatusBarHeight(getContext());
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        final int bottomPadding = mScreenUtils.hasNavigationBar(getContext()) ? mScreenUtils.getNavigationBarHeight(getContext()) : 0;
        view.findViewById(R.id.page_picker_fragment_layout).setPadding(0, topPadding, 0, bottomPadding);

        Toolbar toolbar = view.findViewById(R.id.page_picker_fragment_toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setTitle(getArguments().getString(FRAGMENT_ARG_TITLE));
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mRequestData = getArguments().getBundle(FRAGMENT_ARG_REQUEST_DATA);

        AppWidgetProviderInfo appWidgetProviderInfo = mRequestData.getParcelable(WidgetHelper.APP_WIDGET_PROVIDER_INFO_KEY);
        Drawable widgetIcon = mWidgetUtils.getIcon(getContext(), appWidgetProviderInfo);
        Drawable widgetPreview = mWidgetUtils.getPreviewImage(getContext(), appWidgetProviderInfo);
        String widgetLabel = mWidgetUtils.getLabel(getContext(), appWidgetProviderInfo);

        TextView label = view.findViewById(R.id.page_picker_fragment_widget_label);
        label.setText(widgetLabel);
        if (widgetIcon != null) {
            widgetIcon.mutate();
            final int iconSize = Math.round(mScreenUtils.dpToPx(getContext(), 24));
            widgetIcon.setBounds(0, 0, iconSize, iconSize);
        }
        label.setCompoundDrawables(widgetIcon, null, null, null);

        ImageView preview = view.findViewById(R.id.page_picker_fragment_widget_preview);
        preview.setImageDrawable(widgetPreview != null ? widgetPreview : widgetIcon);

        TextView message = view.findViewById(R.id.page_picker_fragment_message);
        message.setText(getArguments().getString(FRAGMENT_ARG_MESSAGE));

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.page_picker_fragment_fragment_container,
                        PageEditorFragment.newInstance(false, true, mRequestData))
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScreenshotUtils.cancelScheduledScreenshot();
    }

}
