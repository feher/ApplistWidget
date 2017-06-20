package net.feheren_fekete.applist.launcher;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcherpage.LauncherPageFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LauncherFragment extends Fragment {

    private static final String TAG = LauncherFragment.class.getSimpleName();

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
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if (savedInstanceState != null) {
            mPager.setCurrentItem(savedInstanceState.getInt("activePage"), false);
        } else {
            mPager.setCurrentItem(mPagerAdapter.getMainPagePosition(), false);
        }

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
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetMoveStartedEvent(LauncherPageFragment.WidgetMoveStartedEvent event) {
        mPager.setInterceptingTouchEvents(true, mGestureListener, mOnUpListener);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWidgetMoveFinishedEvent(LauncherPageFragment.WidgetMoveFinishedEvent event) {
        mPager.setInterceptingTouchEvents(false, null, null);
    }

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            LauncherPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                pageFragment.handleDown(e);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean handled = false;
            LauncherPageFragment pageFragment = getLauncherPageFragment();
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
            LauncherPageFragment pageFragment = getLauncherPageFragment();
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
            LauncherPageFragment pageFragment = getLauncherPageFragment();
            if (pageFragment != null) {
                pageFragment.handleUp(event);
            }
        }
    };

    @Nullable
    private LauncherPageFragment getLauncherPageFragment() {
        final int pagePosition = mPager.getCurrentItem();
        Fragment pageFragment = mPagerAdapter.getPageFragment(pagePosition);
        if (pageFragment instanceof LauncherPageFragment) {
            return (LauncherPageFragment) pageFragment;
        }
        return null;
    }


}
