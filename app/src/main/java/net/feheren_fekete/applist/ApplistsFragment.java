package net.feheren_fekete.applist;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import net.feheren_fekete.applist.model.BadgeStore;
import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.shortcutbadge.BadgeUtils;
import net.feheren_fekete.applist.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistsFragment extends Fragment {

    private static final String TAG = ApplistsFragment.class.getSimpleName();

    private Handler mHandler;
    private DataModel mDataModel;
    private IconCache mIconCache;
    private BadgeStore mBadgeStore;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private Menu mMenu;
    private SearchView mSearchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applists_fragment, container, false);

        mHandler = new Handler();
        mDataModel = DataModel.getInstance();
        mIconCache = new IconCache();
        mBadgeStore = new BadgeStore(getContext(), getContext().getPackageManager(), new BadgeUtils(getContext()));

        mAppBarLayout = (AppBarLayout) view.findViewById(R.id.appbar_layout);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setOnClickListener(mToolbarClickListener);
        mToolbar.setTitle(R.string.toolbar_title);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mToolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        setHasOptionsMenu(true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        getContext().registerReceiver(mPackageStateReceiver, intentFilter);

        loadData();

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
        EventBus.getDefault().register(this);
        mPackageStateReceiver.onReceive(getContext(), null);
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
        boolean isChangingOrder = false;
        boolean isFilteredByName = false;
        ApplistFragment fragment = getApplistFragment();
        if (fragment != null) {
            isChangingOrder = fragment.isChangingOrder();
            isFilteredByName = fragment.isFilteredByName();
        }
        if (isChangingOrder) {
            setMenuItemVisibility(menu, R.id.action_search_app, false);
            setMenuItemVisibility(menu, R.id.action_create_section, false);
            setMenuItemVisibility(menu, R.id.action_done, true);
            setMenuItemVisibility(menu, R.id.action_settings, false);
        } else if (isFilteredByName) {
            setMenuItemVisibility(menu, R.id.action_search_app, true);
            setMenuItemVisibility(menu, R.id.action_create_section, false);
            setMenuItemVisibility(menu, R.id.action_done, false);
            setMenuItemVisibility(menu, R.id.action_settings, false);
        } else {
            setMenuItemVisibility(menu, R.id.action_search_app, true);
            setMenuItemVisibility(menu, R.id.action_create_section, true);
            setMenuItemVisibility(menu, R.id.action_done, false);
            setMenuItemVisibility(menu, R.id.action_settings, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isHandled = false;
        int id = item.getItemId();

        ApplistFragment fragment = getApplistFragment();
        if (fragment != null) {
            isHandled = fragment.handleMenuItem(id);
        }

        if (!isHandled) {
            switch (id) {
                case R.id.action_settings:
                    showSettings();
                    isHandled = true;
                    break;
            }
        }

        if (!isHandled) {
            isHandled = super.onOptionsItemSelected(item);
        }
        return isHandled;
    }

    private void setMenuItemVisibility(Menu menu, int menuItemId, boolean visible) {
        MenuItem menuItem = menu.findItem(menuItemId);
        if (menuItem != null) {
            menuItem.setVisible(visible);
        }
    }

    private void showSettings() {
        Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private BroadcastReceiver mPackageStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri uri = (intent != null) ? intent.getData() : null;
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (uri != null) {
                        FileUtils.deleteFiles(
                                FileUtils.getIconCacheDirPath(ApplistsFragment.this.getContext()),
                                uri.getSchemeSpecificPart());
                    }
                    mDataModel.updateInstalledApps();
                    mBadgeStore.cleanup();
                    return null;
                }
            });
        }
    };

    private SearchView.OnFocusChangeListener mSearchFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ApplistFragment fragment = getApplistFragment();
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
            ApplistFragment fragment = getApplistFragment();
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

    private ApplistFragment.Listener mFragmentListener = new ApplistFragment.Listener() {
        @Override
        public void onChangeSectionOrderStart() {
            mAppBarLayout.setExpanded(true);
            mToolbar.setOnClickListener(null);
            mToolbar.setTitle("");
            mToolbar.setNavigationIcon(R.drawable.ic_close);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ApplistFragment fragment = getApplistFragment();
                    if (fragment != null) {
                        fragment.cancelChangingOrder();
                    }
                }
            });
            Toast toast = Toast.makeText(
                    ApplistsFragment.this.getContext(),
                    R.string.toast_change_section_position,
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        @Override
        public void onChangeSectionOrderEnd() {
            mToolbar.setOnClickListener(mToolbarClickListener);
            mToolbar.setTitle(R.string.toolbar_title);
            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationOnClickListener(null);
        }
    };

    private void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void startFilteringByName(ApplistFragment fragment) {
        fragment.activateNameFilter();
        onPrepareOptionsMenu(mMenu);
    }

    private void stopFilteringByName(ApplistFragment fragment) {
        fragment.deactivateNameFilter();
        mSearchView.setIconified(true);
        onPrepareOptionsMenu(mMenu);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDataLoadedEvent(DataModel.DataLoadedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateApplistFragment();
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPagesChangedEvent(DataModel.PagesChangedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateApplistFragment();
            }
        });
    }

    private void loadData() {
        final DataModel dataModel = DataModel.getInstance();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                dataModel.loadData();
                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                dataModel.updateInstalledApps();
                                return null;
                            }
                        });
                    }
                }, 1000);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void loadApplistFragment() {
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return mDataModel.getPageNames();
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                List<String> pageNames = task.getResult();
                if (!pageNames.isEmpty()) {
                    showApplistFragment(pageNames.get(0));
                } else {
                    // TODO: log: report
                    Log.d(TAG, "No pages found!");
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
                mDataModel.addNewPage(defaultPageName);
                return null;
            }
        });
    }

    private void updateApplistFragment() {
        if (mDataModel.getPageCount() == 0) {
            addDefaultPage();
        } else {
            loadApplistFragment();
        }
    }

    private void showApplistFragment(String pageName) {
        ApplistFragment fragment = ApplistFragment.newInstance(pageName, mDataModel, mIconCache);
        fragment.setListener(mFragmentListener);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, ApplistFragment.class.getName())
                .commit();
    }

    private ApplistFragment getApplistFragment() {
        return (ApplistFragment) getChildFragmentManager().findFragmentByTag(
                ApplistFragment.class.getName());
    }

}
