package net.feheren_fekete.applist.launcher;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.MainActivity;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class LauncherFragment extends Fragment {

    private static final String TAG = LauncherFragment.class.getSimpleName();

    // TODO: Inject these singletons
    private LauncherModel mLauncherModel = LauncherModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private LauncherStateManager mLauncherStateManager = LauncherStateManager.getInstance();

    private Handler mHandler = new Handler();
    private ApplistPreferences mApplistPreferences;
    private MyViewPager mPager;
    private LauncherPagerAdapter mPagerAdapter;
    private int mActivePagePosition = -1;

    public LauncherFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_fragment, container, false);

        mApplistPreferences = new ApplistPreferences(getContext().getApplicationContext());

        mPager = view.findViewById(R.id.launcher_fragment_view_pager);
        mPagerAdapter = new LauncherPagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                mActivePagePosition = position;
                setPageVisibility(mActivePagePosition, true);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), ScreenshotUtils.DELAY_SHORT);
                } else {
                    mScreenshotUtils.cancelScheduledScreenshot();
                }
            }
        });

        mActivePagePosition = mApplistPreferences.getLastActiveLauncherPagePosition();

        initPages();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // We do this check in case we missed some eventbus events while we were NOT in resumed
        // state.
        if (havePagesChangedInModel()) {
            initPages();
        }

        if (((MainActivity)getActivity()).isHomePressed()) {
            handleHomeButtonPress();
        }

        EventBus.getDefault().register(this);
        mPagerAdapter.registerDataSetObserver(mAdapterDataObserver);
        mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), ScreenshotUtils.DELAY_SHORT);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        mPagerAdapter.unregisterDataSetObserver(mAdapterDataObserver);
        mApplistPreferences.setLastActiveLauncherPagePosition(mActivePagePosition);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadedEvent(LauncherModel.DataLoadedEvent event) {
        initPages();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetMoveStartedEvent(WidgetPageFragment.WidgetMoveStartedEvent event) {
        mPager.setInterceptingTouchEvents(true, mGestureListener, mOnUpListener);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetMoveFinishedEvent(WidgetPageFragment.WidgetMoveFinishedEvent event) {
        mPager.setInterceptingTouchEvents(false, null, null);
    }

    private boolean havePagesChangedInModel() {
        List<PageData> modelPages = mLauncherModel.getPages();
        List<PageData> adapterPages = mPagerAdapter.getPages();
        if (adapterPages.size() != modelPages.size()) {
            return true;
        }
        for (PageData modelPage : modelPages) {
            boolean adapterHasPage = false;
            for (PageData adapterPage : adapterPages) {
                if (adapterPage.getId() == modelPage.getId()) {
                    adapterHasPage = true;
                    break;
                }
            }
            if (!adapterHasPage) {
                return true;
            }
        }
        return false;
    }

    private void initPages() {
        mPagerAdapter.setPages(mLauncherModel.getPages());
        mPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        if (mActivePagePosition == -1) {
            mActivePagePosition = mPagerAdapter.getMainPagePosition();
        }
        scrollToActivePage(false);
    }

    private void handleHomeButtonPress() {
        mActivePagePosition = mPagerAdapter.getMainPagePosition();
        scrollToActivePage(true);
    }

    private void scrollToActivePage(final boolean smoothScroll) {
        if (mActivePagePosition != -1) {
            mActivePagePosition = Math.min(mActivePagePosition, mPagerAdapter.getCount() - 1);
            final int currentPagePosition = mPager.getCurrentItem();
            if (currentPagePosition != mActivePagePosition) {
                // HACK: Without posting a delayed Runnable, the smooth scrolling is very laggy.
                // The delay time also matters. E.g. 10 ms is too little.
                // https://stackoverflow.com/questions/11962268/viewpager-setcurrentitempageid-true-does-not-smoothscroll
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPager.setCurrentItem(mActivePagePosition, smoothScroll);
                    }
                }, 100);
            } else {
                setPageVisibility(mActivePagePosition, true);
            }
        }
    }

    private void setPageVisibility(int pagePosition, boolean visible) {
        if (visible) {
            for (int i = 0; i < mPagerAdapter.getCount(); ++i) {
                PageData pageData = mPagerAdapter.getPageData(i);
                mLauncherStateManager.setPageVisibile(pageData.getId(), false);
            }
            PageData pageData = mPagerAdapter.getPageData(pagePosition);
            mLauncherStateManager.setPageVisibile(pageData.getId(), true);
        } else {
            PageData pageData = mPagerAdapter.getPageData(pagePosition);
            mLauncherStateManager.setPageVisibile(pageData.getId(), false);
        }
    }

    private DataSetObserver mAdapterDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            scheduleScreenshot();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            scheduleScreenshot();
        }

        private void scheduleScreenshot() {
            mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), ScreenshotUtils.DELAY_SHORT);
        }
    };

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            WidgetPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                pageFragment.handleDown(e);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean handled = false;
            WidgetPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                handled = pageFragment.handleScroll(e1, e2, distanceX, distanceY);
            }
            if (!handled) {
                handled = super.onScroll(e1, e2, distanceX, distanceY);
            }
            return handled;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            boolean handled = false;
            WidgetPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                handled = pageFragment.handleSingleTap(e);
            }
            if (!handled) {
                handled = super.onSingleTapUp(e);
            }
            return handled;
        }
    };

    private MyViewPager.OnUpListener mOnUpListener = new MyViewPager.OnUpListener() {
        @Override
        public void onUp(MotionEvent event) {
            WidgetPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                pageFragment.handleUp(event);
            }
        }
    };

    @Nullable
    private WidgetPageFragment getLauncherPageFragment() {
        final int pagePosition = mPager.getCurrentItem();
        Fragment pageFragment = mPagerAdapter.getPageFragment(pagePosition);
        if (pageFragment instanceof WidgetPageFragment) {
            return (WidgetPageFragment) pageFragment;
        }
        return null;
    }


}
