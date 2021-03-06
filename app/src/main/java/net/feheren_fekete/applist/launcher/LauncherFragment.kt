package net.feheren_fekete.applist.launcher

import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.launcher_fragment.*
import kotlinx.android.synthetic.main.launcher_fragment.view.*
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.MainActivity
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.get

class LauncherFragment : Fragment() {

    private val screenshotUtils = get<ScreenshotUtils>()
    private val launcherStateManager = get<LauncherStateManager>()
    private val applistPreferences = get<ApplistPreferences>()

    private val handler = Handler()
    private lateinit var viewModel: LauncherViewModel
    private lateinit var pagerAdapter: LauncherPagerAdapter
    private var activePagePosition = -1

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            val pageFragment = launcherPageFragment
            pageFragment?.handleDown(e)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            var handled = false
            val pageFragment = launcherPageFragment
            if (pageFragment != null) {
                handled = pageFragment.handleScroll(e1, e2, distanceX, distanceY)
            }
            if (!handled) {
                handled = super.onScroll(e1, e2, distanceX, distanceY)
            }
            return handled
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            var handled = false
            val pageFragment = launcherPageFragment
            if (pageFragment != null) {
                handled = pageFragment.handleSingleTap(e)
            }
            if (!handled) {
                handled = super.onSingleTapUp(e)
            }
            return handled
        }
    }

    private val onUpListener = MyViewPager.OnUpListener { event ->
        val pageFragment = launcherPageFragment
        pageFragment?.handleUp(event)
    }

    private val launcherPageFragment: WidgetPageFragment?
        get() {
            val pagePosition = viewPager.currentItem
            val pageFragment = pagerAdapter.getPageFragment(pagePosition)
            return if (pageFragment is WidgetPageFragment) {
                pageFragment
            } else null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pagerAdapter = LauncherPagerAdapter(childFragmentManager)
        viewModel = ViewModelProvider(this).get(LauncherViewModel::class.java)
        viewModel.launcherPages.observe(this, Observer {
            initPages(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.launcher_fragment, container, false)

        view.viewPager.adapter = pagerAdapter
        view.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                activePagePosition = position
                setPageVisibility(activePagePosition, true)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    screenshotUtils.scheduleScreenshot(activity, pagerAdapter.getPageData(activePagePosition).id, ScreenshotUtils.DELAY_SHORT)
                } else {
                    screenshotUtils.cancelScheduledScreenshot()
                }
            }
        })

        activePagePosition = applistPreferences.lastActiveLauncherPagePosition

        return view
    }

    override fun onResume() {
        super.onResume()

        if ((activity as MainActivity).isHomePressed()) {
            handleHomeButtonPress()
        }

        val pageId = requireArguments().getLong(FRAGMENT_ARG_SCROLL_TO_PAGE_ID, -1)
        if (pageId != -1L) {
            requireArguments().remove(FRAGMENT_ARG_SCROLL_TO_PAGE_ID)
            scrollToRequestedPage(pageId)
        }

        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
        applistPreferences.lastActiveLauncherPagePosition = activePagePosition
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWidgetMoveStartedEvent(event: WidgetPageFragment.WidgetMoveStartedEvent) {
        viewPager.setInterceptingTouchEvents(true, gestureListener, onUpListener)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWidgetMoveFinishedEvent(event: WidgetPageFragment.WidgetMoveFinishedEvent) {
        viewPager.setInterceptingTouchEvents(false, null, null)
    }

    private fun initPages(pages: List<LauncherPageData>) {
        if (pages.isEmpty()) {
            return
        }
        pagerAdapter.pages = pages
        viewPager.offscreenPageLimit = pagerAdapter.count - 1
        if (activePagePosition == -1) {
            activePagePosition = pagerAdapter.mainPagePosition
        }
        if (activePagePosition == -1 && pagerAdapter.count > 0) {
            ApplistLog.getInstance().log(IllegalStateException("Missing main page"))
            activePagePosition = 0
        }
        scrollToActivePage(false)
        screenshotUtils.scheduleScreenshot(activity, pagerAdapter.getPageData(activePagePosition).id, ScreenshotUtils.DELAY_SHORT)
    }

    private fun handleHomeButtonPress() {
        activePagePosition = pagerAdapter.mainPagePosition
        scrollToActivePage(true)
    }

    private fun scrollToActivePage(smoothScroll: Boolean) {
        if (activePagePosition != -1) {
            activePagePosition = Math.min(activePagePosition, pagerAdapter.count - 1)
            val currentPagePosition = viewPager.currentItem
            if (currentPagePosition != activePagePosition) {
                if (smoothScroll) {
                    // HACK: Without posting a delayed Runnable, the smooth scrolling is very laggy.
                    // The delay time also matters. E.g. 10 ms is too little.
                    // https://stackoverflow.com/questions/11962268/viewpager-setcurrentitempageid-true-does-not-smoothscroll
                    //
                    // Keep a reference to the ViewPager to avoid NPE inside the runnable (the viewPager
                    // sometimes can become null for some reason).
                    //
                    val vp = viewPager
                    handler.postDelayed({
                        vp.setCurrentItem(activePagePosition, true)
                    }, 100)
                } else {
                    viewPager.setCurrentItem(activePagePosition, false)
                }
            } else {
                setPageVisibility(activePagePosition, true)
            }
        }
    }

    private fun scrollToRequestedPage(pageId: Long) {
        // Wait for the screenshot of the current page.
        // Scroll to the requested page only after that.
        handler.postDelayed({
            val pagePosition = pagerAdapter.getPagePosition(pageId)
            if (pagePosition != -1) {
                activePagePosition = pagePosition
                scrollToActivePage(true)
            }
        }, (ScreenshotUtils.DELAY_SHORT + 500).toLong())
    }

    private fun setPageVisibility(pagePosition: Int, visible: Boolean) {
        if (visible) {
            for (i in 0 until pagerAdapter.count) {
                val pageData = pagerAdapter.getPageData(i)
                launcherStateManager.setPageVisibile(pageData.id, false)
            }
            val pageData = pagerAdapter.getPageData(pagePosition)
            launcherStateManager.setPageVisibile(pageData.id, true)
        } else {
            val pageData = pagerAdapter.getPageData(pagePosition)
            launcherStateManager.setPageVisibile(pageData.id, false)
        }
    }

    companion object {

        private val TAG = LauncherFragment::class.java.simpleName

        private val FRAGMENT_ARG_SCROLL_TO_PAGE_ID = LauncherFragment::class.java.simpleName + ".FRAGMENT_ARG_SCROLL_TO_PAGE_ID"

        fun newInstance(scrollToPageId: Long): LauncherFragment {
            val fragment = LauncherFragment()
            val args = Bundle()
            args.putLong(FRAGMENT_ARG_SCROLL_TO_PAGE_ID, scrollToPageId)
            fragment.arguments = args
            return fragment
        }
    }


}
