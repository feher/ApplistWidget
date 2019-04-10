package net.feheren_fekete.applist.applistpage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.launcher.LauncherUtils;
import net.feheren_fekete.applist.settings.SettingsActivity;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import androidx.fragment.app.Fragment;
import bolts.Continuation;
import bolts.Task;

public class ApplistPageFragment extends Fragment implements ApplistItemDragHandler.Listener {

    private static final String TAG = ApplistPageFragment.class.getSimpleName();

    public static final class ShowPageEditorEvent {}

    // TODO: Inject these singletons.
    private ApplistModel mApplistModel = ApplistModel.getInstance();
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();
    private ScreenUtils mScreenUtils = ScreenUtils.getInstance();
    private LauncherUtils mLauncherUtils = LauncherUtils.getInstance();
    private BadgeStore mBadgeStore = BadgeStore.getInstance();

    private Handler mHandler = new Handler();
    private FileUtils mFileUtils = new FileUtils();
    private IconPackHelper mIconPackHelper = new IconPackHelper();
    private IconCache mIconCache = new IconCache();
    private ApplistPreferences mApplistPreferences;
    private Toolbar mToolbar;
    private Drawable mToolbarGradient;
    private Menu mMenu;
    private SearchView mSearchView;
    private UpdateInstalledAppsRunnable mUpdateInstalledAppsRunnable = null;

