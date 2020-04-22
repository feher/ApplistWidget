package net.feheren_fekete.applist.applistpage

import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.applist_page_page_fragment.*
import kotlinx.android.synthetic.main.applist_page_page_fragment.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.BuildConfig
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuAdapter
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuItem
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuListener
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.NotificationListener
import net.feheren_fekete.applist.applistpage.viewmodel.*
import net.feheren_fekete.applist.launcher.ScreenshotUtils
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.AppUtils
import net.feheren_fekete.applist.utils.ScreenUtils
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class ApplistPagePageFragment : Fragment(), ApplistAdapter.ItemListener {

    class ShowToolbarEvent
    class HideToolbarEvent

    data class ShowIconPickerEvent(
            val applistItemId: Long,
            val appName: String,
            val componentName: ComponentName?,
            val iconPath: String?,
            val customIconPath: String)

    enum class ItemMenuAction {
        AppInfo,
        RenameApp,
        ChangeIcon,
        ClearBadge,
        RemoveShortcut,
        Uninstall,
        RenameSection,
        DeleteSection,
        SortSection,
        ReorderApps,
        ReorderSections
    }

    private val applistLog: ApplistLog by inject()
    private val screenshotUtils: ScreenshotUtils by inject()
    private val settingsUtils: SettingsUtils by inject()
    private val screenUtils: ScreenUtils by inject()
    private val badgeStore: BadgeStore by inject()
    private val applistPreferences: ApplistPreferences by inject()
    private val iconPreloadHelper: IconPreloadHelper by inject()
    private val shortcutHelper: ShortcutHelper by inject()

    private val handler = Handler()
    private lateinit var adapter: ApplistAdapter
    private var itemMenu: ListPopupWindow? = null
    private var itemMenuTarget: BaseItem? = null

    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isMovingStartables = false
    private var isMovingSections = false
    private var draggedToPosition = -1
    private var draggedToTime = 0L

    private lateinit var viewModel: ApplistViewModel

    private val launcherPageId: Long
        get() = arguments!!.getLong(ARG_LAUNCHER_PAGE_ID)

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            scheduleScreenshot()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            scheduleScreenshot()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            scheduleScreenshot()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            scheduleScreenshot()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            scheduleScreenshot()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            scheduleScreenshot()
        }

        private fun scheduleScreenshot() {
            screenshotUtils.scheduleScreenshot(activity, launcherPageId, ScreenshotUtils.DELAY_SHORT)
        }
    }

    private val itemMenuClickListener = object : ItemMenuListener {
        override fun onItemSelected(item: ItemMenuItem) {
            if (item.data is ItemMenuAction) {
                when (item.data) {
                    ItemMenuAction.ClearBadge -> {
                        applistLog.analytics(ApplistLog.CLEAR_APP_BADGE, ApplistLog.ITEM_MENU)
                        clearAppBadge(itemMenuTarget as AppItem)
                    }
                    ItemMenuAction.AppInfo -> {
                        applistLog.analytics(ApplistLog.SHOW_APP_INFO, ApplistLog.ITEM_MENU)
                        showAppInfo(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.RenameApp -> {
                        applistLog.analytics(ApplistLog.RENAME_APP, ApplistLog.ITEM_MENU)
                        renameApp(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.ChangeIcon -> {
                        applistLog.analytics(ApplistLog.CHANGE_APP_ICON, ApplistLog.ITEM_MENU)
                        changeAppIcon(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.Uninstall -> {
                        applistLog.analytics(ApplistLog.UNINSTALL_APP, ApplistLog.ITEM_MENU)
                        uninstallApp(itemMenuTarget as AppItem)
                    }
                    ItemMenuAction.RemoveShortcut -> {
                        applistLog.analytics(ApplistLog.REMOVE_SHORTCUT, ApplistLog.ITEM_MENU)
                        removeShortcut(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.RenameSection -> {
                        applistLog.analytics(ApplistLog.RENAME_SECTION, ApplistLog.ITEM_MENU)
                        renameSection(itemMenuTarget as SectionItem)
                    }
                    ItemMenuAction.DeleteSection -> {
                        applistLog.analytics(ApplistLog.DELETE_SECTION, ApplistLog.ITEM_MENU)
                        deleteSection(itemMenuTarget as SectionItem)
                    }
                    ItemMenuAction.SortSection -> {
                        applistLog.analytics(ApplistLog.SORT_SECTION, ApplistLog.ITEM_MENU)
                        sortSection(itemMenuTarget as SectionItem)
                    }
                    ItemMenuAction.ReorderApps -> {
                        applistLog.analytics(ApplistLog.REORDER_ITEMS, ApplistLog.ITEM_MENU)
                        toggleStartableSelected(itemMenuTarget as StartableItem)
                        setMoveStartablesEnabled(!isMovingStartables)
                    }
                    ItemMenuAction.ReorderSections -> {
                        applistLog.analytics(ApplistLog.REORDER_SECTIONS, ApplistLog.ITEM_MENU)
                        setMoveSectionsEnabled(!isMovingSections)
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item.data is ShortcutInfo) {
                applistLog.analytics(ApplistLog.START_APP_SHORTCUT, ApplistLog.ITEM_MENU)
                launchAppShortcut(item.data)
            } else if (item.data is StatusBarNotification) {
                applistLog.analytics(ApplistLog.START_NOTIFICATION, ApplistLog.ITEM_MENU)
                launchNotification(item.data)
            }
            itemMenu?.dismiss()
        }

        override fun onItemPinClicked(item: ItemMenuItem) {
            val c = this@ApplistPagePageFragment.context ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item.data is ShortcutInfo) {
                shortcutHelper.pinAppShortcut(lifecycleScope, c, item.data, null)
            }
            itemMenu?.dismiss()
        }

        override fun onItemSwiped(item: ItemMenuItem) {
            applistLog.analytics(ApplistLog.CANCEL_NOTIFICATION, ApplistLog.ITEM_MENU)
            cancelNotification(item.data as StatusBarNotification)
            itemMenu?.dismiss()
        }
    }

    private val isAttached: Boolean
        get() = context != null && activity != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ApplistAdapter(applistLog, this)
        viewModel = ViewModelProvider(this).get(ApplistViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.applist_page_page_fragment, container, false)

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        //val topPadding = screenUtils.getStatusBarHeight(context) + screenUtils.getActionBarHeight(context)
        // We add a bottom padding to the RecyclerView to "push it up" above the navigation bar.
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        //val bottomPadding = if (screenUtils.hasNavigationBar(context)) screenUtils.getNavigationBarHeight(context) else 0
        //view.recyclerView.setPadding(0, topPadding, 0, bottomPadding)

        val columnSize = screenUtils.dpToPx(context, settingsUtils.columnWidth.toFloat()).roundToInt()
        val screenWidth = screenUtils.getScreenWidth(context)
        var tempColumnCount = screenWidth / columnSize
        if (tempColumnCount <= 0) {
            applistLog.log(IllegalStateException("Invalid column count: $tempColumnCount"))
            tempColumnCount = 4
        }
        val columnCount = tempColumnCount
        val layoutManager = GridLayoutManager(context!!, columnCount)
        layoutManager.isSmoothScrollbarEnabled = true
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    ApplistAdapter.STARTABLE_ITEM_VIEW -> 1
                    ApplistAdapter.SECTION_ITEM_VIEW -> columnCount
                    else -> 1
                }
            }
        }
        view.recyclerView.layoutManager = layoutManager
        view.recyclerView.adapter = adapter

        itemTouchHelper = ItemTouchHelper(
                ApplistItemTouchCallback(
                        canMoveHorizontally = { position ->
                            adapter.getItem(position) is StartableItem
                        },
                        onItemDrag = ::onItemDragged,
                        onItemDropped = ::onItemDropped)
        )
        itemTouchHelper.attachToRecyclerView(view.recyclerView)

        view.doneButton.setOnClickListener {
            setMoveStartablesEnabled(false)
            setMoveSectionsEnabled(false)
        }

        view.moveToSectionButton.setOnClickListener {
            applistLog.analytics(ApplistLog.MOVE_APP_TO_SECTION, ApplistLog.ACTION_BUTTONS)
            moveApps(adapter.selectedIds)
        }

        view.clearSelectionButton.setOnClickListener {
            applistLog.analytics(ApplistLog.CLEAR_SELECTION, ApplistLog.ACTION_BUTTONS)
            clearSelection()
        }

        iconPreloadHelper.setupPreloader(
                context!!, view.recyclerView,
                adapter, columnCount * 2)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getItems().observe(viewLifecycleOwner, Observer {
            adapter.setItems(it)
            updateActionButtons()
        })
    }

    override fun onResume() {
        super.onResume()
        if (applistPreferences.versionCode < BuildConfig.VERSION_CODE
            || applistPreferences.showWhatsNew
        ) {
            ApplistDialogs.messageDialog(
                requireActivity(),
                R.string.whats_new_title,
                R.string.whats_new_69,
                onOk = {
                    applistPreferences.showWhatsNew = false
                    applistPreferences.versionCode = BuildConfig.VERSION_CODE
                },
                onCancel = {}
            )
        } else if (applistPreferences.showRearrangeItemsHelp) {
            AlertDialog.Builder(activity!!)
                    .setMessage(R.string.rearrange_items_help)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it) { _, _ ->
                        applistPreferences.showRearrangeItemsHelp = false
                    }
                    .show()
        } else if (applistPreferences.showUseLauncherTip) {
            AlertDialog.Builder(activity!!)
                    .setMessage(R.string.use_launcher_tip)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it) { _, _ ->
                        applistPreferences.showUseLauncherTip = false
                    }
                    .show()
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)
    }

    override fun onPause() {
        super.onPause()
        adapter.unregisterAdapterDataObserver(adapterDataObserver)
    }

    override fun onStop() {
        super.onStop()
        if (adapter.isFilteredByName) {
            deactivateNameFilter()
        }
    }

    fun isFilteredByName() = adapter.isFilteredByName

    fun activateNameFilter() {
        if (adapter.isFilteredByName) {
            return
        }

        setNameFilter("")
        recyclerView.setPadding(
                0, screenUtils.getActionBarHeight(context), 0, 0)
    }

    fun deactivateNameFilter() {
        if (!adapter.isFilteredByName) {
            return
        }

        setNameFilter(null)
        recyclerView.setPadding(0, 0, 0, 0)
    }

    fun setNameFilter(filterText: String?) {
        adapter.setNameFilter(filterText)
        recyclerView.scrollToPosition(0)
    }

    fun handleMenuItem(itemId: Int): Boolean {
        var isHandled = false
        when (itemId) {
            R.id.action_create_section -> {
                applistLog.analytics(ApplistLog.CREATE_SECTION, ApplistLog.OPTIONS_MENU)
                createSection(emptyList())
                isHandled = true
            }
        }
        return isHandled
    }

    fun getItemMenuTarget() = itemMenuTarget!!

    override fun onStartableLongTapped(startableItem: StartableItem) {
        if (isMovingSections) {
            return
        }

        if (isMovingStartables) {
            if (adapter.selectedCount == 1) {
                startDraggingStartable(startableItem)
            } else {
                Toast.makeText(
                        context, "Only single items can be dragged", Toast.LENGTH_SHORT).show()
            }
        } else {
            showContextMenuForStartable(startableItem)
        }
    }

    override fun onStartableTapped(startableItem: StartableItem) {
        if (isMovingSections) {
            return
        }

        if (isMovingStartables) {
            toggleStartableSelected(startableItem)
        } else {
            launchStartable(startableItem)
        }
    }

    override fun onStartableTouched(startableItem: StartableItem) {
//        if (!isMovingStartables) {
//            return
//        }
//        val viewHolder = recyclerView.findViewHolderForItemId(startableItem.id)
//        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onSectionLongTapped(sectionItem: SectionItem) {
        if (isMovingStartables || isMovingSections) {
            return
        }

        val c = context ?: return

        val itemMenuItems = ArrayList<ItemMenuItem>()
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.section_item_menu_rename), ItemMenuAction.RenameSection))
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.section_item_menu_reorder_sections), ItemMenuAction.ReorderSections))
        if (!settingsUtils.isKeepAppsSortedAlphabetically) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.section_item_menu_sort_apps), ItemMenuAction.SortSection))
        }
        if (sectionItem.isRemovable) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.section_item_menu_delete), ItemMenuAction.DeleteSection))
        }
        val itemMenuAdapter = ItemMenuAdapter(c)
        itemMenuAdapter.setListener(itemMenuClickListener)
        itemMenuAdapter.setItems(itemMenuItems)

        val sectionItemHolder = recyclerView.findViewHolderForItemId(
                sectionItem.id) as SectionItemHolder

        itemMenuTarget = sectionItem

        val menu = ListPopupWindow(c)
        menu.setContentWidth(resources.getDimensionPixelSize(R.dimen.item_menu_width))
        menu.height = ListPopupWindow.WRAP_CONTENT
        menu.setOnDismissListener {
            itemMenu = null
        }
        menu.anchorView = sectionItemHolder.layout
        menu.setAdapter(itemMenuAdapter)
        menu.isModal = true
        menu.show()
        itemMenu = menu
    }

    override fun onSectionTapped(sectionItem: SectionItem) {
        if (isMovingStartables || isMovingSections) {
            return
        }
        if (adapter.isFilteredByName) {
            return
        }
        val wasSectionCollapsed = sectionItem.isCollapsed
        if (!wasSectionCollapsed) {
            applistLog.analytics(ApplistLog.COLLAPSE_SECTION, ApplistLog.ITEM_MENU)
        } else {
            applistLog.analytics(ApplistLog.UNCOLLAPSE_SECTION, ApplistLog.ITEM_MENU)
        }
        viewModel.setSectionCollapsed(sectionItem.id, !wasSectionCollapsed)
    }

    override fun onSectionTouched(sectionItem: SectionItem) {
        if (!isMovingSections) {
            return
        }
        val viewHolder = recyclerView.findViewHolderForItemId(sectionItem.id)
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun clearSelection() {
        adapter.unselectAll()
        updateActionButtons()
    }

    private fun toggleStartableSelected(startableItem: StartableItem) {
        adapter.setSelected(startableItem, !startableItem.isSelected)
        updateActionButtons()
    }

    private fun launchStartable(startableItem: StartableItem) {
        val c = context ?: return
        if (startableItem is AppItem) {
            val launchIntent = Intent(Intent.ACTION_MAIN)
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            val appComponentName = ComponentName(
                    startableItem.packageName, startableItem.className)
            launchIntent.component = appComponentName

            val smsAppComponentName = AppUtils.getSmsApp(c)
            if (appComponentName == smsAppComponentName) {
                badgeStore.setBadgeCount(
                        smsAppComponentName.packageName,
                        smsAppComponentName.className,
                        0)
            }
            val phoneAppComponentName = AppUtils.getPhoneApp(c.applicationContext)
            if (appComponentName == phoneAppComponentName) {
                badgeStore.setBadgeCount(
                        phoneAppComponentName.packageName,
                        phoneAppComponentName.className,
                        0)
            }

            try {
                c.startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(c, R.string.cannot_start_app, Toast.LENGTH_SHORT).show()
                applistLog.log(e)
            }

        } else if (startableItem is ShortcutItem) {
            try {
                c.startActivity(startableItem.intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
                applistLog.log(e)
            }

        } else if (startableItem is AppShortcutItem) {
            val shortcutInfo = resolveAppShortcutInfo(startableItem)
            launchAppShortcut(shortcutInfo)
        }
    }

    private fun startDraggingStartable(startableItem: StartableItem) {
        adapter.setDragged(startableItem, true)
        handler.postDelayed({
            val viewHolder = recyclerView.findViewHolderForItemId(startableItem.id)
            itemTouchHelper.startDrag(viewHolder)
        }, 100)
    }

    private fun showContextMenuForStartable(startableItem: StartableItem) {
        val c = context ?: return

        itemMenuTarget = startableItem

        // Change the adapter only after the popup window has been displayed.
        // Otherwise the popup window appears in a jittery way due to simultaneous change in the adapter.
        handler.postDelayed({ adapter.setHighlighted(itemMenuTarget, true) }, 300)

        val isApp = startableItem is AppItem
        val isShortcut = startableItem is ShortcutItem || startableItem is AppShortcutItem
        val itemMenuItems = ArrayList<ItemMenuItem>()
        if (isApp) {
            addAppNotificationsToItemMenu(startableItem as AppItem, itemMenuItems)
            addAppShortcutsToItemMenu(startableItem, itemMenuItems)
        }
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_rename), ItemMenuAction.RenameApp))
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_reorder_items), ItemMenuAction.ReorderApps))
        if (isApp) {
            if (settingsUtils.showBadge) {
                val appItem = startableItem as AppItem
                val badgeCount = badgeStore.getBadgeCount(
                        appItem.packageName,
                        appItem.className)
                if (badgeCount > 0) {
                    itemMenuItems.add(createActionMenuItem(
                            resources.getString(R.string.app_item_menu_clear_badge), ItemMenuAction.ClearBadge))
                }
            }
        }
        //
        // Icon pack support is experimental.
        // Enable it only when it's fully implemented.
        // TODO:
        // * Find relevant icons for the selected app from the icon pack.
        //   Currently we just show a list of all the icons in the icon pack without any ordering.
        //   This way it's very difficult to find e.g. the "Email" related icons.
        //
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_change_icon), ItemMenuAction.ChangeIcon))
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_information), ItemMenuAction.AppInfo))
        if (isApp) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.app_item_menu_uninstall), ItemMenuAction.Uninstall))
        }
        if (isShortcut) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.app_item_menu_remove_shortcut), ItemMenuAction.RemoveShortcut))
        }
        val itemMenuAdapter = ItemMenuAdapter(c)
        itemMenuAdapter.setListener(itemMenuClickListener)
        itemMenuAdapter.setItems(itemMenuItems)

        val startableItemHolder = recyclerView.findViewHolderForItemId(
                startableItem.id) as StartableItemHolder

        val menu = ListPopupWindow(c)
        val hasNotificationWithRemoteViews = hasNotificationWithRemoteViews(itemMenuItems)
        menu.setContentWidth(resources.getDimensionPixelSize(
                if (hasNotificationWithRemoteViews) R.dimen.item_menu_width_large else R.dimen.item_menu_width))
        menu.height = ListPopupWindow.WRAP_CONTENT
        menu.setOnDismissListener {
            adapter.setHighlighted(itemMenuTarget, false)
            itemMenu = null
        }
        menu.anchorView = startableItemHolder.layout
        menu.setAdapter(itemMenuAdapter)
        menu.isModal = true
        menu.show()
        itemMenu = menu
    }

    private fun createNotificationMenuItem(text: String, icon: Drawable?, remoteViews: RemoteViews?, statusBarNotification: StatusBarNotification): ItemMenuItem {
        var t = text
        if (t.isEmpty() && remoteViews == null) {
            t = requireContext().getString(R.string.app_item_menu_notification_without_title)
        }
        return ItemMenuItem(
                "", t, icon, R.drawable.notification_menu_item_background,
                true, false, remoteViews, statusBarNotification)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1) // ShortcutInfo
    private fun createAppShortcutMenuItem(name: String, icon: Drawable?, shortcutInfo: ShortcutInfo): ItemMenuItem {
        return ItemMenuItem(
                name, "", icon, 0,
                false, true, null, shortcutInfo)
    }

    private fun createActionMenuItem(name: String, action: ItemMenuAction): ItemMenuItem {
        return ItemMenuItem(
                name, "", null, 0,
                false, false, null, action)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun addAppNotificationsToItemMenu(appItem: AppItem, itemMenuItems: MutableList<ItemMenuItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val c = context ?: return
        val statusBarNotifications = NotificationListener.getNotificationsForPackage(appItem.packageName)
        // Iterate backwards because the notifications are sorted ascending by time.
        // We want to add the newest first.
        for (i in statusBarNotifications.indices.reversed()) {
            val sbn = statusBarNotifications[i]

            if (!shouldShowNotification(sbn, statusBarNotifications)) {
                continue
            }

            val notification = sbn.notification
            val textBuilder = StringBuilder()
            val extras = notification.extras
            var extraText: CharSequence? = extras.getCharSequence(Notification.EXTRA_TITLE, null)
            if (extraText != null) {
                textBuilder.append(extraText).append(", ")
            }
            extraText = extras.getCharSequence(Notification.EXTRA_TEXT, null)
            if (extraText != null) {
                textBuilder.append(extraText).append(", ")
            }
            extraText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT, null)
            if (extraText != null) {
                textBuilder.append(extraText).append(", ")
            }
            extraText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT, null)
            if (extraText != null) {
                textBuilder.append(extraText).append(", ")
            }
            extraText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT, null)
            if (extraText != null) {
                textBuilder.append(extraText).append(", ")
            }
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (textLines != null) {
                for (textLine in textLines) {
                    textBuilder.append(textLine.toString()).append(", ")
                }
            }
            val text = if (textBuilder.length >= 2)
                textBuilder.substring(0, textBuilder.length - 2)
            else
                textBuilder.toString()

            var icon: Icon? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val iconType = notification.badgeIconType
                if (iconType == Notification.BADGE_ICON_LARGE) {
                    icon = notification.getLargeIcon()
                } else if (iconType == Notification.BADGE_ICON_SMALL || iconType == Notification.BADGE_ICON_NONE) {
                    icon = notification.smallIcon
                }
            }
            if (icon == null) {
                icon = notification.smallIcon
            }
            if (icon == null) {
                icon = notification.getLargeIcon()
            }
            val iconDrawable: Drawable? = if (icon != null) {
                icon.setTint(if (notification.color != 0) notification.color else Color.GRAY)
                icon.loadDrawable(c)
            } else {
                c.resources.getDrawable(R.drawable.ic_notification, null)
            }

            itemMenuItems.add(createNotificationMenuItem(text, iconDrawable, sbn.notification.contentView, sbn))
        }
    }

    private fun shouldShowNotification(statusBarNotification: StatusBarNotification,
                                       statusBarNotifications: List<StatusBarNotification>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true
        }
        // In case of grouped notifications
        // * show only the summary notification or
        // * every item if there is no group summary.
        var isGroupSummary = false
        var hasGroupSummary = false
        if (statusBarNotification.isGroup) {
            isGroupSummary = statusBarNotification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
            if (!isGroupSummary) {
                for (statusBarNotification2 in statusBarNotifications) {
                    if (statusBarNotification2 !== statusBarNotification
                            && statusBarNotification.groupKey == statusBarNotification2.groupKey
                            && statusBarNotification2.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
                        hasGroupSummary = true
                        break
                    }
                }
            }
        }
        return isGroupSummary || !hasGroupSummary
    }

    private fun hasNotificationWithRemoteViews(itemMenuItems: List<ItemMenuItem>): Boolean {
        for (itemMenuItem in itemMenuItems) {
            if (itemMenuItem.name.isEmpty() && itemMenuItem.text.isEmpty() && itemMenuItem.contentRemoteViews != null) {
                return true
            }
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun addAppShortcutsToItemMenu(appItem: AppItem, itemMenuItems: MutableList<ItemMenuItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return
        }
        val c = context ?: return
        val shortcutInfos = performAppShortcutQuery(
                appItem.packageName, null,
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        shortcutInfos.sortWith( Comparator { a, b ->
            when {
                !a.isDynamic && b.isDynamic -> -1
                a.isDynamic && !b.isDynamic -> 1
                else -> Integer.compare(a.rank, b.rank)
            }
        })
        val maxShortcutCount = 10
        if (shortcutInfos.size > maxShortcutCount) {
            applistLog.log(RuntimeException("Max $maxShortcutCount app shortcuts are supported!"))
        }
        val launcherApps = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        var i = 0
        while (i < shortcutInfos.size && i < maxShortcutCount) {
            val shortcutInfo = shortcutInfos[i]
            var iconDrawable: Drawable? = null
            try {
                iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0)
            } catch (e: Exception) {
                applistLog.log(e)
            }

            try {
                iconDrawable = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0)
            } catch (e: Exception) {
                applistLog.log(e)
            }

            if (iconDrawable == null) {
                iconDrawable = resources.getDrawable(R.drawable.app_shortcut_default, null)
            }
            itemMenuItems.add(createAppShortcutMenuItem(
                    shortcutInfo.shortLabel!!.toString(),
                    iconDrawable,
                    shortcutInfo))
            ++i
        }
    }

    private fun onItemDragged(fromPosition: Int, toPosition: Int): Boolean {
        if (toPosition == draggedToPosition) {
            val elapsedTime = System.currentTimeMillis() - draggedToTime
            if (elapsedTime > 300) {
                draggedToPosition = -1;
                return adapter.moveItem(fromPosition, toPosition)
            } else {
                return false
            }
        } else {
            draggedToPosition = toPosition
            draggedToTime = System.currentTimeMillis()
            return false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onItemDropped(position: Int) {
        handler.postDelayed({
            adapter.clearDragged()
            val itemIds = adapter.allItemIds
            val parentSectionIds = adapter.allParentSectionIds
            viewModel.updateItemPositionsAndParentSectionIds(itemIds, parentSectionIds)
        }, 100)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun performAppShortcutQuery(packageName: String,
                                        shortcutId: String?,
                                        queryFlags: Int): MutableList<ShortcutInfo> {
        val c = context ?: return mutableListOf()
        val result = ArrayList<ShortcutInfo>()
        val launcherApps = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps?
        if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
            val profiles = ArrayList<UserHandle>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                profiles.addAll(launcherApps.profiles)
            } else {
                profiles.add(android.os.Process.myUserHandle())
            }
            for (userHandle in profiles) {
                val shortcutQuery = LauncherApps.ShortcutQuery()
                shortcutQuery.setPackage(packageName)
                shortcutQuery.setQueryFlags(queryFlags)
                if (shortcutId != null) {
                    shortcutQuery.setShortcutIds(listOf(shortcutId))
                }
                var shortcutInfos: List<ShortcutInfo>? = null
                try {
                    shortcutInfos = launcherApps.getShortcuts(shortcutQuery, userHandle)
                } catch (e: IllegalStateException) {
                    // Nothing. Just don't crash.
                }

                if (shortcutInfos != null) {
                    result.addAll(shortcutInfos)
                }
            }
        }
        return result
    }

    // This is used only for testing pinning of app shortcuts.
    @TargetApi(Build.VERSION_CODES.O)
    private fun testPinShortcut(shortcutInfo: ShortcutInfo) {
        Log.d(TAG, "REQUEST PIN " + shortcutInfo.getPackage() + " " + shortcutInfo.id)
        val c = context ?: return
        val shortcutManager = c.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            shortcutManager.requestPinShortcut(shortcutInfo, null)
        }
    }

    private fun launchAppShortcut(shortcutInfo: ShortcutInfo?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return
        }
        val c = context ?: return
        if (shortcutInfo == null) {
            Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
        } else if (shortcutInfo.isEnabled) {
            val launcherApps = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps?
            if (launcherApps != null) {
                try {
                    launcherApps.startShortcut(shortcutInfo, null, null)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
                } catch (e: IllegalStateException) {
                    Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(c, R.string.cannot_start_disabled_shortcut, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchNotification(statusBarNotification: StatusBarNotification) {
        if (!isAttached) {
            return
        }
        try {
            if (statusBarNotification.notification.contentIntent != null) {
                statusBarNotification.notification.contentIntent.send()
            }
            if (statusBarNotification.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) {
                val cancelNotificationIntent = Intent(activity, NotificationListener::class.java)
                cancelNotificationIntent.action = NotificationListener.ACTION_CANCEL_NOTIFICATION
                cancelNotificationIntent.putExtra(NotificationListener.EXTRA_NOTIFICATION_KEY, statusBarNotification.key)
                activity!!.startService(cancelNotificationIntent)
            }
        } catch (e: PendingIntent.CanceledException) {
            // Ignore.
        }

    }

    private fun cancelNotification(statusBarNotification: StatusBarNotification) {
        if (!isAttached) {
            return
        }
        val isOngoing = statusBarNotification.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        if (!isOngoing) {
            val cancelNotificationIntent = Intent(activity, NotificationListener::class.java)
            cancelNotificationIntent.action = NotificationListener.ACTION_CANCEL_NOTIFICATION
            cancelNotificationIntent.putExtra(NotificationListener.EXTRA_NOTIFICATION_KEY, statusBarNotification.key)
            activity!!.startService(cancelNotificationIntent)
        }
    }

    private fun resolveAppShortcutInfo(appShortcutItem: AppShortcutItem): ShortcutInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return null
        }
        val shortcutInfos = performAppShortcutQuery(
                appShortcutItem.packageName,
                appShortcutItem.shortcutId,
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                        or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        return if (!shortcutInfos.isEmpty()) shortcutInfos[0] else null
    }

    private fun clearAppBadge(appItem: AppItem) {
        badgeStore.setBadgeCount(appItem.packageName, appItem.className, 0)

        // Clear the badgecount also for the whole package.
        // This is necessary to get rid of notification badges that got stuck due to missed
        // notifications.
        badgeStore.setBadgeCount(appItem.packageName, "", 0)
    }

    private fun showAppInfo(startableItem: StartableItem) {
        val c = context ?: return
        var packageName: String? = null
        if (startableItem is AppItem) {
            packageName = startableItem.packageName
        } else if (startableItem is ShortcutItem) {
            packageName = startableItem.intent.getPackage()
            if (packageName == null) {
                val componentName = startableItem.intent.component
                if (componentName != null) {
                    packageName = componentName.packageName
                }
            }
        } else if (startableItem is AppShortcutItem) {
            packageName = startableItem.packageName
        }
        if (packageName != null) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            try {
                c.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                applistLog.log(e)
                Toast.makeText(c, R.string.cannot_show_app_info, Toast.LENGTH_SHORT).show()
            }

        } else {
            applistLog.log(RuntimeException("Package name is not available for shortcut"))
            Toast.makeText(c, R.string.cannot_show_app_info, Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(appItem: AppItem) {
        val c = context ?: return
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
        val uri = Uri.fromParts("package", appItem.packageName, null)
        intent.data = uri
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, false)
        c.startActivity(intent)
    }

    private fun removeShortcut(shortcutItem: StartableItem) {
        viewModel.removeShortcut(shortcutItem.id)
    }

    private fun moveApps(startableItemIds: List<Long>) {
        if (!isAttached) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val sections = viewModel.getSections()

            val sectionNames = arrayListOf<String>().apply {
                sections.forEach { section -> add(section.second) }
            }
            sectionNames.add(getString(R.string.move_app_to_new_section))

            ApplistDialogs.listDialog(
                    activity!!, getString(R.string.move_app_title), sectionNames) { itemIndex ->
                val moveToNewSection = (itemIndex == sectionNames.size - 1)
                if (moveToNewSection) {
                    createSection(startableItemIds)
                } else {
                    val sectionId = sections.get(itemIndex).first
                    viewModel.moveStartablesToSection(startableItemIds, sectionId, true)
                }
            }
        }
    }

    private fun renameApp(startableItem: StartableItem) {
        if (!isAttached) {
            return
        }
        val oldAppName = startableItem.getDisplayName()
        val startableNames = adapter.startableDisplayNames
        ApplistDialogs.textInputDialog(
                activity!!, R.string.startable_name, oldAppName,
                { appName ->
                    if (isAttached && startableNames.contains(appName)) {
                        return@textInputDialog resources.getString(R.string.dialog_error_app_name_exists)
                    }
                    null
                },
                { newAppName ->
                    viewModel.setStartableCustomName(startableItem.id, newAppName)
                })
    }

    private fun changeAppIcon(startableItem: StartableItem) {
        if (!isAttached) {
            return
        }
        val event = when (startableItem) {
            is AppItem -> ShowIconPickerEvent(
                    startableItem.id,
                    startableItem.getDisplayName(),
                    ComponentName(startableItem.packageName, startableItem.className),
                    null,
                    startableItem.customIconPath)
            is AppShortcutItem -> ShowIconPickerEvent(
                    startableItem.id,
                    startableItem.getDisplayName(),
                    null,
                    startableItem.iconPath,
                    startableItem.customIconPath)
            is ShortcutItem -> ShowIconPickerEvent(
                    startableItem.id,
                    startableItem.getDisplayName(),
                    null,
                    startableItem.iconPath,
                    startableItem.customIconPath)
            else -> throw java.lang.IllegalStateException()
        }
        EventBus.getDefault().post(event)
    }

    private fun renameSection(sectionItem: SectionItem) {
        if (!isAttached) {
            return
        }
        val oldSectionName = sectionItem.name
        val sectionNames = adapter.sectionNames
        ApplistDialogs.textInputDialog(
                activity!!, R.string.section_name, oldSectionName,
                { sectionName ->
                    if (isAttached && sectionNames.contains(sectionName)) {
                        return@textInputDialog resources.getString(R.string.dialog_error_section_exists)
                    }
                    null
                },
                { newSectionName ->
                    val sectionId = sectionItem.id
                    viewModel.setSectionName(sectionId, newSectionName)
                })
    }

    private fun deleteSection(sectionItem: SectionItem) {
        if (!isAttached) {
            return
        }
        val sectionName = sectionItem.name
        val uncategorizedSectionName = adapter.uncategorizedSectionName
        ApplistDialogs.questionDialog(
                activity!!,
                resources.getString(R.string.remove_section_title),
                resources.getString(R.string.remove_section_message, sectionName, uncategorizedSectionName),
                {
                    viewModel.removeSection(sectionItem.id)
                },
                {
                    // Nothing.
                })
    }

    private fun sortSection(sectionItem: SectionItem) {
        viewModel.sortSection(sectionItem.id)
    }

    private fun createSection(appsToMove: List<Long>) {
        if (!isAttached) {
            return
        }
        val sectionNames = adapter.sectionNames
        ApplistDialogs.textInputDialog(
                activity!!, R.string.section_name, "",
                { sectionName ->
                    if (isAttached && sectionNames.contains(sectionName)) {
                        return@textInputDialog resources.getString(R.string.dialog_error_section_exists)
                    }
                    null
                },
                { sectionName ->
                    if (!sectionName.isEmpty()) {
                        viewModel.createSection(sectionName, appsToMove)
                    }
                })
    }

    private fun setMoveStartablesEnabled(enable: Boolean) {
        if (!isAttached) {
            return
        }
        if (isMovingStartables == enable) {
            return
        }
        if (isFilteredByName() && enable) {
            return
        }

        isMovingStartables = enable
        adapter.setSelectionModeEnabled(isMovingStartables)

        if (isMovingStartables) {
            showActionButtons(false)
            EventBus.getDefault().post(HideToolbarEvent())
            viewModel.setAllSectionsCollapsed(false)
            if (applistPreferences.showReorderAppsHelp) {
                AlertDialog.Builder(activity!!)
                        .setMessage(R.string.reorder_apps_help)
                        .setCancelable(true)
                        .setPositiveButton(R.string.got_it) { _, _ ->
                            applistPreferences.showReorderAppsHelp = false
                        }
                        .show()
            }
        } else {
            clearSelection()
            hideActionButtons()
            EventBus.getDefault().post(ShowToolbarEvent())
        }
    }

    private fun setMoveSectionsEnabled(enable: Boolean) {
        if (!isAttached) {
            return
        }
        if (isMovingSections == enable) {
            return
        }
        if (isFilteredByName() && enable) {
            return
        }

        isMovingSections = enable

        if (isMovingSections) {
            Toast.makeText(context, "Drag and drop to reorder", Toast.LENGTH_SHORT).show()
            showActionButtons(true)
            EventBus.getDefault().post(HideToolbarEvent())
            viewModel.setAllSectionsCollapsed(true)
        } else {
            hideActionButtons()
            EventBus.getDefault().post(ShowToolbarEvent())
            viewModel.setAllSectionsCollapsed(false)
        }
    }

    private fun showActionButtons(onlyDoneButton: Boolean) {
        actionButtonsLayout.visibility = View.VISIBLE
        if (onlyDoneButton) {
            clearSelectionButton.visibility = View.GONE
            clearSelectionButtonText.visibility = View.GONE
            moveToSectionButton.visibility = View.GONE
            moveToSectionButtonText.visibility = View.GONE
        } else {
            clearSelectionButton.visibility = View.VISIBLE
            clearSelectionButtonText.visibility = View.VISIBLE
            moveToSectionButton.visibility = View.VISIBLE
            moveToSectionButtonText.visibility = View.VISIBLE
        }
        recyclerView.setPadding(
                0, 0, 0,
                Math.round(
                        0.7f * resources.getDimensionPixelSize(R.dimen.applist_action_buttons_height))
        )
    }

    private fun hideActionButtons() {
        actionButtonsLayout.visibility = View.GONE
        recyclerView.setPadding(0, 0, 0, 0)
    }

    private fun updateActionButtons() {
        val hasSelectedItem = (adapter.selectedCount > 0)
        if (hasSelectedItem) {
            clearSelectionButton.setBackgroundResource(R.drawable.applist_fab_background)
            clearSelectionButton.isEnabled = true
            moveToSectionButton.setBackgroundResource(R.drawable.applist_fab_background)
            moveToSectionButton.isEnabled = true
        } else {
            clearSelectionButton.setBackgroundResource(R.drawable.applist_disabled_fab_background)
            clearSelectionButton.isEnabled = false
            moveToSectionButton.setBackgroundResource(R.drawable.applist_disabled_fab_background)
            moveToSectionButton.isEnabled = false
        }
    }

    companion object {

        private val TAG = ApplistPagePageFragment::class.java.simpleName

        private const val ARG_APPLIST_PAGE_ID = "applistPageId"
        private const val ARG_APPLIST_PAGE_NAME = "applistPageName"
        private const val ARG_LAUNCHER_PAGE_ID = "launcherPageId"

        fun newInstance(pageItem: PageItem,
                        launcherPageId: Long): ApplistPagePageFragment {
            val fragment = ApplistPagePageFragment()

            val args = Bundle()
            args.putLong(ARG_APPLIST_PAGE_ID, pageItem.id)
            args.putString(ARG_APPLIST_PAGE_NAME, pageItem.name)
            args.putLong(ARG_LAUNCHER_PAGE_ID, launcherPageId)
            fragment.arguments = args

            return fragment
        }
    }

}
