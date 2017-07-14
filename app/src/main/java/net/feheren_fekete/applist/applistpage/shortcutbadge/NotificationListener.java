package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import net.feheren_fekete.applist.applistpage.model.BadgeStore;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = NotificationListener.class.getSimpleName();

    // TODO: Inject
    private BadgeStore mBadgeStore = BadgeStore.getInstance();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        setBadgeCount(true, sbn.getPackageName(), sbn.getNotification().number);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        setBadgeCount(true, sbn.getPackageName(), sbn.getNotification().number);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        setBadgeCount(false, sbn.getPackageName(), sbn.getNotification().number);
    }

    private void setBadgeCount(boolean notificationPosted, String packageName, int badgeCount) {
        if (notificationPosted) {
            mBadgeStore.setBadgeCount(
                    packageName, "",
                    badgeCount > 0 ? badgeCount : BadgeUtils.NOT_NUMBERED_BADGE_COUNT);
        } else {
            mBadgeStore.setBadgeCount(packageName, "", 0);
        }
    }

}
