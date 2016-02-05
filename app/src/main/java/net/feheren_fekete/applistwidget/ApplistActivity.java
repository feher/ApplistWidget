package net.feheren_fekete.applistwidget;

import android.content.DialogInterface;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import net.feheren_fekete.applistwidget.model.DataModel;
import net.feheren_fekete.applistwidget.utils.RunnableWithArg;
import net.feheren_fekete.applistwidget.viewmodel.SectionItem;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import de.greenrobot.event.EventBus;

public class ApplistActivity extends AppCompatActivity {

    private static final String TAG = ApplistActivity.class.getSimpleName();

    private Handler mHandler;
    private DataModel mDataModel;
    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private ApplistPagerAdapter2 mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applists_activity);

        mHandler = new Handler();
        mDataModel = ((ApplistApp)getApplication()).getDataModel();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mPagerAdapter = new ApplistPagerAdapter2(getSupportFragmentManager(), mDataModel);
        mViewPager.setAdapter(mPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        // WORKAROUND: We must post setupWithViewPager() due to a bug:
        // https://code.google.com/p/android/issues/detail?id=180462
        mTabLayout.post(new Runnable() {
            @Override
            public void run() {
                mTabLayout.setupWithViewPager(mViewPager);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDataModel.storeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.applist_menu, menu);

        MenuItem item = menu.findItem(R.id.action_search_app);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnSearchClickListener(mSearchStartListener);
        searchView.setOnQueryTextListener(mSearchTextListener);
        searchView.setOnCloseListener(mSearchCloseListener);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isChangingOrder = false;
        ApplistFragment fragment = mPagerAdapter.getCurrentPageFragment();
        if (fragment != null) {
            isChangingOrder = fragment.isChangingOrder();
        }
        if (isChangingOrder) {
            menu.findItem(R.id.action_search_app).setVisible(false);
            menu.findItem(R.id.action_done).setVisible(true);
        } else {
            menu.findItem(R.id.action_search_app).setVisible(true);
            menu.findItem(R.id.action_done).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isHandled = false;
        int id = item.getItemId();

        ApplistFragment fragment = mPagerAdapter.getCurrentPageFragment();
        if (fragment != null) {
            isHandled = fragment.handleMenuItem(id);
        }

        if (!isHandled) {
            isHandled = super.onOptionsItemSelected(item);
        }
        return isHandled;
    }

    private SearchView.OnQueryTextListener mSearchTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            ApplistFragment fragment = mPagerAdapter.getCurrentPageFragment();
            if (fragment != null) {
                fragment.setFilter(newText);
            }
            return true;
        }
    };

    private SearchView.OnCloseListener mSearchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            ApplistFragment fragment = mPagerAdapter.getCurrentPageFragment();
            if (fragment != null) {
                fragment.setFilter(null);
            }
            return false;
        }
    };

    private View.OnClickListener mSearchStartListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ApplistFragment fragment = mPagerAdapter.getCurrentPageFragment();
            if (fragment != null) {
                fragment.setFilter("");
            }
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.DataLoadedEvent event) {
        updatePageFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.PagesChangedEvent event) {
        updatePageFragments();
    }

    private void loadPageFragments() {
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return mDataModel.getPageNames();
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                mPagerAdapter.clearPageNames();
                List<String> pageNames = task.getResult();
                mPagerAdapter.setPageNames(pageNames);
                mPagerAdapter.notifyDataSetChanged();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPagerAdapter.forEachPageFragment(new RunnableWithArg<ApplistFragment>() {
                            @Override
                            public void run(ApplistFragment fragment) {
                                Log.d(TAG, "ZIZI UPDATE " + fragment.getPageName());
                                fragment.update();
                            }
                        });
                    }
                }, 1000);
                hack();
                mTabLayout.setVisibility((pageNames.size() == 1) ? View.GONE : View.VISIBLE);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void addDefaultPage() {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mDataModel.addNewPage(DataModel.DEFAULT_PAGE_NAME);
                return null;
            }
        });
    }

    private void updatePageFragments() {
        if (mDataModel.getPageCount() == 0) {
            addDefaultPage();
        } else {
            loadPageFragments();
        }
    }

    private void hack() {
        mTabLayout.post(new Runnable() {
            @Override
            public void run() {
                mTabLayout.setupWithViewPager(mViewPager);
            }
        });
        mTabLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                LinearLayout tabStrip = (LinearLayout) mTabLayout.getChildAt(0);
                final List<String> pageNames = mPagerAdapter.getPageNames();
                for (int i = 0; i < tabStrip.getChildCount(); i++) {
                    final int finalI = i;
                    tabStrip.getChildAt(i).setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            onPageLongTapped(pageNames.get(finalI));
                            return false;
                        }
                    });
                }
            }
        }, 2000);
    }

    private static final int PAGE_MENU_RENAME = 0;
    private static final int PAGE_MENU_DELETE = 1;
    private static final int PAGE_MENU_CREATE = 2;

    private void onPageLongTapped(final String pageName) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(pageName);
        alertDialogBuilder.setItems(R.array.page_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case PAGE_MENU_RENAME:
                        renamePage(pageName);
                        break;
                    case PAGE_MENU_DELETE:
                        deletePage(pageName);
                        break;
                    case PAGE_MENU_CREATE:
                        createPage();
                        break;
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void renamePage(final String pageName) {
        ApplistDialogs.textInputDialog(
                this, R.string.page_name, pageName,
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String newPageName) {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mDataModel.setPageName(pageName, newPageName);
                                return null;
                            }
                        });
                    }
                });
    }

    private void deletePage(final String pageName) {
        ApplistDialogs.questionDialog(
                this, R.string.remove_page,
                new Runnable() {
                    @Override
                    public void run() {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mDataModel.removePage(pageName);
                                return null;
                            }
                        });
                    }
                });
    }

    private void createPage() {
        ApplistDialogs.textInputDialog(
                this, R.string.page_name, "",
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String pageName) {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mDataModel.addNewPage(pageName);
                                return null;
                            }
                        });
                    }
                });
    }

}
