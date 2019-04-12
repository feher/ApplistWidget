package net.feheren_fekete.applist.launcher.pageeditor

import android.Manifest
import android.appwidget.AppWidgetHost
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.launcher_page_editor_fragment.*
import kotlinx.android.synthetic.main.launcher_page_editor_fragment.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.ApplistDialogs
import net.feheren_fekete.applist.launcher.LauncherStateManager
import net.feheren_fekete.applist.launcher.ScreenshotUtils
import net.feheren_fekete.applist.launcher.model.LauncherModel
import net.feheren_fekete.applist.launcher.model.PageData
import net.feheren_fekete.applist.utils.ScreenUtils
import net.feheren_fekete.applist.widgetpage.model.WidgetModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject

class PageEditorFragment : Fragment() {

    class DoneEvent
    class PageTappedEvent(val requestData: Bundle, val pageData: PageData)

    private val launcherStateManager: LauncherStateManager by inject()
    private val launcherModel: LauncherModel by inject()
    private val widgetModel: WidgetModel by inject()
    private val appWidgetHost: AppWidgetHost by inject()
    private val screenshotUtils: ScreenshotUtils by inject()
    private val screenUtils: ScreenUtils by inject()

    private lateinit var adapter: PageEditorAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var dragScrollThreshold: Int = 0
    private var maxDragScroll: Int = 0

    private lateinit var requestData: Bundle

    private val pageEditorAdapterListener = object : PageEditorAdapter.Listener {
        override fun onHomeTapped(position: Int) {
            setMainPage(position)
        }

        override fun onRemoveTapped(position: Int) {
            removePage(position)
        }

        override fun onPageMoverTouched(position: Int, viewHolder: RecyclerView.ViewHolder) {
            itemTouchHelper.startDrag(viewHolder)
        }

        override fun onPageTapped(position: Int, viewHolder: RecyclerView.ViewHolder) {
            handlePageTapped(position)
        }
    }

