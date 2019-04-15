package net.feheren_fekete.applist.applistpage

import android.content.ComponentName
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MotionEventCompat
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.IconPreloadHelper.Companion.PRELOAD_ICON_SIZE
import net.feheren_fekete.applist.applistpage.model.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem
import net.feheren_fekete.applist.applistpage.viewmodel.AppShortcutItem
import net.feheren_fekete.applist.applistpage.viewmodel.ShortcutItem
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.glide.GlideApp
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.lang.ref.WeakReference

class StartableItemHolder(view: View, itemListener: ApplistAdapter.ItemListener) : ViewHolderBase(view, R.id.applist_app_item_layout) {

    private val settingsUtils: SettingsUtils by inject(SettingsUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

    private val draggedOverIndicatorLeft: View = view.findViewById(R.id.applist_app_item_dragged_over_indicator_left)
    private val draggedOverIndicatorRight: View = view.findViewById(R.id.applist_app_item_dragged_over_indicator_right)
    val appIcon: ImageView = view.findViewById(R.id.applist_app_item_icon)
    private val appNameWithoutShadow: TextView = view.findViewById(R.id.applist_app_item_app_name)
    private val appNameWithShadow: TextView = view.findViewById(R.id.applist_app_item_app_name_with_shadow)
    private val badgeCount: TextView = view.findViewById(R.id.applist_app_item_badge_count)
    private val shortcutIndicator: ImageView = view.findViewById(R.id.applist_app_item_shortcut_indicator)

    private val iconPlaceholderColors: IntArray = if (settingsUtils.isThemeTransparent)
        view.context.resources.getIntArray(R.array.icon_placeholder_colors_dark)
    else
        view.context.resources.getIntArray(R.array.icon_placeholder_colors_light);
    private var nextPlaceholderColor: Int = 0

    private var item: StartableItem? = null
    private val itemListenerRef: WeakReference<ApplistAdapter.ItemListener> = WeakReference(itemListener)

    init {
        layout.setOnClickListener {
            itemListenerRef.get()?.onStartableTapped(item)
        }
        layout.setOnLongClickListener {
            itemListenerRef.get()?.onStartableLongTapped(item)
            true
        }
        layout.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                itemListenerRef.get()?.onStartableTouched(item)
            }
            false
        }
    }

    fun bind(startableItem: StartableItem) {
        item = startableItem

        // REF: 2017_06_22_22_08_setShadowLayer_not_working
        val appName = if (settingsUtils.isThemeTransparent) {
            appNameWithShadow.visibility = View.VISIBLE
            appNameWithoutShadow.visibility = View.INVISIBLE
            appNameWithShadow
        } else {
            appNameWithoutShadow.visibility = View.VISIBLE
            appNameWithShadow.visibility = View.INVISIBLE
            appNameWithoutShadow
        }

        appName.text = startableItem.getDisplayName()

        val alpha = if (startableItem.isEnabled) 1.0f else 0.3f
        appIcon.alpha = alpha
        appName.alpha = alpha
        badgeCount.alpha = alpha

        if (startableItem.isHighlighted) {
            layout.setBackgroundResource(R.drawable.applist_startable_item_highlighted_background)
        } else {
            layout.background = null
        }

        draggedOverIndicatorLeft.visibility = if (startableItem.isDraggedOverLeft) View.VISIBLE else View.INVISIBLE
        draggedOverIndicatorRight.visibility = if (startableItem.isDraggedOverRight) View.VISIBLE else View.INVISIBLE

        if (startableItem is AppItem) {
            bindAppItemHolder(startableItem)
        } else if (startableItem is ShortcutItem || startableItem is AppShortcutItem) {
            bindShortcutItemHolder(startableItem)
        }
    }

    private fun bindAppItemHolder(item: AppItem) {
        GlideApp.with(appIcon.context)
                .load(ComponentName(item.packageName, item.className))
                .placeholder(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .error(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
                .into(appIcon)
        nextPlaceholderColor = (nextPlaceholderColor + 1) % iconPlaceholderColors.size

        if (settingsUtils.showBadge) {
            val badgeCountValue = badgeStore.getBadgeCount(item.packageName, item.className)
            if (badgeCountValue > 0) {
                badgeCount.visibility = View.VISIBLE
                if (badgeCountValue != BadgeUtils.NOT_NUMBERED_BADGE_COUNT) {
                    badgeCount.text = badgeCountValue.toString()
                } else {
                    badgeCount.text = null
                }
            } else {
                badgeCount.visibility = View.GONE
            }
        } else {
            badgeCount.visibility = View.GONE
        }

        shortcutIndicator.visibility = View.GONE
    }

    private fun bindShortcutItemHolder(item: StartableItem) {
        val iconFile = if (item is ShortcutItem) {
            File(item.iconPath)
        } else {
            val appShortcutItem = item as AppShortcutItem
            File(appShortcutItem.iconPath)
        }
        GlideApp.with(appIcon.context)
                .load(iconFile)
                .placeholder(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .error(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
                .into(appIcon)
        nextPlaceholderColor = (nextPlaceholderColor + 1) % iconPlaceholderColors.size

        badgeCount.visibility = View.GONE
        shortcutIndicator.visibility = View.VISIBLE
    }

}
