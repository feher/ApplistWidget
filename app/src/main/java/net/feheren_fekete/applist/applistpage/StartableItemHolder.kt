package net.feheren_fekete.applist.applistpage

import android.content.ComponentName
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MotionEventCompat
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.IconPreloadHelper.Companion.PRELOAD_ICON_SIZE
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.applistpage.shortcutbadge.BadgeUtils
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem
import net.feheren_fekete.applist.applistpage.viewmodel.AppShortcutItem
import net.feheren_fekete.applist.applistpage.viewmodel.ShortcutItem
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem
import net.feheren_fekete.applist.settings.SettingsUtils
import net.feheren_fekete.applist.utils.glide.AppIconSignature
import net.feheren_fekete.applist.utils.glide.FileSignature
import net.feheren_fekete.applist.utils.glide.GlideApp
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorMatrix



class StartableItemHolder(view: View, itemListener: ApplistAdapter.ItemListener) : ViewHolderBase(view, R.id.applist_app_item_layout) {

    private val applistLog: ApplistLog by inject(ApplistLog::class.java)
    private val settingsUtils: SettingsUtils by inject(SettingsUtils::class.java)
    private val badgeStore: BadgeStore by inject(BadgeStore::class.java)

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
        appIcon.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                itemListenerRef.get()?.onStartableTouched(item)
            }
            false
        }
    }

    fun bind(startableItem: StartableItem, isSelectionModeEnabled: Boolean) {
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

        if (isSelectionModeEnabled) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(colorMatrix)
            appIcon.colorFilter = filter
            if (startableItem.isSelected) {
                layout.setBackgroundResource(R.drawable.applist_startable_item_selected_border_background)
            } else {
                layout.background = null
            }
        } else {
            appIcon.colorFilter = null
        }

        if (startableItem is AppItem) {
            bindAppItemHolder(startableItem, isSelectionModeEnabled)
        } else if (startableItem is ShortcutItem || startableItem is AppShortcutItem) {
            bindShortcutItemHolder(startableItem, isSelectionModeEnabled)
        }
    }

    private fun bindAppItemHolder(item: AppItem, isSelectionModeEnabled: Boolean) {
        val iconFile = File(item.customIconPath)
        if (iconFile.exists()) {
            loadAppIcon(iconFile)
        } else {
            loadAppIcon(item.versionCode, ComponentName(item.packageName, item.className))
        }

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

    private fun bindShortcutItemHolder(item: StartableItem, isSelectionModeEnabled: Boolean) {
        val iconFile = File(item.customIconPath)
        if (iconFile.exists()) {
            loadAppIcon(iconFile)
        } else if (item is AppShortcutItem) {
            loadAppIcon(File(item.iconPath))
        } else if (item is ShortcutItem) {
            loadAppIcon(File(item.iconPath))
        } else {
            appIcon.setImageBitmap(null)
            applistLog.log(IllegalStateException())
        }

        badgeCount.visibility = View.GONE
        shortcutIndicator.visibility = View.VISIBLE
    }

    private fun loadAppIcon(iconFile: File) {
        GlideApp.with(appIcon.context)
                .load(iconFile)
                .signature(FileSignature(iconFile))
                .placeholder(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .error(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
                .into(appIcon)
        nextPlaceholderColor = (nextPlaceholderColor + 1) % iconPlaceholderColors.size
    }

    private fun loadAppIcon(appVersionCode: Long, componentName: ComponentName) {
        GlideApp.with(appIcon.context)
                .load(componentName)
                .signature(AppIconSignature(appVersionCode))
                .placeholder(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .error(ColorDrawable(iconPlaceholderColors[nextPlaceholderColor]))
                .override(PRELOAD_ICON_SIZE, PRELOAD_ICON_SIZE)
                .into(appIcon)
        nextPlaceholderColor = (nextPlaceholderColor + 1) % iconPlaceholderColors.size
    }

}
