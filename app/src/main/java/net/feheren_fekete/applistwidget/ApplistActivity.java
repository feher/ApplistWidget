package net.feheren_fekete.applistwidget;

import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import net.feheren_fekete.applistwidget.model.DataModel;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import de.greenrobot.event.EventBus;

public class ApplistActivity
        extends AppCompatActivity
        implements SearchView.OnQueryTextListener {

    private DataModel mDataModel;
    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private ApplistPagerAdapter2 mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applists_activity);

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
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_create_section:
                createSection();
                break;
            case R.id.action_remove_items:
                break;
            case R.id.action_create_page:
                createPage();
                break;
            case R.id.action_remove_page:
                removePage();
                break;
            case R.id.action_test_reset: {
                mDataModel.removeAllPages();
                mDataModel.storeData();
                finish();
                break;
            }
            case R.id.action_test_save: {
                //((ApplistApp)getApplication()).getDataModel().storeData("/sdcard/Download/applist.saved.json");
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ApplistFragment fragment = (ApplistFragment) mPagerAdapter.getCurrentPageFragment();
        if (fragment != null) {
            fragment.setFilter(newText);
        }
        return true;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.DataLoadedEvent event) {
        updatePageFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.PagesChangedEvent event) {
        updatePageFragments();
    }

    private void createSection() {
        ApplistDialogs.textInputDialog(
                this, R.string.section_name, "",
                new ApplistDialogs.RunnableWithArg<String>() {
                    @Override
                    public void run(final String sectionName) {
                        ApplistFragment fragment = (ApplistFragment) mPagerAdapter.getCurrentPageFragment();
                        if (fragment != null && !sectionName.isEmpty()) {
                            final String pageName = fragment.getPageName();
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    mDataModel.addNewSection(pageName, sectionName, true);
                                    mDataModel.storePages();
                                    return null;
                                }
                            });
                        }
                    }
                });
    }

    private void createPage() {
        ApplistDialogs.textInputDialog(
                this, R.string.page_name, "",
                new ApplistDialogs.RunnableWithArg<String>() {
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

    private void removePage() {
        ApplistDialogs.questionDialog(this, R.string.remove_page, new Runnable() {
            @Override
            public void run() {
                ApplistFragment fragment = (ApplistFragment) mPagerAdapter.getCurrentPageFragment();
                if (fragment != null) {
                    final String pageName = fragment.getPageName();
                    Task.callInBackground(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            mDataModel.removePage(pageName);
                            return null;
                        }
                    });
                }
            }
        });
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
                hack();
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
    }

}
