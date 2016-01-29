package net.feheren_fekete.applistwidget;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import net.feheren_fekete.applistwidget.model.DataModel;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import de.greenrobot.event.EventBus;

public class ApplistActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private ApplistPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applists_activity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mPagerAdapter = new ApplistPagerAdapter(getSupportFragmentManager());
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
        final DataModel dataModel = ((ApplistApp)getApplication()).getDataModel();
        dataModel.storeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.applist_menu, menu);
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
                ((ApplistApp)getApplication()).getDataModel().removeAllPages();
                ((ApplistApp)getApplication()).getDataModel().storeData();
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

    private void createSection() {
        ApplistDialogs.textInputDialog(
                this, R.string.section_name, "",
                new ApplistDialogs.RunnableWithArg<String>() {
                    @Override
                    public void run(final String sectionName) {
                        int pageNumber = mViewPager.getCurrentItem();
                        int pageCount = mPagerAdapter.getCount();
                        if (0 <= pageNumber
                                && pageNumber < pageCount
                                && !sectionName.isEmpty()) {
                            ApplistFragment fragment = (ApplistFragment) mPagerAdapter.getItem(pageNumber);
                            final String pageName = fragment.getPageName();
                            final DataModel dataModel = ((ApplistApp) getApplication()).getDataModel();
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    dataModel.addNewSection(pageName, sectionName, true);
                                    dataModel.storePages();
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
                        final DataModel dataModel = ((ApplistApp) getApplication()).getDataModel();
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                dataModel.addNewPage(pageName);
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
                int pageNumber = mViewPager.getCurrentItem();
                int pageCount = mPagerAdapter.getCount();
                if (0 <= pageNumber
                        && pageNumber < pageCount) {
                    ApplistFragment fragment = (ApplistFragment) mPagerAdapter.getItem(pageNumber);
                    final String pageName = fragment.getPageName();
                    final DataModel dataModel = ((ApplistApp) getApplication()).getDataModel();
                    Task.callInBackground(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            dataModel.removePage(pageName);
                            return null;
                        }
                    });
                }
            }
        });
    }

    private void loadPageFragments(final DataModel dataModel) {
        Task.callInBackground(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return dataModel.getPageNames();
            }
        }).continueWith(new Continuation<List<String>, Void>() {
            @Override
            public Void then(Task<List<String>> task) throws Exception {
                mPagerAdapter.removeFragments();
                List<String> pageNames = task.getResult();
                for (String pageName : pageNames) {
                    ApplistFragment fragment = ApplistFragment.newInstance(pageName);
                    fragment.setDataModel(dataModel);
                    mPagerAdapter.addFragment(fragment, pageName);
                }
                mPagerAdapter.notifyDataSetChanged();
                hack();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void addDefaultPage(final DataModel dataModel) {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                dataModel.addNewPage(DataModel.DEFAULT_PAGE_NAME);
                return null;
            }
        });
    }

    private void updatePageFragments() {
        ApplistApp app = (ApplistApp) getApplication();
        DataModel dataModel = app.getDataModel();
        if (app.getDataModel().getPageCount() == 0) {
            addDefaultPage(dataModel);
        } else {
            loadPageFragments(dataModel);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.DataLoadedEvent event) {
        updatePageFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DataModel.PagesChangedEvent event) {
        updatePageFragments();
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
