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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.applist_page_page_fragment.*
import kotlinx.android.synthetic.main.applist_page_page_fragment.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.ApplistPreferences
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuAdapter
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuItem
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuListener
import net.feheren_fekete.applist.applistpage.model.ApplistModel
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.NotificationListener
import net.feheren_fekete.applist.applistpage.viewmodel.*
import net.feheren_fekete.applist.launcher.ScreenshotUtils
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.AppUtils
import net.feheren_fekete.applist.utils.ScreenUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import java.util.*

class ApplistPagePageFragment : Fragment(), ApplistAdapter.ItemListener {

    data class ShowIconPickerEvent(
            val appName: String,
            val componentName: ComponentName?,
            val iconPath: String?,
            val customIconPath: String)

    enum class ItemMenuAction {
        AppInfo,
        MoveAppToSection,
        RenameApp,
        ChangeIcon,
        ClearBadge,
        RemoveShortcut,
        Uninstall,
        RenameSection,
        DeleteSection,
        SortSection,
    }

    private val applistModel: ApplistModel by inject()
    private val screenshotUtils: ScreenshotUtils by inject()
    private val settingsUtils: SettingsUtils by inject()
    private val screenUtils: ScreenUtils by inject()
    private val badgeStore: BadgeStore by inject()
    private val applistPreferences: ApplistPreferences by inject()
    private val iconPreloadHelper: IconPreloadHelper by inject()

