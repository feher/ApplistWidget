package net.feheren_fekete.applist.applistpage

import androidx.recyclerview.widget.DiffUtil
import net.feheren_fekete.applist.applistpage.viewmodel.*

internal class ApplistItemDiffer : DiffUtil.ItemCallback<BaseItem>() {
    override fun areItemsTheSame(oldItem: BaseItem, newItem: BaseItem): Boolean {
        if (oldItem is SectionItem && newItem !is SectionItem) {
            return false
        }

        if (oldItem is StartableItem && newItem !is StartableItem) {
            return false
        }

        if (oldItem is AppItem && newItem !is AppItem) {
            return false
        }

        if (oldItem is ShortcutItem && newItem !is ShortcutItem) {
            return false
        }

        if (oldItem is AppShortcutItem && newItem !is AppShortcutItem) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BaseItem, newItem: BaseItem): Boolean {
        if (oldItem.name != newItem.name
                || oldItem.isEnabled != newItem.isEnabled
                || oldItem.isHighlighted != newItem.isHighlighted
                || oldItem.isDraggedOverLeft != newItem.isDraggedOverLeft
                || oldItem.isDraggedOverRight != newItem.isDraggedOverRight) {
            return false
        }

        if (oldItem is SectionItem && newItem is SectionItem) {
            if (oldItem.isCollapsed != newItem.isCollapsed
                    || oldItem.isRemovable != newItem.isRemovable) {
                return false
            }
        }

        if (oldItem is StartableItem && newItem is StartableItem) {
            if (oldItem.getDisplayName() != newItem.getDisplayName()) {
                return false
            }
            if (oldItem is AppItem && newItem is AppItem) {
                if (oldItem.versionCode != newItem.versionCode
                        || oldItem.badgeCount != newItem.badgeCount) {
                    return false
                }
            }
        }

        return true
    }
}
