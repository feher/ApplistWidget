package net.feheren_fekete.applist.launcher

import android.database.DataSetObserver
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.launcher_fragment.*
import kotlinx.android.synthetic.main.launcher_fragment.view.*
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.MainActivity
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.launcher.model.LauncherModel
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject

class LauncherFragment : Fragment() {

    private val launcherModel: LauncherModel by inject()
    private val screenshotUtils: ScreenshotUtils by inject()
    private val launcherStateManager: LauncherStateManager by inject()
    private val applistPreferences: ApplistPreferences by inject()

    private val handler = Handler()
    private lateinit var pagerAdapter: LauncherPagerAdapter
    private var activePagePosition = -1

    private val adapterDataObserver = object : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            screenshotUtils.scheduleScreenshot(activity, pagerAdapter.getPageData(activePagePosition).id, ScreenshotUtils.DELAY_SHORT)
        }

        override fun onInvalidated() {
            super.onInvalidated()
            screenshotUtils.scheduleScreenshot(activity, pagerAdapter.getPageData(activePagePosition).id, ScreenshotUtils.DELAY_SHORT)
        }
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPages()
    }

    override fun onResume() {
        super.onResume()

        // We do this check in case we missed some eventbus events while we were NOT in resumed
        // state.
        if (havePagesChangedInModel()) {
            initPages()
        }

        if ((activity as MainActivity).isHomePressed()) {
            handleHomeButtonPress()
        }

        val pageId = arguments!!.getLong(FRAGMENT_ARG_SCROLL_TO_PAGE_ID, -1)
        if (pageId != -1L) {
            arguments!!.remove(FRAGMENT_ARG_SCROLL_TO_PAGE_ID)
            scrollToRequestedPage(pageId)
        }

        EventBus.getDefault().register(this)
        pagerAdapter.registerDataSetObserver(adapterDataObserver)
        screenshotUtils.scheduleScreenshot(activity, pagerAdapter.getPageData(activePagePosition).id, ScreenshotUtils.DELAY_SHORT)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
        pagerAdapter.unregisterDataSetObserver(adapterDataObserver)
        applistPreferences.lastActiveLauncherPagePosition = activePagePosition
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDataLoadedEvent(event: LauncherModel.DataLoadedEvent) {
        initPages()
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

    private fun havePagesChangedInModel(): Boolean {
        val modelPages = launcherModel.pages
        val adapterPages = pagerAdapter.pages
        if (adapterPages.size != modelPages.size) {
            return true
        }
        for (modelPage in modelPages) {
            var adapterHasPage = false
            for (adapterPage in adapterPages) {
                if (adapterPage.id == modelPage.id) {
                    adapterHasPage = true
                    break
                }
            }
            if (!adapterHasPage) {
                return true
            }
        }
        return false
    }

    private fun initPages() {
        pagerAdapter.pages = launcherModel.pages
        viewPager.offscreenPageLimit = pagerAdapter.count - 1
        if (activePagePosition == -1) {
            activePagePosition = pagerAdapter.mainPagePosition
        }
        if (activePagePosition == -1 && pagerAdapter.count > 0) {
            ApplistLog.getInstance().log(IllegalStateException("Missing main page"))
            activePagePosition = 0
        }
        scrollToActivePage(false)
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
                    handler.postDelayed({
                        viewPager.setCurrentItem(activePagePosition, true)
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