    private inner class SimpleItemTouchHelperCallback : ItemTouchHelper.Callback() {

        private var itemAction: Int = 0

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Nothing.
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = 0
            return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
        }

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            val result = adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            GlobalScope.launch {
                launcherModel.pages = adapter.items
            }
            return result
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (viewHolder != null) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    (viewHolder as PageEditorAdapter.PageViewHolder).layout.animate()
                            .scaleX(0.9f).scaleY(0.9f).setDuration(150).start()
                    itemAction = actionState
                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (itemAction == ItemTouchHelper.ACTION_STATE_DRAG) {
                (viewHolder as PageEditorAdapter.PageViewHolder).layout.animate()
                        .scaleX(1.0f).scaleY(1.0f).setDuration(150).start()

                // This is needed to re-draw (re-bind) all the items in the RecyclerView.
                // We want to update the page numbers of every item.
                adapter.notifyDataSetChanged()
            }
        }

        override fun interpolateOutOfBoundsScroll(recyclerView: RecyclerView, viewSize: Int, viewSizeOutOfBounds: Int, totalSize: Int, msSinceStartScroll: Long): Int {
            var scrollX = 0
            val absOutOfBounds = Math.abs(viewSizeOutOfBounds)
            if (absOutOfBounds > dragScrollThreshold) {
                val direction = Math.signum(viewSizeOutOfBounds.toFloat()).toInt()
                scrollX = direction * maxDragScroll
                if (scrollX == 0) {
                    scrollX = if (viewSizeOutOfBounds > 0) 1 else -1
                }
            }
            return scrollX
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val useAsPagePicker = arguments!!.getBoolean(FRAGMENT_ARG_USE_AS_PAGE_PICKER)
        val pagePreviewSizeMultiplier = if (useAsPagePicker) 0.5f else 0.7f
        adapter = PageEditorAdapter(pagePreviewSizeMultiplier, pageEditorAdapterListener)
        adapter.showMainPageIndicator(!useAsPagePicker)
        adapter.showMovePageIndicator(!useAsPagePicker)

        requestData = arguments!!.getBundle(FRAGMENT_ARG_REQUEST_DATA)!!
        itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.launcher_page_editor_fragment, container, false)

        if (arguments!!.getBoolean(FRAGMENT_ARG_ADD_PADDING)) {
            // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
            val topPadding = screenUtils.getStatusBarHeight(context)
            // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
            val bottomPadding = if (screenUtils.hasNavigationBar(context)) screenUtils.getNavigationBarHeight(context) else 0
            view.layout.setPadding(0, topPadding, 0, bottomPadding)
        }

        view.recyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        view.recyclerView.adapter = adapter

        maxDragScroll = Math.round(screenUtils.dpToPx(context, 3f))
        dragScrollThreshold = Math.round(screenUtils.dpToPx(context, 100f))

        val useAsPagePicker = arguments!!.getBoolean(FRAGMENT_ARG_USE_AS_PAGE_PICKER)
        val pagePreviewSizeMultiplier = if (useAsPagePicker) 0.5f else 0.7f
        if (!useAsPagePicker) {
            val helper = PagerSnapHelper()
            helper.attachToRecyclerView(view.recyclerView)
            val screenSize = screenUtils.getScreenSize(context)
            val padding = Math.round(screenSize.x * (pagePreviewSizeMultiplier / 2) / 2)
            view.recyclerView.setPadding(padding, 0, padding, 0)
        }

        view.addPageButton.setOnClickListener { addNewPage() }

        view.doneButton.setOnClickListener { doneWithEditing() }
        if (useAsPagePicker) {
            view.doneButton.setText(R.string.launcher_page_editor_cancel)
            view.doneButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_close, 0, 0)
        } else {
            view.doneButton.setText(R.string.launcher_page_editor_done)
            view.doneButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_done, 0, 0)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemTouchHelper.attachToRecyclerView(view.recyclerView)
        ensureReadWallpaperPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        itemTouchHelper.attachToRecyclerView(null)
    }

    override fun onResume() {
        super.onResume()
        screenshotUtils.cancelScheduledScreenshot()
        EventBus.getDefault().register(this)
        adapter.setPages(launcherModel.pages)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_WALLPAPER) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Update the adapter to show the current wallpaper in the items' backgrounds.
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDataLoadedEvent(event: LauncherModel.DataLoadedEvent) {
        adapter.setPages(launcherModel.pages)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPagesChangedEvent(event: LauncherModel.PagesChangedEvent) {
        adapter.setPages(launcherModel.pages)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageAddedEvent(event: LauncherModel.PageAddedEvent) {
        adapter.addPage(event.pageData)
        recyclerView.smoothScrollToPosition(adapter.getItemPosition(event.pageData))
    }

    private fun ensureReadWallpaperPermission() {
        val a = activity ?: return
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ApplistDialogs.messageDialog(
                        a,
                        a.getString(R.string.launcher_page_editor_permission_title),
                        a.getString(R.string.launcher_page_editor_permission_message),
                        {
                            ActivityCompat.requestPermissions(
                                    a,
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                    PERMISSIONS_REQUEST_READ_WALLPAPER)
                        },
                        {
                            // Nothing
                        })
            } else {
                ActivityCompat.requestPermissions(
                        a,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_READ_WALLPAPER)
            }
        }
    }

    private fun addNewPage() {
        GlobalScope.launch {
            launcherModel.addPage(PageData(System.currentTimeMillis(), PageData.TYPE_WIDGET_PAGE, false))
        }
    }

    private fun setMainPage(position: Int) {
        GlobalScope.launch {
            launcherModel.setMainPage(position)
        }
    }

    private fun removePage(position: Int) {
        val pageData = adapter.getItem(position)
        val pageId = pageData.id
        val screenshotPath = screenshotUtils.createScreenshotPath(context, pageId)
        val alertDialog = AlertDialog.Builder(context!!)
                .setTitle(R.string.launcher_page_editor_remove_dialog_title)
                .setMessage(R.string.launcher_page_editor_remove_dialog_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    GlobalScope.launch {
                        screenshotUtils.deleteScreenshot(screenshotPath)
                        val deletedWidgetIds = widgetModel.deleteWidgetsOfPage(pageId)
                        for (widgetId in deletedWidgetIds) {
                            appWidgetHost.deleteAppWidgetId(widgetId!!)
                        }
                        launcherModel.removePage(position)
                        launcherStateManager.clearPageVisible(pageId)
                    }
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    // Nothing.
                }
                .setCancelable(true)
                .create()
        alertDialog.show()
    }

    private fun handlePageTapped(position: Int) {
        val pageData = adapter.getItem(position)
        EventBus.getDefault().post(PageTappedEvent(requestData, pageData))
    }

    private fun doneWithEditing() {
        EventBus.getDefault().post(DoneEvent())
    }

    companion object {

        private val TAG = PageEditorFragment::class.java.simpleName

        private val FRAGMENT_ARG_REQUEST_DATA = PageEditorFragment::class.java.simpleName + ".FRAGMENT_ARG_REQUEST_DATA"
        private val FRAGMENT_ARG_USE_AS_PAGE_PICKER = PageEditorFragment::class.java.simpleName + ".FRAGMENT_ARG_USE_AS_PAGE_PICKER"
        private val FRAGMENT_ARG_ADD_PADDING = PageEditorFragment::class.java.simpleName + ".FRAGMENT_ARG_ADD_PADDING"

        private val PERMISSIONS_REQUEST_READ_WALLPAPER = 1234

        fun newInstance(addPadding: Boolean, useAsPagePicker: Boolean, requestData: Bundle): PageEditorFragment {
            val fragment = PageEditorFragment()
            val args = Bundle()
            args.putBoolean(FRAGMENT_ARG_ADD_PADDING, addPadding)
            args.putBoolean(FRAGMENT_ARG_USE_AS_PAGE_PICKER, useAsPagePicker)
            args.putBundle(FRAGMENT_ARG_REQUEST_DATA, requestData)
            fragment.arguments = args
            return fragment
        }
    }

}