    public static ApplistPageFragment newInstance(long pageId) {
        ApplistPageFragment fragment = new ApplistPageFragment();

        Bundle args = new Bundle();
        args.putLong("pageId", pageId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_page_fragment, container, false);

        mApplistPreferences = new ApplistPreferences(getContext());

        mToolbar = (Toolbar) view.findViewById(R.id.applist_page_fragment_toolbar);

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        mToolbar.setPadding(0, mScreenUtils.getStatusBarHeight(getContext()), 0, 0);

        mToolbar.setOnClickListener(mToolbarClickListener);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mToolbar);
        activity.getSupportActionBar().setTitle(R.string.toolbar_title);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);

        IntentFilter packageIntentFilter = new IntentFilter();
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageIntentFilter.addDataScheme("package");
        getContext().registerReceiver(mPackageStateReceiver, packageIntentFilter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mToolbarGradient == null) {
            mToolbarGradient = createToolbarGradient();
        }
        mToolbar.setBackground(mToolbarGradient);
        if (mSettingsUtils.getShowNewContentBadge()) {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mBadgeStore.updateBadgesFromLauncher();
                    return null;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateApplistFragmentDelayed();

        EventBus.getDefault().register(this);
        mPackageStateReceiver.onReceive(getContext(), null);

        String currentDeviceLocale = Locale.getDefault().toString();
        String savedDeviceLocale = mApplistPreferences.getDeviceLocale();
        if (!currentDeviceLocale.equals(savedDeviceLocale)) {
            mApplistPreferences.setDeviceLocale(currentDeviceLocale);
            updateData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSearchView != null) {
            hideKeyboardFrom(getContext(), mSearchView);
            mSearchView.setIconified(true);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(mPackageStateReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.applist_menu, menu);

        mMenu = menu;

        MenuItem searchItem = menu.findItem(R.id.action_search_app);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextFocusChangeListener(mSearchFocusListener);
        mSearchView.setOnQueryTextListener(mSearchTextListener);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isFilteredByName = false;
        ApplistPagePageFragment fragment = getApplistPagePageFragment();
        if (fragment != null) {
            isFilteredByName = fragment.isFilteredByName();
        }
        MenuItem menuItem;
        if (isFilteredByName) {
            menuItem = menu.findItem(R.id.action_search_app);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
            menuItem = menu.findItem(R.id.action_create_section);
            if (menuItem != null) {
                menuItem.setVisible(false);
            }
            menuItem = menu.findItem(R.id.action_settings);
            if (menuItem != null) {
                menuItem.setVisible(false);
            }
            menuItem = menu.findItem(R.id.action_edit_pages);
            if (menuItem != null) {
                menuItem.setVisible(false);
            }
        } else {
            menuItem = menu.findItem(R.id.action_search_app);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
            menuItem = menu.findItem(R.id.action_create_section);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
            menuItem = menu.findItem(R.id.action_settings);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
            menuItem = menu.findItem(R.id.action_edit_pages);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isHandled = false;
        int id = item.getItemId();

        ApplistPagePageFragment fragment = getApplistPagePageFragment();
        if (fragment != null) {
            isHandled = fragment.handleMenuItem(id);
        }

        if (!isHandled) {
            switch (id) {
                case R.id.action_settings:
                    showSettings();
                    isHandled = true;
                    break;
                case R.id.action_edit_pages:
                    showPageEditor();
                    isHandled = true;
                    break;
                case R.id.action_change_wallpaper:
                    mLauncherUtils.changeWallpaper(getActivity());
                    isHandled = true;
                    break;
            }
        }

        if (!isHandled) {
            isHandled = super.onOptionsItemSelected(item);
        }
        return isHandled;
    }

    @Override
    public void onItemDragStart() {
    }

    @Override
    public void onItemDragEnd() {
    }

    private Drawable createToolbarGradient() {
        // REF: 2017_06_30_toolbar_gradient
        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.toolbarBackgroundColor, typedValue, true);
        int startColor = typedValue.data;
        int endColor;
        if (mSettingsUtils.isThemeTransparent()) {
            endColor = (startColor & 0xffffff) | 0x55000000;
        } else {
            startColor = (startColor & 0xffffff) | 0x88000000;
            endColor = startColor;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setColors(new int[]{startColor, endColor});
        return drawable;
    }

    private void showPageEditor() {
        EventBus.getDefault().post(new ShowPageEditorEvent());
    }

    private void showSettings() {
        Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private class UpdateInstalledAppsRunnable implements Runnable {
        private Context appContext;
        private Uri uri;
        UpdateInstalledAppsRunnable(Context appContext, Uri uri) {
            this.appContext = appContext;
            this.uri = uri;
        }
        @Override
        public void run() {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (appContext == null) {
                        return null;
                    }
                    if (uri != null) {
                        mFileUtils.deleteFiles(
                                mFileUtils.getIconCacheDirPath(appContext),
                                uri.getSchemeSpecificPart());
                    }
                    mApplistModel.updateInstalledApps();
                    mBadgeStore.cleanup();
                    appContext = null;
                    return null;
                }
            });
        }
    }

    private void scheduleUpdateInstalledApps(Context appContext, Uri uri) {
        if (mUpdateInstalledAppsRunnable != null) {
            mHandler.removeCallbacks(mUpdateInstalledAppsRunnable);
            mUpdateInstalledAppsRunnable = null;
        }
        mUpdateInstalledAppsRunnable = new UpdateInstalledAppsRunnable(appContext, uri);
        mHandler.postDelayed(mUpdateInstalledAppsRunnable, 2000);
    }

    private BroadcastReceiver mPackageStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri uri = (intent != null) ? intent.getData() : null;
            final Context appContext = context.getApplicationContext();
            scheduleUpdateInstalledApps(appContext, uri);
        }
    };

    private SearchView.OnFocusChangeListener mSearchFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ApplistPagePageFragment fragment = getApplistPagePageFragment();
            if (fragment != null) {
                if (!hasFocus) {
                    stopFilteringByName(fragment);
                } else {
                    startFilteringByName(fragment);
                }
            }
        }
    };

    private SearchView.OnQueryTextListener mSearchTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            ApplistPagePageFragment fragment = getApplistPagePageFragment();
            if (fragment != null) {
                if (newText == null || newText.isEmpty()) {
                    fragment.setNameFilter("");
                } else {
                    fragment.setNameFilter(newText);
                }
            }
            return true;
        }
    };

    private View.OnClickListener mToolbarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSearchView != null) {
                mSearchView.setIconified(false);
            }
        }
    };

    private void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void startFilteringByName(ApplistPagePageFragment fragment) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle("");
        fragment.activateNameFilter();
        onPrepareOptionsMenu(mMenu);
    }

    private void stopFilteringByName(ApplistPagePageFragment fragment) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.toolbar_title);
        fragment.deactivateNameFilter();
        mSearchView.setIconified(true);
        onPrepareOptionsMenu(mMenu);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadedEvent(ApplistModel.DataLoadedEvent event) {
        updateApplistFragmentDelayed();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPagesChangedEvent(ApplistModel.PagesChangedEvent event) {
        updateApplistFragmentDelayed();
    }

    private long getPageId() {
        return getArguments().getLong("pageId");
    }

    private void updateData() {
        final ApplistModel applistModel = ApplistModel.getInstance();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                applistModel.updateInstalledApps();
                return null;
            }
        });
    }

    private void loadApplistFragment() {
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return mApplistModel.getPageNames();
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                List<String> pageNames = task.getResult();
                if (!pageNames.isEmpty()) {
                    showApplistFragment(pageNames.get(0));
                } else {
                    ApplistLog.getInstance().log(new RuntimeException("No pages found!"));
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void addDefaultPage() {
        final String defaultPageName = getResources().getString(R.string.default_page_name);
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mApplistModel.addNewPage(defaultPageName);
                return null;
            }
        });
    }

    private void updateApplistFragmentDelayed() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    updateApplistFragment();
                }
            }
        });
    }

    private void updateApplistFragment() {
        if (mApplistModel.getPageCount() == 0) {
            addDefaultPage();
        } else {
            loadApplistFragment();
        }
    }

    private void showApplistFragment(String pageName) {
        ApplistPagePageFragment fragment = ApplistPagePageFragment.newInstance(
                pageName, getPageId(), mIconPackHelper, mIconCache, this);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.applist_page_fragment_page_container, fragment, ApplistPagePageFragment.class.getName())
                .commit();
    }

    private ApplistPagePageFragment getApplistPagePageFragment() {
        return (ApplistPagePageFragment) getChildFragmentManager().findFragmentByTag(
                ApplistPagePageFragment.class.getName());
    }

}
