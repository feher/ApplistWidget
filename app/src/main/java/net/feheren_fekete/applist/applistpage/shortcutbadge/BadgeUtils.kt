package net.feheren_fekete.applist.applistpage.shortcutbadge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.SQLException
import android.net.Uri
import net.feheren_fekete.applist.ApplistLog
import java.util.*

class BadgeUtils(context: Context) {

    private val appContext: Context = context.applicationContext

    private val launcherPackageName: String
        get() {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = appContext.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return if (resolveInfo == null
                    || resolveInfo.activityInfo.name.toLowerCase().contains("resolver")) {
                ""
            } else {
                resolveInfo.activityInfo.packageName
            }
        }

    fun getBadgeCountFromLauncher(packageName: String, className: String): Int {
        val launcherPackageName = launcherPackageName
        return when {
            sSamsungLaunchers.contains(launcherPackageName) -> getSamsungBadgeCount(packageName, className)
            sNovaLaunchers.contains(launcherPackageName) -> getNovaBadgeCount(packageName, className)
            sSonyLaunchers.contains(launcherPackageName) -> getSonyBadgeCount(packageName, className)
            sHuaweiLaunchers.contains(launcherPackageName) -> getHuaweiBadgeCount(packageName, className)
            else -> INVALID_BADGE_COUNT
        }
    }

    private fun getSamsungBadgeCount(packageName: String, className: String): Int {
        var result = INVALID_BADGE_COUNT
        val contentResolver = appContext.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                    Uri.parse(SAMSUNG_PROVIDER_CONTENT_URI),
                    arrayOf(SAMSUNG_COLUMN_BADGECOUNT),
                    SAMSUNG_COLUMN_PACKAGE + " = ? "
                            + " AND " + SAMSUNG_COLUMN_CLASS + " = ?",
                    arrayOf(packageName, className), null)
            if (cursor != null) {
                if (cursor.count > 0) {
                    cursor.moveToNext()
                    val badgeCountColumnIndex = cursor.getColumnIndex(SAMSUNG_COLUMN_BADGECOUNT)
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex)
                    }
                } else {
                    result = 0
                }
            }
        } catch (e: SecurityException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } catch (e: SQLException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun getNovaBadgeCount(packageName: String, className: String): Int {
        var result = INVALID_BADGE_COUNT
        val contentResolver = appContext.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                    Uri.parse(NOVA_PROVIDER_CONTENT_URI),
                    arrayOf(NOVA_COLUMN_COUNT),
                    "$NOVA_COLUMN_TAG = ? ",
                    arrayOf("$packageName/$className"), null)
            if (cursor != null) {
                if (cursor.count > 0) {
                    cursor.moveToNext()
                    val badgeCountColumnIndex = cursor.getColumnIndex(NOVA_COLUMN_COUNT)
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex)
                    }
                } else {
                    result = 0
                }
            }
        } catch (e: SecurityException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } catch (e: SQLException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun getSonyBadgeCount(packageName: String, className: String): Int {
        var result = INVALID_BADGE_COUNT
        val contentResolver = appContext.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                    Uri.parse(SONY_PROVIDER_CONTENT_URI),
                    arrayOf(SONY_COLUMN_BADGE_COUNT),
                    SONY_COLUMN_PACKAGE_NAME + " = ? "
                            + " AND " + SONY_COLUMN_ACTIVITY_NAME + " = ?",
                    arrayOf(packageName, className), null)
            if (cursor != null) {
                if (cursor.count > 0) {
                    cursor.moveToNext()
                    val badgeCountColumnIndex = cursor.getColumnIndex(SONY_COLUMN_BADGE_COUNT)
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex)
                    }
                } else {
                    result = 0
                }
            }
        } catch (e: SecurityException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } catch (e: SQLException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun getHuaweiBadgeCount(packageName: String, className: String): Int {
        var result = INVALID_BADGE_COUNT
        val contentResolver = appContext.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                    Uri.parse(HUAWEI_PROVIDER_CONTENT_URI),
                    arrayOf(HUAWEI_COLUMN_BADGENUMBER),
                    HUAWEI_COLUMN_PACKAGE + " = ? "
                            + " AND " + HUAWEI_COLUMN_CLASS + " = ?",
                    arrayOf(packageName, className), null)
            if (cursor != null) {
                if (cursor.count > 0) {
                    cursor.moveToNext()
                    val badgeCountColumnIndex = cursor.getColumnIndex(HUAWEI_COLUMN_BADGENUMBER)
                    if (badgeCountColumnIndex != -1) {
                        result = cursor.getInt(badgeCountColumnIndex)
                    }
                } else {
                    result = 0
                }
            }
        } catch (e: SecurityException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } catch (e: SQLException) {
            ApplistLog.getInstance().log(RuntimeException("Cannot get badge count", e))
        } finally {
            cursor?.close()
        }
        return result
    }

    companion object {

        private val TAG = BadgeUtils::class.java.simpleName

        const val INVALID_BADGE_COUNT = -1
        const val NOT_NUMBERED_BADGE_COUNT = Integer.MAX_VALUE

        // Querying badges does not work for Sony.
        // We don't have permission to do so.
        private val sSonyLaunchers = Arrays.asList<String>(
                //            "com.sonyericsson.home",
                //            "com.sonymobile.home"
        )
        private const val SONY_PROVIDER_CONTENT_URI = "content://com.sonymobile.home.resourceprovider/badge"
        private const val SONY_COLUMN_PACKAGE_NAME = "package_name"
        private const val SONY_COLUMN_ACTIVITY_NAME = "activity_name"
        private const val SONY_COLUMN_BADGE_COUNT = "badge_count"

        private val sHuaweiLaunchers = Arrays.asList(
                "com.huawei.android.launcher"
        )
        private const val HUAWEI_PROVIDER_CONTENT_URI = "content://com.huawei.android.launcher.settings/badge"
        private const val HUAWEI_COLUMN_PACKAGE = "package"
        private const val HUAWEI_COLUMN_CLASS = "class"
        private const val HUAWEI_COLUMN_BADGENUMBER = "badgenumber"

        private val sSamsungLaunchers = Arrays.asList(
                "com.sec.android.app.launcher",
                "com.sec.android.app.twlauncher"
        )
        private const val SAMSUNG_PROVIDER_CONTENT_URI = "content://com.sec.badge/apps?notify=true"
        private const val SAMSUNG_COLUMN_PACKAGE = "package"
        private const val SAMSUNG_COLUMN_CLASS = "class"
        private const val SAMSUNG_COLUMN_BADGECOUNT = "badgecount"

        // Querying badges does not work for Nova (TeslaUnread).
        // We don't have permission to do so.
        private val sNovaLaunchers = Arrays.asList<String>(
                //            "com.teslacoilsw.launcher"
        )
        private const val NOVA_PROVIDER_CONTENT_URI = "content://com.teslacoilsw.notifier/unread_count"
        private const val NOVA_COLUMN_TAG = "tag"
        private const val NOVA_COLUMN_COUNT = "count"
    }

}
