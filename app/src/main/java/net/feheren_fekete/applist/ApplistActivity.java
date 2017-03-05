package net.feheren_fekete.applist;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class ApplistActivity extends AppCompatActivity {

    private static final String TAG = ApplistActivity.class.getSimpleName();

    public static final String ACTION_RESTART =
            ApplistActivity.class.getCanonicalName()+ "ACTION_RESTART";

    private Handler mHandler;
    private DataModel mDataModel;
    private IconCache mIconCache;
    private BadgeStore mBadgeStore;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private Menu mMenu;
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SettingsUtils.applyColorTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.applists_activity);

        mHandler = new Handler();
        mDataModel = DataModel.getInstance();
        mIconCache = new IconCache();
        mBadgeStore = new BadgeStore(this, getPackageManager(), new BadgeUtils(this));

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setOnClickListener(mToolbarClickListener);
        mToolbar.setTitle(R.string.toolbar_title);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        registerReceiver(mPackageStateReceiver, intentFilter);

        loadData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_RESTART.equals(intent.getAction())) {
            finish();
            startActivity(intent);
        } else {
            loadData();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SettingsUtils.getShowBadge(this)) {
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
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        mPackageStateReceiver.onReceive(this, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSearchView != null) {
            hideKeyboardFrom(this, mSearchView);
            mSearchView.setIconified(true);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPackageStateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.applist_menu, menu);

        mMenu = menu;

        MenuItem searchItem = menu.findItem(R.id.action_search_app);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextFocusChangeListener(mSearchFocusListener);
        mSearchView.setOnQueryTextListener(mSearchTextListener);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isChangingOrder = false;
        boolean isFilteredByName = false;
        ApplistFragment fragment = getApplistFragment();
        if (fragment != null) {
            isChangingOrder = fragment.isChangingOrder();
            isFilteredByName = fragment.isFilteredByName();
        }
        if (isChangingOrder) {
            menu.findItem(R.id.action_search_app).setVisible(false);
            menu.findItem(R.id.action_create_section).setVisible(false);
            menu.findItem(R.id.action_done).setVisible(true);
            menu.findItem(R.id.action_settings).setVisible(false);
        } else if (isFilteredByName) {
            menu.findItem(R.id.action_search_app).setVisible(true);
            menu.findItem(R.id.action_create_section).setVisible(false);
            menu.findItem(R.id.action_done).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(false);
        } else {
            menu.findItem(R.id.action_search_app).setVisible(true);
            menu.findItem(R.id.action_create_section).setVisible(true);
            menu.findItem(R.id.action_done).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
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

    private void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
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
                                FileUtils.getIconCacheDirPath(ApplistActivity.this),
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
                    ApplistActivity.this,
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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, ApplistFragment.class.getName())
                .commit();
    }

    private ApplistFragment getApplistFragment() {
        return (ApplistFragment) getSupportFragmentManager().findFragmentByTag(
                ApplistFragment.class.getName());
    }

}