    private val handler = Handler()
    private lateinit var pageItem: PageItem
    private lateinit var adapter: ApplistAdapter
    private var itemMenu: ListPopupWindow? = null
    private var itemMenuTarget: BaseItem? = null

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
                        ApplistLog.getInstance().analytics(ApplistLog.CLEAR_APP_BADGE, ApplistLog.ITEM_MENU)
                        clearAppBadge(itemMenuTarget as AppItem)
                    }
                    ItemMenuAction.AppInfo -> {
                        ApplistLog.getInstance().analytics(ApplistLog.SHOW_APP_INFO, ApplistLog.ITEM_MENU)
                        showAppInfo(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.MoveAppToSection -> {
                        ApplistLog.getInstance().analytics(ApplistLog.MOVE_APP_TO_SECTION, ApplistLog.ITEM_MENU)
                        moveApp(itemMenuTarget as AppItem)
                    }
                    ItemMenuAction.RenameApp -> {
                        ApplistLog.getInstance().analytics(ApplistLog.RENAME_APP, ApplistLog.ITEM_MENU)
                        renameApp(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.ChangeIcon -> {
                        ApplistLog.getInstance().analytics(ApplistLog.CHANGE_APP_ICON, ApplistLog.ITEM_MENU)
                        changeAppIcon(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.Uninstall -> {
                        ApplistLog.getInstance().analytics(ApplistLog.UNINSTALL_APP, ApplistLog.ITEM_MENU)
                        uninstallApp(itemMenuTarget as AppItem)
                    }
                    ItemMenuAction.RemoveShortcut -> {
                        ApplistLog.getInstance().analytics(ApplistLog.REMOVE_SHORTCUT, ApplistLog.ITEM_MENU)
                        removeShortcut(itemMenuTarget as StartableItem)
                    }
                    ItemMenuAction.RenameSection -> {
                        ApplistLog.getInstance().analytics(ApplistLog.RENAME_SECTION, ApplistLog.ITEM_MENU)
                        renameSection(itemMenuTarget as SectionItem)
                    }
                    ItemMenuAction.DeleteSection -> {
                        ApplistLog.getInstance().analytics(ApplistLog.DELETE_SECTION, ApplistLog.ITEM_MENU)
                        deleteSection(itemMenuTarget as SectionItem)
                    }
                    ItemMenuAction.SortSection -> {
                        ApplistLog.getInstance().analytics(ApplistLog.SORT_SECTION, ApplistLog.ITEM_MENU)
                        sortSection(itemMenuTarget as SectionItem)
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item.data is ShortcutInfo) {
                ApplistLog.getInstance().analytics(ApplistLog.START_APP_SHORTCUT, ApplistLog.ITEM_MENU)
                startAppShortcut(item.data)
            } else if (item.data is StatusBarNotification) {
                ApplistLog.getInstance().analytics(ApplistLog.START_NOTIFICATION, ApplistLog.ITEM_MENU)
                startNotification(item.data)
            }
            itemMenu?.dismiss()
        }

        override fun onItemSwiped(item: ItemMenuItem) {
            ApplistLog.getInstance().analytics(ApplistLog.CANCEL_NOTIFICATION, ApplistLog.ITEM_MENU)
            cancelNotification(item.data as StatusBarNotification)
            itemMenu?.dismiss()
        }
    }

    private val isAttached: Boolean
        get() = context != null && activity != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageItem = PageItem(
                arguments!!.getLong(ARG_APPLIST_PAGE_ID),
                arguments!!.getString(ARG_APPLIST_PAGE_NAME)!!)
        adapter = ApplistAdapter(this)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.applist_page_page_fragment, container, false)

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        val topPadding = screenUtils.getStatusBarHeight(context) + screenUtils.getActionBarHeight(context)
        // We add a bottom padding to the RecyclerView to "push it up" above the navigation bar.
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        val bottomPadding = if (screenUtils.hasNavigationBar(context)) screenUtils.getNavigationBarHeight(context) else 0
        view.recyclerView.setPadding(0, topPadding, 0, bottomPadding)

        val columnSize = Math.round(
                screenUtils.dpToPx(context,
                        settingsUtils.columnWidth.toFloat()))
        val screenWidth = screenUtils.getScreenWidth(context)
        var tempColumnCount = screenWidth / columnSize
        if (tempColumnCount <= 0) {
            ApplistLog.getInstance().log(IllegalStateException("Invalid column count: $tempColumnCount"))
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

        iconPreloadHelper.setupPreloader(
                context!!, view.recyclerView,
                adapter, columnCount * 2)

        loadAllItems()

        return view
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        if (applistPreferences.showRearrangeItemsHelp) {
            AlertDialog.Builder(activity!!)
                    .setMessage(R.string.rearrange_items_help)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it) { _, _ ->
                        applistPreferences.showRearrangeItemsHelp = false
                    }
                    .show()
        }
        if (applistPreferences.showUseLauncherTip) {
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
        EventBus.getDefault().unregister(this)
        adapter.unregisterAdapterDataObserver(adapterDataObserver)
    }

    override fun onStop() {
        super.onStop()
        if (adapter.isFilteredByName) {
            deactivateNameFilter()
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSectionsChangedEvent(event: ApplistModel.SectionsChangedEvent) {
        handler.post { loadAllItems() }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBadgeEvent(event: BadgeStore.BadgeEvent) {
        handler.post { loadAllItems() }
    }

    fun getPageItem() = pageItem

    fun isFilteredByName() = adapter.isFilteredByName

    fun activateNameFilter() {
        if (adapter.isFilteredByName) {
            return
        }

        setNameFilter("")
    }

    fun deactivateNameFilter() {
        if (!adapter.isFilteredByName) {
            return
        }

        setNameFilter(null)
    }

    fun setNameFilter(filterText: String?) {
        adapter.setNameFilter(filterText)
        recyclerView.scrollToPosition(0)
    }

    fun isItemMenuOpen() = itemMenu != null

    fun closeItemMenu() {
        itemMenu?.dismiss()
    }

    fun handleMenuItem(itemId: Int): Boolean {
        var isHandled = false
        when (itemId) {
            R.id.action_create_section -> {
                ApplistLog.getInstance().analytics(ApplistLog.CREATE_SECTION, ApplistLog.OPTIONS_MENU)
                createSection(null)
                isHandled = true
            }
        }
        return isHandled
    }

    fun getItemMenuTarget() = itemMenuTarget!!

    override fun onStartableLongTapped(startableItem: StartableItem) {
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
        if (isApp) {
            val appItem = startableItem as AppItem
            if (settingsUtils.showBadge) {
                val badgeCount = badgeStore.getBadgeCount(
                        appItem.packageName,
                        appItem.className)
                if (badgeCount > 0) {
                    itemMenuItems.add(createActionMenuItem(
                            resources.getString(R.string.app_item_menu_clear_badge), ItemMenuAction.ClearBadge))
                }
            }
        }
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_information), ItemMenuAction.AppInfo))
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_move_to_section), ItemMenuAction.MoveAppToSection))
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.app_item_menu_rename), ItemMenuAction.RenameApp))
        //
        // Icon pack support is experimental.
        // Enable it only when it's fully implemented.
        // TODO:
        // * Find relevant icons for the selected app from the icon pack.
        //   Currently we just show a list of all the icons in the icon pack without any ordering.
        //   This way it's very difficult to find e.g. the "Email" related icons.
        //
        //itemMenuItems.add(createActionMenuItem(
        //        resources.getString(R.string.app_item_menu_change_icon), ItemMenuAction.ChangeIcon))
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

    override fun onStartableTapped(startableItem: StartableItem) {
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
                ApplistLog.getInstance().log(e)
            }

        } else if (startableItem is ShortcutItem) {
            try {
                c.startActivity(startableItem.intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
                ApplistLog.getInstance().log(e)
            }

        } else if (startableItem is AppShortcutItem) {
            val shortcutInfo = resolveAppShortcutInfo(startableItem)
            startAppShortcut(shortcutInfo)
        }
    }

    override fun onStartableTouched(startableItem: StartableItem) {}

    override fun onSectionLongTapped(sectionItem: SectionItem) {
        val c = context ?: return

        val itemMenuItems = ArrayList<ItemMenuItem>()
        itemMenuItems.add(createActionMenuItem(
                resources.getString(R.string.section_item_menu_rename), ItemMenuAction.RenameSection))
        if (sectionItem.isRemovable) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.section_item_menu_delete), ItemMenuAction.DeleteSection))
        }
        if (!settingsUtils.isKeepAppsSortedAlphabetically) {
            itemMenuItems.add(createActionMenuItem(
                    resources.getString(R.string.section_item_menu_sort_apps), ItemMenuAction.SortSection))
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
        if (adapter.isFilteredByName) {
            return
        }
        val wasSectionCollapsed = sectionItem.isCollapsed
        if (!wasSectionCollapsed) {
            ApplistLog.getInstance().analytics(ApplistLog.COLLAPSE_SECTION, ApplistLog.ITEM_MENU)
        } else {
            ApplistLog.getInstance().analytics(ApplistLog.UNCOLLAPSE_SECTION, ApplistLog.ITEM_MENU)
        }
        GlobalScope.launch {
            applistModel.setSectionCollapsed(
                    pageItem.id,
                    sectionItem.id,
                    !wasSectionCollapsed)
            handler.postDelayed(Runnable {
                if (!isAdded) {
                    return@Runnable
                }
                if (wasSectionCollapsed) {
                    val position = adapter.getItemPosition(sectionItem)
                    if (position != RecyclerView.NO_POSITION) {
                        val layoutManager = recyclerView.layoutManager as GridLayoutManager
                        val firstPosition = layoutManager.findFirstVisibleItemPosition()
                        val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                        val firstVisibleView = recyclerView.getChildAt(firstVisiblePosition - firstPosition)
                                ?: return@Runnable
                        val toY = firstVisibleView.top
                        val thisView = recyclerView.getChildAt(position - firstPosition)
                                ?: return@Runnable
                        val fromY = thisView.top
                        recyclerView.smoothScrollBy(0, fromY - toY)
                    }
                }
            }, 200)
        }
    }

    override fun onSectionTouched(sectionItem: SectionItem) {}

    private fun createNotificationMenuItem(text: String, icon: Drawable?, remoteViews: RemoteViews?, statusBarNotification: StatusBarNotification): ItemMenuItem {
        var t = text
        if (t.isEmpty() && remoteViews == null) {
            t = context!!.getString(R.string.app_item_menu_notification_without_title)
        }
        return ItemMenuItem("", t, icon, R.drawable.notification_menu_item_background, true, remoteViews, statusBarNotification)
    }

    @TargetApi(Build.VERSION_CODES.N_MR1) // ShortcutInfo
    private fun createAppShortcutMenuItem(name: String, icon: Drawable?, shortcutInfo: ShortcutInfo): ItemMenuItem {
        return ItemMenuItem(name, "", icon, 0, false, null, shortcutInfo)
    }

    private fun createActionMenuItem(name: String, action: ItemMenuAction): ItemMenuItem {
        return ItemMenuItem(name, "", null, 0, false, null, action)
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
            ApplistLog.getInstance().log(RuntimeException("Max $maxShortcutCount app shortcuts are supported!"))
        }
        val launcherApps = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        var i = 0
        while (i < shortcutInfos.size && i < maxShortcutCount) {
            val shortcutInfo = shortcutInfos[i]
            var iconDrawable: Drawable? = null
            try {
                iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0)
            } catch (e: Exception) {
                ApplistLog.getInstance().log(e)
            }

            try {
                iconDrawable = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0)
            } catch (e: Exception) {
                ApplistLog.getInstance().log(e)
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

    private fun startAppShortcut(shortcutInfo: ShortcutInfo?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return
        }
        val c = context ?: return
        if (shortcutInfo == null) {
            Toast.makeText(c, R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show()
        } else if (shortcutInfo.isEnabled) {
            val launcherApps = c.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
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

    private fun startNotification(statusBarNotification: StatusBarNotification) {
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

    private fun loadAllItems() {
        val items = mutableListOf<BaseItem>()
        applistModel.withPage(pageItem.id) {
            items.addAll(ViewModelUtils.modelToView(applistModel, it))
        }
        adapter.setItems(items)
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
                ApplistLog.getInstance().log(e)
                Toast.makeText(c, R.string.cannot_show_app_info, Toast.LENGTH_SHORT).show()
            }

        } else {
            ApplistLog.getInstance().log(RuntimeException("Package name is not available for shortcut"))
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
        GlobalScope.launch {
            applistModel.removeInstalledShortcut(shortcutItem.id)
        }
    }

    // REF: 2019_07_16_broken_drag_and_drop
    // The drag-n-drop code is broken. So, we must provide a dailog
    // based move operation.
    private fun moveApp(appItem: AppItem) {
        if (!isAttached) {
            return
        }

        val sections = applistModel.getSections(pageItem.id)
        val sectionNames = arrayListOf<String>().apply {
            sections.forEach { section -> add(section.second) }
        }
        sectionNames.add(getString(R.string.move_app_to_new_section))

        ApplistDialogs.listDialog(
                activity!!, getString(R.string.move_app_title), sectionNames) { itemIndex ->
            val moveToNewSection = (itemIndex == sectionNames.size - 1)
            if (moveToNewSection) {
                createSection(appItem)
            } else {
                val pageId = pageItem.id
                val sectionId = sections.get(itemIndex).first
                val startableId = appItem.id
                val sort = settingsUtils.isKeepAppsSortedAlphabetically
                GlobalScope.launch {
                    applistModel.moveStartableToSection(pageId, sectionId, startableId)
                    if (sort) {
                        applistModel.sortStartablesInSection(pageId, sectionId)
                    }
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
                    GlobalScope.launch {
                        applistModel.setStartableCustomName(
                                pageItem.id, startableItem.id, newAppName)
                    }
                })
    }

    private fun changeAppIcon(startableItem: StartableItem) {
        if (!isAttached) {
            return
        }
        val event = when (startableItem) {
            is AppItem -> ShowIconPickerEvent(
                    startableItem.getDisplayName(),
                    ComponentName(startableItem.packageName, startableItem.className),
                    null,
                    startableItem.customIconPath)
            is AppShortcutItem -> ShowIconPickerEvent(
                    startableItem.getDisplayName(),
                    null,
                    startableItem.iconPath,
                    startableItem.customIconPath)
            is ShortcutItem -> ShowIconPickerEvent(
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
                    GlobalScope.launch {
                        applistModel.setSectionName(
                                pageItem.id, sectionItem.id, newSectionName)
                    }
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
                    GlobalScope.launch {
                        applistModel.removeSection(pageItem.id, sectionItem.id)
                    }
                },
                {
                    // Nothing.
                })
    }

    private fun sortSection(sectionItem: SectionItem) {
        GlobalScope.launch {
            applistModel.sortStartablesInSection(pageItem.id, sectionItem.id)
        }
    }

    private fun createSection(appToMove: AppItem?) {
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
                        GlobalScope.launch {
                            val section = applistModel.addNewSection(
                                    pageItem.id, sectionName, true)
                            if (section != null && appToMove != null) {
                                applistModel.moveStartableToSection(
                                        pageItem.id, section.id, appToMove.id)
                            }
                        }
                    }
                })
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
