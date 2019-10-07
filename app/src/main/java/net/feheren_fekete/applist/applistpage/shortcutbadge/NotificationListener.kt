package net.feheren_fekete.applist.applistpage.shortcutbadge

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.repository.BadgeStore
import net.feheren_fekete.applist.settings.SettingsUtils
import org.koin.android.ext.android.inject
import java.util.ArrayList
import java.util.Arrays
import kotlin.Comparator

class NotificationListener : NotificationListenerService() {

    private val settingsUtils: SettingsUtils by inject()
    private val badgeStore: BadgeStore by inject()

    private var isConnected: Boolean = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (isConnected) {
            if (ACTION_CANCEL_NOTIFICATION == intent.action) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cancelNotification(intent.getStringExtra(EXTRA_NOTIFICATION_KEY))
                }
            }
        }
        return result
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        updateStatusBarNotifications()
        updateBadgeCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        sStatusBarNotifications = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (isConnected) {
            updateStatusBarNotifications()
            updateBadgeCounts()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: NotificationListenerService.RankingMap) {
        super.onNotificationPosted(sbn, rankingMap)
        if (isConnected) {
            updateStatusBarNotifications()
            updateBadgeCounts()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (isConnected) {
            setBadgeCount(false, sbn.packageName, sbn.notification.number)
            updateStatusBarNotifications()
            updateBadgeCounts()
        }
    }

    private fun updateStatusBarNotifications() {
        val statusBarNotifications = try {
            activeNotifications
        } catch (e: RuntimeException) {
            ApplistLog.getInstance().log(e)
            null
        }

        if (statusBarNotifications != null) {
            val statusBarNotificationList = Arrays.asList(*statusBarNotifications)
            statusBarNotificationList.sortWith(Comparator { a, b ->
                java.lang.Long.compare(a.postTime, b.postTime)
            })
            sStatusBarNotifications = statusBarNotificationList
        } else {
            sStatusBarNotifications = null
        }
    }

    private fun updateBadgeCounts() {
        sStatusBarNotifications?.let {
            for (sbn in it) {
                setBadgeCount(true, sbn.packageName, sbn.notification.number)
            }
        }
    }

    private fun setBadgeCount(notificationPosted: Boolean, packageName: String, badgeCount: Int) {
        if (!settingsUtils.showNotificationBadge) {
            return
        }

        if (notificationPosted) {
            badgeStore.setBadgeCount(
                    packageName, "",
                    if (badgeCount > 0) badgeCount else BadgeUtils.NOT_NUMBERED_BADGE_COUNT)
        } else {
            badgeStore.setBadgeCount(packageName, "", 0)
        }
    }

    companion object {

        val ACTION_CANCEL_NOTIFICATION = NotificationListener::class.java.simpleName + ".ACTION_CANCEL_NOTIFICATION"
        val EXTRA_NOTIFICATION_KEY = NotificationListener::class.java.simpleName + ".EXTRA_NOTIFICATION_KEY"

        private val TAG = NotificationListener::class.java.simpleName
        private var sStatusBarNotifications: List<StatusBarNotification>? = null

        fun getNotificationsForPackage(packageName: String): List<StatusBarNotification> {
            val result = ArrayList<StatusBarNotification>()
            sStatusBarNotifications?.let {
                for (sbn in it) {
                    if (sbn.packageName == packageName) {
                        result.add(sbn)
                    }
                }
            }
            return result
        }
    }

}
