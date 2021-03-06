package net.feheren_fekete.applist.applistpage

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.applist_page_fragment.view.*
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.viewmodel.ApplistViewModel
import net.feheren_fekete.applist.applistpage.viewmodel.PageItem
import net.feheren_fekete.applist.iap.DonutActivity
import net.feheren_fekete.applist.iap.IapRepository
import net.feheren_fekete.applist.launcher.LauncherUtils
import net.feheren_fekete.applist.launcher.pageeditor.PageEditorActivity
import net.feheren_fekete.applist.settings.SettingsActivity
import net.feheren_fekete.applist.settings.SettingsUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.get
import java.util.*

class ApplistPageFragment : Fragment() {

    private val settingsUtils = get<SettingsUtils>()
    private val launcherUtils = get<LauncherUtils>()
    private val applistPreferences = get<ApplistPreferences>()
    private val iapRepository = get<IapRepository>()

    private lateinit var viewModel: ApplistViewModel
    private lateinit var toolbarGradient: Drawable
    private var menu: Menu? = null
    private var searchView: SearchView? = null

    private val packageStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            // When a package is being replaced, the Android system actually
            // broadcasts three intents (in this order):
            //
            // ACTION_PACKAGE_REMOVED
            // ACTION_PACKAGE_ADDED
            // ACTION_PACKAGE_REPLACED
            //
            // To tell that that an ACTION_PACKAGE_REMOVED or ACTION_PACKAGE_ADDED intent is
            // received as the result of a package being replaced (instead of plain add
            // or remove), check the EXTRA_REPLACING flag.
            //
            val action = intent?.action ?: ""
            if (action == Intent.ACTION_PACKAGE_ADDED
                    || action == Intent.ACTION_PACKAGE_REMOVED) {
                val isReplacing = intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) ?: false
                if (isReplacing) {
                    return
                }
            }
            viewModel.updateData(context)
        }
    }

    private val launcherPageId: Long
        get() = requireArguments().getLong("launcherPageId")

    private val applistPagePageFragment: ApplistPagePageFragment?
        get() = childFragmentManager.findFragmentByTag(
                ApplistPagePageFragment::class.java.name) as ApplistPagePageFragment?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ApplistViewModel::class.java)
        toolbarGradient = createToolbarGradient()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.applist_page_fragment, container, false)

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        //view.toolbar.setPadding(0, screenUtils.getStatusBarHeight(context), 0, 0)

        view.toolbar.setOnClickListener {
            ApplistLog.getInstance().analytics(ApplistLog.START_APP_SEARCH, ApplistLog.TOOLBAR)
            searchView?.isIconified = false
        }

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(view.toolbar)
        activity.supportActionBar!!.setDisplayShowTitleEnabled(false)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        setHasOptionsMenu(true)

        val packageIntentFilter = IntentFilter()
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        packageIntentFilter.addDataScheme("package")
        requireContext().registerReceiver(packageStateReceiver, packageIntentFilter)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showApplistFragment(PageItem(0, "unused"))
    }

    override fun onStart() {
        super.onStart()
        view?.toolbar?.background = toolbarGradient
        if (settingsUtils.showNewContentBadge
                || settingsUtils.showNotificationBadge) {
            viewModel.updateBadgesFromLauncher()
        }
    }

    override fun onResume() {
        super.onResume()

        EventBus.getDefault().register(this)

        iapRepository.queryAndHandlePurchases()

        context?.let {
            packageStateReceiver.onReceive(it, null)
        }

        context?.let {
            val currentDeviceLocale = Locale.getDefault().toString()
            val savedDeviceLocale = applistPreferences.deviceLocale
            if (currentDeviceLocale != savedDeviceLocale) {
                applistPreferences.deviceLocale = currentDeviceLocale
                viewModel.updateData(it)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        searchView?.let {
            hideKeyboardFrom(requireContext(), it)
            it.isIconified = true
        }
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(packageStateReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.applist_menu, menu)

        this.menu = menu

        val searchItem = menu.findItem(R.id.action_search_app)
        searchView = searchItem.actionView as SearchView
        searchView!!.setIconifiedByDefault(true)

        searchView!!.setOnQueryTextFocusChangeListener { _, hasFocus ->
            val fragment = applistPagePageFragment
            if (fragment != null) {
                if (hasFocus) {
                    startFilteringByName(fragment)
                } else {
                    stopFilteringByName(fragment)
                }
            }
        }

        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val fragment = applistPagePageFragment
                if (fragment != null) {
                    if (newText == null || newText.isEmpty()) {
                        fragment.setNameFilter("")
                    } else {
                        fragment.setNameFilter(newText)
                    }
                }
                return true
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val isFilteredByName = applistPagePageFragment?.isFilteredByName() ?: false
        if (isFilteredByName) {
            menu.findItem(R.id.action_search_app)?.isVisible = true
            menu.findItem(R.id.action_create_section)?.isVisible = false
            menu.findItem(R.id.action_settings)?.isVisible = false
            menu.findItem(R.id.action_edit_pages)?.isVisible = false
            menu.findItem(R.id.action_change_wallpaper)?.isVisible = false
            menu.findItem(R.id.action_donut)?.isVisible = false
        } else {
            menu.findItem(R.id.action_search_app)?.isVisible = true
            menu.findItem(R.id.action_create_section)?.isVisible = true
            menu.findItem(R.id.action_settings)?.isVisible = true
            menu.findItem(R.id.action_edit_pages)?.isVisible = true
            menu.findItem(R.id.action_change_wallpaper)?.isVisible = true
            menu.findItem(R.id.action_donut)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        var isHandled = applistPagePageFragment?.handleMenuItem(id) ?: false

        if (!isHandled) {
            when (id) {
                R.id.action_settings -> {
                    ApplistLog.getInstance().analytics(ApplistLog.SHOW_SETTINGS, ApplistLog.OPTIONS_MENU)
                    showSettings()
                    isHandled = true
                }
                R.id.action_edit_pages -> {
                    ApplistLog.getInstance().analytics(ApplistLog.SHOW_PAGE_EDITOR, ApplistLog.OPTIONS_MENU)
                    showPageEditor()
                    isHandled = true
                }
                R.id.action_change_wallpaper -> {
                    ApplistLog.getInstance().analytics(ApplistLog.CHANGE_WALLPAPER, ApplistLog.OPTIONS_MENU)
                    launcherUtils.changeWallpaper(activity)
                    isHandled = true
                }
                R.id.action_search_app -> {
                    ApplistLog.getInstance().analytics(ApplistLog.START_APP_SEARCH, ApplistLog.OPTIONS_MENU)
                    // Let the system handle it. It will open the SearchView.
                    isHandled = false
                }
                R.id.action_donut -> {
                    ApplistLog.getInstance().analytics(ApplistLog.SHOW_DONUTS, ApplistLog.OPTIONS_MENU)
                    showDonuts()
                    isHandled = true
                }
            }
        }

        if (!isHandled) {
            isHandled = super.onOptionsItemSelected(item)
        }
        return isHandled
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShowToolbarEvent(event: ApplistPagePageFragment.ShowToolbarEvent) {
        showToolbar()
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHideToolbarEvent(event: ApplistPagePageFragment.HideToolbarEvent) {
        hideToolbar()
    }

    private fun createToolbarGradient(): Drawable {
        // REF: 2017_06_30_toolbar_gradient
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.toolbarBackgroundColor, typedValue, true)
        var startColor = typedValue.data
        val endColor: Int
        if (settingsUtils.isThemeTransparent) {
            endColor = startColor and 0xffffff or 0x55000000
        } else {
            startColor = startColor and 0xffffff or -0x78000000
            endColor = startColor
        }
        val drawable = GradientDrawable()
        drawable.orientation = GradientDrawable.Orientation.TOP_BOTTOM
        drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        drawable.colors = intArrayOf(startColor, endColor)
        drawable.setCornerRadii(arrayOf(
                0.0f, 0.0f, // top-left
                0.0f, 0.0f, // top-right
                0.0f, 0.0f, // bottom-right
                50.0f, 50.0f  // bottom-left
        ).toFloatArray())
        return drawable
    }

    private fun showPageEditor() {
        val intent = Intent(context, PageEditorActivity::class.java)
        startActivity(intent)
    }

    private fun showSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showDonuts() {
        val intent = Intent(context, DonutActivity::class.java)
        startActivity(intent)
    }

    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun startFilteringByName(fragment: ApplistPagePageFragment) {
        fragment.activateNameFilter()
        onPrepareOptionsMenu(menu!!)
    }

    private fun stopFilteringByName(fragment: ApplistPagePageFragment) {
        fragment.deactivateNameFilter()
        searchView!!.isIconified = true
        onPrepareOptionsMenu(menu!!)
    }

    private fun showApplistFragment(pageItem: PageItem) {
        if (!isAdded || isStateSaved) {
            return
        }
        val fragment = ApplistPagePageFragment.newInstance(
                pageItem, launcherPageId)
        childFragmentManager
                .beginTransaction()
                .replace(R.id.applist_page_fragment_page_container, fragment, ApplistPagePageFragment::class.java.name)
                .commit()
    }

    private fun showToolbar() {
        view?.toolbar?.visibility = View.VISIBLE
    }

    private fun hideToolbar() {
        view?.toolbar?.visibility = View.GONE
    }

    companion object {

        private val TAG = ApplistPageFragment::class.java.simpleName

        fun newInstance(launcherPageId: Long): ApplistPageFragment {
            val fragment = ApplistPageFragment()

            val args = Bundle()
            args.putLong("launcherPageId", launcherPageId)
            fragment.arguments = args

            return fragment
        }
    }

}
