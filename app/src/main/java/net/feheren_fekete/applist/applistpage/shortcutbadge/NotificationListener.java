package net.feheren_fekete.applist.applistpage.shortcutbadge;

import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.settings.SettingsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationListener extends NotificationListenerService {

    public static final String ACTION_CANCEL_NOTIFICATION = NotificationListener.class.getSimpleName() + ".ACTION_CANCEL_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_KEY = NotificationListener.class.getSimpleName() + ".EXTRA_NOTIFICATION_KEY";

    private static final String TAG = NotificationListener.class.getSimpleName();

    // TODO: Inject
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();
    private BadgeStore mBadgeStore = BadgeStore.getInstance();

    private boolean mIsConnected;
    private static List<StatusBarNotification> sStatusBarNotifications;

    public static List<StatusBarNotification> getNotificationsForPackage(String packageName) {
        List<StatusBarNotification> result = new ArrayList<>();
        if (sStatusBarNotifications != null) {
            for (StatusBarNotification sbn : sStatusBarNotifications) {
                if (sbn.getPackageName().equals(packageName)) {
                    result.add(sbn);
                }
            }
        }
        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int result = super.onStartCommand(intent, flags, startId);
        if (mIsConnected) {
            if (ACTION_CANCEL_NOTIFICATION.equals(intent.getAction())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cancelNotification(intent.getStringExtra(EXTRA_NOTIFICATION_KEY));
                }
            }
        }
        return result;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        mIsConnected = true;
        updateStatusBarNotifications();
        updateBadgeCounts();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        mIsConnected = false;
        sStatusBarNotifications = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (mIsConnected) {
            updateStatusBarNotifications();
            updateBadgeCounts();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        if (mIsConnected) {
            updateStatusBarNotifications();
            updateBadgeCounts();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        if (mIsConnected) {
            setBadgeCount(false, sbn.getPackageName(), sbn.getNotification().number);
            updateStatusBarNotifications();
            updateBadgeCounts();
        }
    }

    private void updateStatusBarNotifications() {
        StatusBarNotification[] statusBarNotifications = null;
        try {
            statusBarNotifications = getActiveNotifications();
        } catch (RuntimeException e) {
            ApplistLog.getInstance().log(e);
            sStatusBarNotifications = null;
            return;
        }
        if (statusBarNotifications != null) {
            List<StatusBarNotification> statusBarNotificationList = Arrays.asList(statusBarNotifications);
            Collections.sort(statusBarNotificationList, new Comparator<StatusBarNotification>() {
                @Override
                public int compare(StatusBarNotification a, StatusBarNotification b) {
                    return Long.compare(a.getPostTime(), b.getPostTime());
                }
            });
            sStatusBarNotifications = statusBarNotificationList;
        } else {
            sStatusBarNotifications = null;
        }
    }

    private void updateBadgeCounts() {
        for (StatusBarNotification sbn : sStatusBarNotifications) {
            setBadgeCount(true, sbn.getPackageName(), sbn.getNotification().number);
        }
    }

    private void setBadgeCount(boolean notificationPosted, String packageName, int badgeCount) {
        if (!mSettingsUtils.getShowNotificationBadge()) {
            return;
        }

        if (notificationPosted) {
            mBadgeStore.setBadgeCount(
                    packageName, "",
                    badgeCount > 0 ? badgeCount : BadgeUtils.NOT_NUMBERED_BADGE_COUNT);
        } else {
            mBadgeStore.setBadgeCount(packageName, "", 0);
        }
    }

}
