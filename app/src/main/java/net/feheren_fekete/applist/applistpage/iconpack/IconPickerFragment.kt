package net.feheren_fekete.applist.applistpage.iconpack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ComponentName
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.iconpack_picker_fragment.*
import kotlinx.android.synthetic.main.iconpack_picker_fragment.view.*
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.ApplistDialogs
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon
import net.feheren_fekete.applist.utils.glide.FileSignature
import net.feheren_fekete.applist.utils.glide.GlideApp
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import java.io.File
import java.lang.StringBuilder

class IconPickerFragment : Fragment() {

    class CancelEvent
    class DoneEvent

    private val iconPackHelper: IconPackHelper by inject()
    private val applistPreferences: ApplistPreferences by inject()

    private lateinit var viewModel: IconPickerViewModel
    private lateinit var iconPacksAdapter: IconPacksAdapter
    private lateinit var iconsAdapter: IconsAdapter

    private var menu: Menu? = null
    private var searchView: SearchView? = null
    private var icons: IconPackIconsLiveData? = null

    companion object {
        private const val FRAGMENT_ARG_TITLE = "title"
        private const val FRAGMENT_ARG_APPLIST_ITEM_ID = "applistItemId"
        private const val FRAGMENT_ARG_APP_NAME = "appName"
        private const val FRAGMENT_ARG_COMPONENT_NAME = "componentName"
        private const val FRAGMENT_ARG_ICON_PATH = "iconPath"
        private const val FRAGMENT_ARG_CUSTOM_ICON_PATH = "customIconPath"

        fun newInstance(
            title: String,
            applistItemId: Long,
            appName: String,
            componentName: ComponentName?,
            iconPath: String?,
            customIconPath: String
        ): IconPickerFragment {
            val args = Bundle()
            args.putString(FRAGMENT_ARG_TITLE, title)
            args.putLong(FRAGMENT_ARG_APPLIST_ITEM_ID, applistItemId)
            args.putString(FRAGMENT_ARG_APP_NAME, appName)
            if (componentName != null) {
                args.putParcelable(FRAGMENT_ARG_COMPONENT_NAME, componentName)
            }
            if (iconPath != null) {
                args.putString(FRAGMENT_ARG_ICON_PATH, iconPath)
            }
            args.putString(FRAGMENT_ARG_CUSTOM_ICON_PATH, customIconPath)

            val fragment = IconPickerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconPacksAdapter = IconPacksAdapter { onIconPackSelected(it) }
        iconsAdapter = IconsAdapter(::onIconSelected, ::onIconLongTapped)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.iconpack_picker_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity?)?.let {
            it.setSupportActionBar(view.toolbar)
            it.supportActionBar?.title = requireArguments().getString(FRAGMENT_ARG_TITLE)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        }
        setHasOptionsMenu(true)

        view.appName.text = requireArguments().getString(FRAGMENT_ARG_APP_NAME)
        clearAppIconPreview(view)

        view.iconPacksRecyclerView.adapter = iconPacksAdapter
        view.iconPacksRecyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        view.iconsRecyclerView.adapter = iconsAdapter
        view.iconsRecyclerView.layoutManager = GridLayoutManager(context, 6)

        viewModel = ViewModelProvider(this).get(IconPickerViewModel::class.java)
        viewModel.iconPacks.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                iconpackPickerEmptyLayout.visibility = View.VISIBLE
                iconpackPickerContentLayout.visibility = View.GONE
                emptyContentText.movementMethod = LinkMovementMethod.getInstance()
            } else {
                iconpackPickerEmptyLayout.visibility = View.GONE
                iconpackPickerContentLayout.visibility = View.VISIBLE
                iconPacksAdapter.setItems(it)
                onIconPackSelected(0)
            }
        })

        view.setFab.visibility = View.GONE
        view.setFab.setOnClickListener {
            setAppIcon(iconsAdapter.selectedItem)
        }
    }

    override fun onResume() {
        super.onResume()
        if (applistPreferences.showIconPackHelp) {
            ApplistDialogs.messageDialog(
                requireActivity(),
                R.string.help_title,
                R.string.icon_pack_help,
                onOk = {
                    applistPreferences.showIconPackHelp = false
                },
                onCancel = {}
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.iconpack_picker_menu, menu)

        this.menu = menu

        val searchItem = menu.findItem(R.id.action_search_icon)
        searchView = searchItem.actionView as SearchView
        searchView!!.setIconifiedByDefault(true)

        searchView!!.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                //startFilteringByName(fragment)
            } else {
                //stopFilteringByName(fragment)
            }
        }

        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    iconsAdapter.setFilterText(null)
                } else {
                    iconsAdapter.setFilterText(newText)
                }
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                EventBus.getDefault().post(CancelEvent())
                return true
            }
            R.id.action_reset_icon -> {
                ApplistDialogs.questionDialog(
                    requireActivity(),
                    android.R.string.dialog_alert_title,
                    R.string.iconpack_picker_reset_warning,
                    onOk = {
                        viewModel.resetOriginalIcon(
                            requireArguments().getLong(FRAGMENT_ARG_APPLIST_ITEM_ID),
                            requireArguments().getString(FRAGMENT_ARG_CUSTOM_ICON_PATH)!!
                        )
                        EventBus.getDefault().post(DoneEvent())
                    },
                    onCancel = {}
                )
                return true
            }
            R.id.action_reset_all_icons -> {
                ApplistDialogs.questionDialog(
                    requireActivity(),
                    android.R.string.dialog_alert_title,
                    R.string.iconpack_picker_reset_all_warning,
                    onOk = {
                        viewModel.resetAllIcons()
                        EventBus.getDefault().post(DoneEvent())
                    },
                    onCancel = {}
                )
                return true
            }
            R.id.action_apply_iconpack -> {
                ApplistDialogs.questionDialog(
                    requireActivity(),
                    android.R.string.dialog_alert_title,
                    R.string.iconpack_picker_apply_all_warning,
                    onOk = {
                        val iconPackPackageName = iconsAdapter.iconPackPackageName
                        applistPreferences.iconPackPackageName = iconPackPackageName
                        viewModel.applyIconPack(iconPackPackageName)
                        EventBus.getDefault().post(DoneEvent())
                    },
                    onCancel = {}
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onIconPackSelected(position: Int) {
        val iconPack = iconPacksAdapter.getItem(position)
        val iconPackPackageName = iconPack.componentName.packageName

        icons?.removeObserver(iconsObserver)

        iconsAdapter.clearItems()
        iconsAdapter.iconPackPackageName = iconPackPackageName

        iconsRecyclerView.visibility = View.GONE
        iconsLoadingProgressBar.visibility = View.VISIBLE

        icons = viewModel.icons(
            iconPackPackageName,
            requireArguments().getString(FRAGMENT_ARG_APP_NAME, ""),
            requireArguments().getParcelable(FRAGMENT_ARG_COMPONENT_NAME) ?: ComponentName("", "")
        )
        icons?.observe(viewLifecycleOwner, iconsObserver)
    }

    private val iconsObserver = Observer<Pair<Boolean, List<IconPackIcon>>> {
        val isLoadingDone = it.first
        if (isLoadingDone) {
            iconsLoadingProgressBar.visibility = View.GONE
        }
        iconsRecyclerView.visibility = View.VISIBLE
        iconsAdapter.setItems(it.second)
    }

    private fun onIconSelected(icon: IconPackIcon, isSelected: Boolean) {
        if (isSelected) {
            setAppIconPreview(icon)
            showFab()
        } else {
            clearAppIconPreview(requireView())
            hideFab()
        }
    }

    private fun onIconLongTapped(icon: IconPackIcon) {
        val sb = StringBuilder()
        sb.append("Drawable: ${icon.drawableName}\n\n")
        for (component in icon.componentNames) {
            sb.append("${component.packageName} / ${component.className}\n\n")
        }
        ApplistDialogs.messageDialog(requireActivity(), "", sb.toString(), {}, {})
    }

    private fun showFab() {
        if (setFab.visibility == View.VISIBLE) {
            return
        }
        setFab.visibility = View.VISIBLE
        setFab.scaleX = 0.3f
        setFab.scaleY = 0.3f
        setFab.alpha = 0.0f
        setFab.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(300)
            .setListener(null)
    }

    private fun hideFab() {
        if (setFab.visibility == View.GONE) {
            return
        }
        setFab.animate()
            .scaleX(0.3f)
            .scaleY(0.3f)
            .alpha(0.0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    setFab.visibility = View.GONE
                }
            })
    }

    private fun setAppIconPreview(icon: IconPackIcon) {
        val iconBitmap = iconPackHelper.loadIcon(
            iconsAdapter.iconPackPackageName,
            icon.drawableName
        )
        appIcon.setImageBitmap(iconBitmap)
    }

    private fun clearAppIconPreview(view: View) {
        val customIconFile = File(requireArguments().getString(FRAGMENT_ARG_CUSTOM_ICON_PATH))
        if (customIconFile.exists()) {
            GlideApp.with(this)
                .load(customIconFile)
                .signature(FileSignature(customIconFile))
                .into(view.appIcon)
            return
        }

        val iconPath = requireArguments().getString(FRAGMENT_ARG_ICON_PATH)
        if (iconPath != null) {
            val iconFile = File(iconPath)
            if (iconFile.exists()) {
                GlideApp.with(this)
                    .load(iconFile)
                    .signature(FileSignature(iconFile))
                    .into(view.appIcon)
                return
            }
        }

        val componentName =
            requireArguments().getParcelable<ComponentName>(FRAGMENT_ARG_COMPONENT_NAME)
        if (componentName != null) {
            GlideApp.with(this)
                .load(componentName)
                .into(view.appIcon)
        }
    }

    private fun setAppIcon(icon: IconPackIcon) {
        if (!icon.isValid()) {
            return
        }
        viewModel.setAppIcon(
            requireArguments().getLong(FRAGMENT_ARG_APPLIST_ITEM_ID),
            iconsAdapter.iconPackPackageName,
            icon.drawableName,
            requireArguments().getString(FRAGMENT_ARG_CUSTOM_ICON_PATH)!!
        )
        EventBus.getDefault().post(DoneEvent())
    }

}
