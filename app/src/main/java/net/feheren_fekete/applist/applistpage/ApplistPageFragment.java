package net.feheren_fekete.applist.applistpage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.settings.SettingsActivity;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.FileUtils;
import net.feheren_fekete.applist.utils.ScreenUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistPageFragment extends Fragment implements ApplistItemDragHandler.Listener {

    private static final String TAG = ApplistPageFragment.class.getSimpleName();

    public static final class ShowPageEditorEvent {}

    // TODO: Inject these singletons.
    private ApplistModel mApplistModel = ApplistModel.getInstance();

    private Handler mHandler = new Handler();
    private FileUtils mFileUtils = new FileUtils();
    private IconCache mIconCache = new IconCache();
    private BadgeStore mBadgeStore;
    private ApplistPreferences mApplistPreferences;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private FrameLayout mFragmentContainer;
    private Menu mMenu;
    private SearchView mSearchView;
    private int mAppBarBottomBeforeItemDrag;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_fragment, container, false);

        mBadgeStore = new BadgeStore(getContext(), getContext().getPackageManager(), new BadgeUtils(getContext()));
        mApplistPreferences = new ApplistPreferences(getContext());

        mAppBarLayout = (AppBarLayout) view.findViewById(R.id.applist_fragment_appbar_layout);
        mToolbar = (Toolbar) view.findViewById(R.id.applist_fragment_toolbar);
        if (SettingsUtils.isThemeTransparent(getContext())) {
            ScreenUtils.setStatusBarTranslucent(getActivity(), true);
            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            layoutParams.height += ScreenUtils.getStatusBarHeight(getContext());
            mToolbar.setLayoutParams(layoutParams);
            mToolbar.setPadding(0, ScreenUtils.getStatusBarHeight(getContext()), 0, 0);
        }
        mToolbar.setOnClickListener(mToolbarClickListener);
        mToolbar.setTitle(R.string.toolbar_title);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mToolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);

        mFragmentContainer = (FrameLayout) view.findViewById(R.id.applist_fragment_fragment_container);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        getContext().registerReceiver(mPackageStateReceiver, intentFilter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (SettingsUtils.getShowBadge(getContext())) {
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
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextFocusChangeListener(mSearchFocusListener);
        mSearchView.setOnQueryTextListener(mSearchTextListener);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isFilteredByName = false;
        ApplistPagePageFragment fragment = getApplistFragment();
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

        ApplistPagePageFragment fragment = getApplistFragment();
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
            }
        }

        if (!isHandled) {
            isHandled = super.onOptionsItemSelected(item);
        }
        return isHandled;
    }

    @Override
    public void onItemDragStart() {
        // Manually move the appbar out of the screen.
        // I tried all kinds of other ways to achieve this but I gave up. This one works.
        //
        // We use CoordinatorLayout with AppBarLayout and RecyclerView (inside a fragment).
        // For this reason, we must also manually restore the translation values otherwise
        // AppBarLayout gets confused about its own location.
        //
        mAppBarBottomBeforeItemDrag = mAppBarLayout.getBottom();
        mAppBarLayout.animate().translationYBy(-mAppBarBottomBeforeItemDrag).setDuration(150);
        mFragmentContainer.animate().translationYBy(-mAppBarBottomBeforeItemDrag).setDuration(150);
    }

    @Override
    public void onItemDragEnd() {
        // Manually restore the appbar's position on the screen.
        // I tried all kinds of other ways to achieve this but I gave up. This one works.
        //
        // We use CoordinatorLayout with AppBarLayout and RecyclerView (inside a fragment).
        // For this reason, we must also manually restore the translation values otherwise
        // AppBarLayout gets confused about its own location.
        //
        mAppBarLayout.animate().translationYBy(mAppBarBottomBeforeItemDrag).setDuration(150);
        mFragmentContainer.animate().translationYBy(mAppBarBottomBeforeItemDrag).setDuration(150);
    }

    private void showPageEditor() {
        EventBus.getDefault().post(new ShowPageEditorEvent());
    }

    private void showSettings() {
        Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private BroadcastReceiver mPackageStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri uri = (intent != null) ? intent.getData() : null;
            final Context appContext = context.getApplicationContext();
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (uri != null) {
                        mFileUtils.deleteFiles(
                                mFileUtils.getIconCacheDirPath(appContext),
                                uri.getSchemeSpecificPart());
                    }
                    mApplistModel.updateInstalledApps();
                    mBadgeStore.cleanup();
                    return null;
                }
            });
        }
    };

    private SearchView.OnFocusChangeListener mSearchFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ApplistPagePageFragment fragment = getApplistFragment();
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
            ApplistPagePageFragment fragment = getApplistFragment();
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
        fragment.activateNameFilter();
        onPrepareOptionsMenu(mMenu);
    }

    private void stopFilteringByName(ApplistPagePageFragment fragment) {
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
                updateApplistFragment();
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
        ApplistPagePageFragment fragment = ApplistPagePageFragment.newInstance(pageName, mApplistModel, mIconCache, this);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.applist_fragment_fragment_container, fragment, ApplistPagePageFragment.class.getName())
                .commit();
    }

    private ApplistPagePageFragment getApplistFragment() {
        return (ApplistPagePageFragment) getChildFragmentManager().findFragmentByTag(
                ApplistPagePageFragment.class.getName());
    }

}
