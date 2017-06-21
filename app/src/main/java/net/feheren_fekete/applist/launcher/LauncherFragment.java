package net.feheren_fekete.applist.launcher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LauncherFragment extends Fragment {

    private static final String TAG = LauncherFragment.class.getSimpleName();

    // TODO: Inject these singletons
    private LauncherModel mLauncherModel = LauncherModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();

    private MyViewPager mPager;
    private LauncherPagerAdapter mPagerAdapter;
    private int mActivePage = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_fragment, container, false);

        mPager = (MyViewPager) view.findViewById(R.id.launcher_fragment_view_pager);
        mPagerAdapter = new LauncherPagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                mActivePage = position;
                mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePage).getId(), 500);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if (savedInstanceState != null) {
            mActivePage = savedInstanceState.getInt("activePage");
        }

        initPages();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("activePage", mActivePage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);

        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        getView().setBackground(wallpaperDrawable);

        mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePage).getId(), 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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

    private void initPages() {
        mPagerAdapter.setPages(mLauncherModel.getPages());
        mPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        if (mActivePage == -1) {
            mActivePage = mPagerAdapter.getMainPagePosition();
        }
        if (mActivePage != -1) {
            mActivePage = Math.min(mActivePage, mPagerAdapter.getCount() - 1);
            mPager.setCurrentItem(mActivePage, false);
            mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePage).getId(), 1000);
        }
    }

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
