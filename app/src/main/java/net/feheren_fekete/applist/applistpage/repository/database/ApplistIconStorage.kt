package net.feheren_fekete.applist.applistpage.repository.database

import android.content.Context
import android.graphics.Bitmap
import net.feheren_fekete.applist.utils.ImageUtils
import java.io.File

class ApplistIconStorage(context: Context, private val mImageUtils: ImageUtils) {

    private val customAppIconsDirPath: String = context.filesDir.absolutePath + File.separator + "custom-app-icons-v2"
    private val customShortcutIconsDirPath: String = context.filesDir.absolutePath + File.separator + "custom-shortcut-icons-v2"
    private val shortcutIconsDirPath: String = context.filesDir.absolutePath + File.separator + "shortcut-icons-v2"

    fun getCustomAppIconFilePath(packageName: String, className: String): String {
        return customAppIconsDirPath + File.separator + packageName + "::" + className
    }

    fun getCustomShortcutIconFilePath(shortcutId: Long): String {
        return customShortcutIconsDirPath + File.separator + "shortcut-icon-" + shortcutId + ".png"
    }

    fun getShortcutIconFilePath(shortcutId: Long): String {
        return shortcutIconsDirPath + File.separator + "shortcut-icon-" + shortcutId + ".png"
    }

    fun storeCustomStartableIcon(iconPath: String, shortcutIcon: Bitmap) {
        mImageUtils.saveBitmap(shortcutIcon, iconPath)
    }

    fun deleteCustomStartableIcon(iconPath: String) {
        File(iconPath).delete()
    }

    fun storeShortcutIcon(shortcutId: Long, shortcutIcon: Bitmap) {
        mImageUtils.saveBitmap(
                shortcutIcon,
                getShortcutIconFilePath(shortcutId))
    }

    fun deleteShortcutIcon(shortcutId: Long) {
        File(getShortcutIconFilePath(shortcutId)).delete()
    }

}
