package net.feheren_fekete.applist.launcher;

import android.app.WallpaperManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.MainActivity;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class LauncherFragment extends Fragment {

    private static final String TAG = LauncherFragment.class.getSimpleName();

    // TODO: Inject these singletons
    private LauncherModel mLauncherModel = LauncherModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private LauncherStateManager mLauncherStateManager = LauncherStateManager.getInstance();

    private ApplistPreferences mApplistPreferences;
    private ImageView mBackgroundImage;
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

        mBackgroundImage = (ImageView) view.findViewById(R.id.launcher_fragment_background);

        mPager = (MyViewPager) view.findViewById(R.id.launcher_fragment_view_pager);
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
                    mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), 500);
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

        EventBus.getDefault().register(this);

        Task.callInBackground(new Callable<Drawable>() {
            @Override
            public Drawable call() throws Exception {
                final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                return wallpaperDrawable;
            }
        }).continueWith(new Continuation<Drawable, Void>() {
            @Override
            public Void then(Task<Drawable> task) throws Exception {
                Drawable wallpaperDrawable = task.getResult();
                mBackgroundImage.setImageDrawable(wallpaperDrawable);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        if (((MainActivity)getActivity()).isHomePressed()) {
            handleHomeButtonPress();
        } else {
            mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), 1000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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

    private void scrollToActivePage(boolean smoothScroll) {
        if (mActivePagePosition != -1) {
            mActivePagePosition = Math.min(mActivePagePosition, mPagerAdapter.getCount() - 1);
            final int currentPagePosition = mPager.getCurrentItem();
            if (currentPagePosition != mActivePagePosition) {
                mPager.setCurrentItem(mActivePagePosition, false);
            } else {
                setPageVisibility(mActivePagePosition, true);
                mScreenshotUtils.scheduleScreenshot(getActivity(), mPagerAdapter.getPageData(mActivePagePosition).getId(), 500);
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
