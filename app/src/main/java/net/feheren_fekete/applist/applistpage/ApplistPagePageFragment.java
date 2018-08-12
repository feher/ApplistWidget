package net.feheren_fekete.applist.applistpage;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.Toast;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.ApplistPreferences;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuAdapter;
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuItem;
import net.feheren_fekete.applist.applistpage.itemmenu.ItemMenuListener;
import net.feheren_fekete.applist.applistpage.model.AppData;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.BadgeStore;
import net.feheren_fekete.applist.applistpage.model.PageData;
import net.feheren_fekete.applist.applistpage.model.SectionData;
import net.feheren_fekete.applist.applistpage.shortcutbadge.NotificationListener;
import net.feheren_fekete.applist.applistpage.viewmodel.AppShortcutItem;
import net.feheren_fekete.applist.applistpage.viewmodel.ShortcutItem;
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem;
import net.feheren_fekete.applist.applistpage.viewmodel.ViewModelUtils;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.settings.SettingsUtils;
import net.feheren_fekete.applist.utils.*;
import net.feheren_fekete.applist.applistpage.viewmodel.AppItem;
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem;
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ApplistPagePageFragment extends Fragment implements ApplistAdapter.ItemListener {

    private static final String TAG = ApplistPagePageFragment.class.getSimpleName();

    private final int ITEM_MENU_ITEM_APP_INFO = 1;
    private final int ITEM_MENU_ITEM_CLEAR_BADGE = 2;
    private final int ITEM_MENU_ITEM_REMOVE_SHORTCUT = 3;
    private final int ITEM_MENU_ITEM_UNINSTALL = 4;
    private final int ITEM_MENU_ITEM_SECTION_RENAME = 5;
    private final int ITEM_MENU_ITEM_SECTION_DELETE = 6;
    private final int ITEM_MENU_ITEM_SECTION_SORT_APPS = 7;

    // TODO: Inject these singletons.
    private ApplistModel mApplistModel = ApplistModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = ScreenshotUtils.getInstance();
    private SettingsUtils mSettingsUtils = SettingsUtils.getInstance();
    private ScreenUtils mScreenUtils = ScreenUtils.getInstance();
    private BadgeStore mBadgeStore = BadgeStore.getInstance();

    private Handler mHandler = new Handler();
    private ApplistPreferences mApplistPreferences;
    private RecyclerView mRecyclerView;
    private ViewGroup mTouchOverlay;
    private IconCache mIconCache;
    private ApplistAdapter mAdapter;
    private MyGridLayoutManager mLayoutManager;
    private DragGestureRecognizer mItemDragGestureRecognizer;
    private ApplistItemDragHandler mItemDragCallback;
    private @Nullable ListPopupWindow mItemMenu;
    private @Nullable BaseItem mItemMenuTarget;
    private ApplistItemDragHandler.Listener mListener;

    public static ApplistPagePageFragment newInstance(String pageName,
                                                      long launcherPageId,
                                                      IconCache iconCache,
                                                      ApplistItemDragHandler.Listener listener) {
        ApplistPagePageFragment fragment = new ApplistPagePageFragment();

        Bundle args = new Bundle();
        args.putString("pageName", pageName);
        args.putLong("launcherPageId", launcherPageId);
        fragment.setArguments(args);

        fragment.mIconCache = iconCache;
        fragment.mListener = listener;

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplistPreferences = new ApplistPreferences(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applist_page_page_fragment, container, false);

        mRecyclerView = view.findViewById(R.id.applist_page_page_fragment_recycler_view);

        // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
        final int topPadding = mScreenUtils.getStatusBarHeight(getContext()) + mScreenUtils.getActionBarHeight(getContext());
        // We add a bottom padding to the RecyclerView to "push it up" above the navigation bar.
        // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
        final int bottomPadding = mScreenUtils.hasNavigationBar(getContext()) ? mScreenUtils.getNavigationBarHeight(getContext()) : 0;
        mRecyclerView.setPadding(0, topPadding, 0, bottomPadding);

        final int columnSize = Math.round(
                mScreenUtils.dpToPx(getContext(),
                        mSettingsUtils.getColumnWidth()));
        final int screenWidth = mScreenUtils.getScreenWidth(getContext());
        final int columnCount = screenWidth / columnSize;
        mLayoutManager = new MyGridLayoutManager(getContext(), columnCount);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case ApplistAdapter.STARTABLE_ITEM_VIEW:
                        return 1;
                    case ApplistAdapter.SECTION_ITEM_VIEW:
                        return columnCount;
                    default:
                        return 1;
                }
            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ApplistAdapter(
                getContext(),
                this,
                getContext().getPackageManager(),
                new FileUtils(),
                this,
                mIconCache);
        mRecyclerView.setAdapter(mAdapter);

        loadAllItems();

        mTouchOverlay = view.findViewById(R.id.applist_page_page_fragment_touch_overlay);
        mItemDragCallback = new ApplistItemDragHandler(
                getContext(), this, mTouchOverlay, mRecyclerView, mLayoutManager, mAdapter, mListener);
        mItemDragGestureRecognizer = new DragGestureRecognizer(mItemDragCallback, mTouchOverlay, mRecyclerView);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        if (mApplistPreferences.getShowRearrangeItemsHelp()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.rearrange_items_help)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mApplistPreferences.setShowRearrangeItemsHelp(false);
                        }
                    })
                    .show();
        }
        if (mApplistPreferences.getShowUseLauncherTip()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.use_launcher_tip)
                    .setCancelable(true)
                    .setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mApplistPreferences.setShowUseLauncherTip(false);
                        }
                    })
                    .show();
        }
        mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter.isFilteredByName()) {
            deactivateNameFilter();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSectionsChangedEvent(ApplistModel.SectionsChangedEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBadgeEvent(BadgeStore.BadgeEvent event) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadAllItems();
            }
        });
    }

    private long getLauncherPageId() {
        return getArguments().getLong("launcherPageId");
    }

    public String getPageName() {
        return getArguments().getString("pageName");
    }

    public boolean isFilteredByName() {
        return mAdapter.isFilteredByName();
    }

    public void activateNameFilter() {
        if (mAdapter.isFilteredByName()) {
            return;
        }

        setNameFilter("");
    }

    public void deactivateNameFilter() {
        if (!mAdapter.isFilteredByName()) {
            return;
        }

        setNameFilter(null);
    }

    public void setNameFilter(String filterText) {
        mAdapter.setNameFilter(filterText);
        mRecyclerView.scrollToPosition(0);
    }

    public boolean isItemMenuOpen() {
        return mItemMenu != null;
    }

    public void closeItemMenu() {
        mItemMenu.dismiss();
    }

    @Nullable
    public BaseItem getItemMenuTarget() {
        return mItemMenuTarget;
    }

    public boolean handleMenuItem(int itemId) {
        boolean isHandled = false;
        switch (itemId) {
            case R.id.action_create_section:
                createSection(null);
                isHandled = true;
                break;
        }
        return isHandled;
    }

    @Override
    public void onStartableLongTapped(final StartableItem startableItem) {
        mItemMenuTarget = startableItem;

        // Change the adapter only after the popup window has been displayed.
        // Otherwise the popup window appears in a jittery way due to simultaneous change in the adapter.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.setHighlighted(mItemMenuTarget, true);
            }
        }, 200);

        mItemDragGestureRecognizer.setDelegateEnabled(false);

        final boolean isApp = (startableItem instanceof AppItem);
        final boolean isShortcut = (startableItem instanceof ShortcutItem) || (startableItem instanceof AppShortcutItem);
        final List<ItemMenuItem> itemMenuItems = new ArrayList<>();
        if (isApp) {
            addAppNotificationsToItemMenu((AppItem) startableItem, itemMenuItems);
            addAppShortcutsToItemMenu((AppItem) startableItem, itemMenuItems);
        }
        if (isApp) {
            AppItem appItem = (AppItem) startableItem;
            if (mSettingsUtils.getShowBadge()) {
                final int badgeCount = mBadgeStore.getBadgeCount(
                        appItem.getPackageName(),
                        appItem.getClassName());
                if (badgeCount > 0) {
                    itemMenuItems.add(createActionMenuItem(
                            getResources().getString(R.string.app_item_menu_clear_badge), ITEM_MENU_ITEM_CLEAR_BADGE));
                }
            }
        }
        itemMenuItems.add(createActionMenuItem(
                getResources().getString(R.string.app_item_menu_information), ITEM_MENU_ITEM_APP_INFO));
        if (isApp) {
            itemMenuItems.add(createActionMenuItem(
                    getResources().getString(R.string.app_item_menu_uninstall), ITEM_MENU_ITEM_UNINSTALL));
        }
        if (isShortcut) {
            itemMenuItems.add(createActionMenuItem(
                    getResources().getString(R.string.app_item_menu_remove_shortcut), ITEM_MENU_ITEM_REMOVE_SHORTCUT));
        }
        final ItemMenuAdapter itemMenuAdapter = new ItemMenuAdapter(getContext());
        itemMenuAdapter.setListener(mItemMenuClickListener);
        itemMenuAdapter.setItems(itemMenuItems);

        final ApplistAdapter.StartableItemHolder startableItemHolder =
                (ApplistAdapter.StartableItemHolder) mRecyclerView.findViewHolderForItemId(
                        startableItem.getId());

        mItemMenu = new ListPopupWindow(getContext());
        final boolean hasNotificationWithRemoteViews = hasNotificationWithRemoteViews(itemMenuItems);
        mItemMenu.setContentWidth(getResources().getDimensionPixelSize(
                hasNotificationWithRemoteViews ? R.dimen.item_menu_width_large : R.dimen.item_menu_width));
        mItemMenu.setHeight(ListPopupWindow.WRAP_CONTENT);
        mItemMenu.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mItemDragGestureRecognizer.setDelegateEnabled(true);
                mAdapter.setHighlighted(mItemMenuTarget, false);
                mItemMenu = null;
            }
        });
        mItemMenu.setAnchorView(startableItemHolder.layout);
        mItemMenu.setAdapter(itemMenuAdapter);
        mItemMenu.setModal(true);
        mItemMenu.show();
    }

    @Override
    public void onStartableTapped(StartableItem startableItem) {
        if (startableItem instanceof AppItem) {
            AppItem appItem = (AppItem) startableItem;

            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            ComponentName appComponentName = new ComponentName(
                    appItem.getPackageName(), appItem.getClassName());
            launchIntent.setComponent(appComponentName);

            ComponentName smsAppComponentName = AppUtils.getSmsApp(getContext());
            if (appComponentName.equals(smsAppComponentName)) {
                mBadgeStore.setBadgeCount(
                        smsAppComponentName.getPackageName(),
                        smsAppComponentName.getClassName(),
                        0);
            }
            ComponentName phoneAppComponentName = AppUtils.getPhoneApp(getContext().getApplicationContext());
            if (appComponentName.equals(phoneAppComponentName)) {
                mBadgeStore.setBadgeCount(
                        phoneAppComponentName.getPackageName(),
                        phoneAppComponentName.getClassName(),
                        0);
            }

            try {
                getContext().startActivity(launchIntent);
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.cannot_start_app, Toast.LENGTH_SHORT).show();
                ApplistLog.getInstance().log(e);
            }
        } else if (startableItem instanceof ShortcutItem) {
            ShortcutItem shortcutItem = (ShortcutItem) startableItem;
            try {
                getContext().startActivity(shortcutItem.getIntent());
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show();
                ApplistLog.getInstance().log(e);
            }
        } else if (startableItem instanceof AppShortcutItem) {
            AppShortcutItem appShortcutItem = (AppShortcutItem) startableItem;
            ShortcutInfo shortcutInfo = resolveAppShortcutInfo(appShortcutItem);
            startAppShortcut(shortcutInfo);
        }
    }

    @Override
    public void onStartableTouched(final StartableItem startableItem) {
    }

    @Override
    public void onSectionLongTapped(final SectionItem sectionItem) {
        mItemDragGestureRecognizer.setDelegateEnabled(false);

        List<ItemMenuItem> itemMenuItems = new ArrayList<>();
        itemMenuItems.add(createActionMenuItem(
                getResources().getString(R.string.section_item_menu_rename), ITEM_MENU_ITEM_SECTION_RENAME));
        if (sectionItem.isRemovable()) {
            itemMenuItems.add(createActionMenuItem(
                    getResources().getString(R.string.section_item_menu_delete), ITEM_MENU_ITEM_SECTION_DELETE));
        }
        if (!mSettingsUtils.isKeepAppsSortedAlphabetically()) {
            itemMenuItems.add(createActionMenuItem(
                    getResources().getString(R.string.section_item_menu_sort_apps), ITEM_MENU_ITEM_SECTION_SORT_APPS));
        }
        ItemMenuAdapter itemMenuAdapter = new ItemMenuAdapter(getContext());
        itemMenuAdapter.setListener(mItemMenuClickListener);
        itemMenuAdapter.setItems(itemMenuItems);

        ApplistAdapter.SectionItemHolder sectionItemHolder =
                (ApplistAdapter.SectionItemHolder) mRecyclerView.findViewHolderForItemId(
                        sectionItem.getId());

        mItemMenuTarget = sectionItem;

        mItemMenu = new ListPopupWindow(getContext());
        mItemMenu.setContentWidth(getResources().getDimensionPixelSize(R.dimen.item_menu_width));
        mItemMenu.setHeight(ListPopupWindow.WRAP_CONTENT);
        mItemMenu.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mItemDragGestureRecognizer.setDelegateEnabled(true);
                mItemMenu = null;
            }
        });
        mItemMenu.setAnchorView(sectionItemHolder.layout);
        mItemMenu.setAdapter(itemMenuAdapter);
        mItemMenu.setModal(true);
        mItemMenu.show();
    }

    @Override
    public void onSectionTapped(final SectionItem sectionItem) {
        final String pageName = getPageName();
        final boolean wasSectionCollapsed = sectionItem.isCollapsed();
        if (!mAdapter.isFilteredByName()) {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mApplistModel.setSectionCollapsed(
                            pageName,
                            sectionItem.getName(),
                            !wasSectionCollapsed);
                    return null;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (wasSectionCollapsed) {
                                int position = mAdapter.getItemPosition(sectionItem);
                                if (position != RecyclerView.NO_POSITION) {
                                    int firstPosition = mLayoutManager.findFirstVisibleItemPosition();
                                    int firstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition();
                                    View firstVisibleView = mRecyclerView.getChildAt(firstVisiblePosition - firstPosition);
                                    int toY = firstVisibleView.getTop();
                                    View thisView = mRecyclerView.getChildAt(position - firstPosition);
                                    int fromY = thisView.getTop();
                                    mRecyclerView.smoothScrollBy(0, fromY - toY);
                                }
                            }
                        }
                    }, 200);
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    @Override
    public void onSectionTouched(final SectionItem sectionItem) {
    }

    private RecyclerView.AdapterDataObserver mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            scheduleScreenshot();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
            scheduleScreenshot();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            super.onItemRangeChanged(positionStart, itemCount, payload);
            scheduleScreenshot();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            scheduleScreenshot();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            scheduleScreenshot();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
            scheduleScreenshot();
        }

        private void scheduleScreenshot() {
            mScreenshotUtils.scheduleScreenshot(getActivity(), getLauncherPageId(), ScreenshotUtils.DELAY_SHORT);
        }
    };

    private ItemMenuListener mItemMenuClickListener = new ItemMenuListener() {
        @Override
        public void onItemSelected(ItemMenuItem item) {
            if (item.data instanceof Integer) {
                final int itemId = (Integer) item.data;
                switch (itemId) {
                    case ITEM_MENU_ITEM_CLEAR_BADGE:
                        clearAppBadge((AppItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_APP_INFO:
                        showAppInfo((StartableItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_UNINSTALL:
                        uninstallApp((AppItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_REMOVE_SHORTCUT:
                        removeShortcut((StartableItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_SECTION_RENAME:
                        renameSection((SectionItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_SECTION_DELETE:
                        deleteSection((SectionItem) mItemMenuTarget);
                        break;
                    case ITEM_MENU_ITEM_SECTION_SORT_APPS:
                        sortSection((SectionItem) mItemMenuTarget);
                        break;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                    && item.data instanceof ShortcutInfo) {
                startAppShortcut((ShortcutInfo) item.data);
            } else if (item.data instanceof StatusBarNotification) {
                startNotification((StatusBarNotification) item.data);
            }
            if (mItemMenu != null) {
                mItemMenu.dismiss();
            }
        }

        @Override
        public void onItemSwiped(ItemMenuItem item) {
            cancelNotification((StatusBarNotification) item.data);
            if (mItemMenu != null) {
                mItemMenu.dismiss();
            }
        }
    };

    private ItemMenuItem createNotificationMenuItem(String text, Drawable icon, RemoteViews remoteViews, StatusBarNotification statusBarNotification) {
        if (text.isEmpty() && remoteViews == null) {
            text = getContext().getString(R.string.app_item_menu_notification_without_title);
        }
        return new ItemMenuItem("", text, icon, R.drawable.notification_menu_item_background, true, remoteViews, statusBarNotification);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1) // ShortcutInfo
    private ItemMenuItem createAppShortcutMenuItem(String name, Drawable icon, ShortcutInfo shortcutInfo) {
        return new ItemMenuItem(name, "", icon, 0, false, null, shortcutInfo);
    }

    private ItemMenuItem createActionMenuItem(String name, Integer actionId) {
        return new ItemMenuItem(name, "", null, 0, false, null, actionId);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void addAppNotificationsToItemMenu(AppItem appItem, List<ItemMenuItem> itemMenuItems) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        List<StatusBarNotification> statusBarNotifications = NotificationListener.getNotificationsForPackage(appItem.getPackageName());
        // Iterate backwards because the notifications are sorted ascending by time.
        // We want to add the newest first.
        for (int i = statusBarNotifications.size() - 1; i >= 0; --i) {
            StatusBarNotification sbn = statusBarNotifications.get(i);

            if (!shouldShowNotification(sbn, statusBarNotifications)) {
                continue;
            }

            final Notification notification = sbn.getNotification();
            final StringBuilder textBuilder = new StringBuilder();
            final Bundle extras = notification.extras;
            CharSequence extraText = extras.getCharSequence(Notification.EXTRA_TITLE, null);
            if (extraText != null) {
                textBuilder.append(extraText).append(", ");
            }
            extraText = extras.getCharSequence(Notification.EXTRA_TEXT, null);
            if (extraText != null) {
                textBuilder.append(extraText).append(", ");
            }
            extraText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT, null);
            if (extraText != null) {
                textBuilder.append(extraText).append(", ");
            }
            extraText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT, null);
            if (extraText != null) {
                textBuilder.append(extraText).append(", ");
            }
            extraText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT, null);
            if (extraText != null) {
                textBuilder.append(extraText).append(", ");
            }
            final CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null) {
                for (CharSequence textLine : textLines) {
                    textBuilder.append(textLine.toString()).append(", ");
                }
            }
            final String text = (textBuilder.length() >= 2)
                    ?  textBuilder.substring(0, textBuilder.length() - 2)
                    : textBuilder.toString();

            Icon icon = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final int iconType = notification.getBadgeIconType();
                if (iconType == Notification.BADGE_ICON_LARGE) {
                    icon = notification.getLargeIcon();
                } else if (iconType == Notification.BADGE_ICON_SMALL
                        || iconType == Notification.BADGE_ICON_NONE) {
                    icon = notification.getSmallIcon();
                }
            }
            if (icon == null) {
                icon = notification.getSmallIcon();
            }
            if (icon == null) {
                icon = notification.getLargeIcon();
            }
            Drawable iconDrawable;
            if (icon != null) {
                icon.setTint((notification.color != 0) ? notification.color : Color.GRAY);
                iconDrawable = icon.loadDrawable(getContext());
            } else {
                iconDrawable = getContext().getResources().getDrawable(R.drawable.ic_notification, null);
            }

            itemMenuItems.add(createNotificationMenuItem(text, iconDrawable, sbn.getNotification().contentView, sbn));
        }
    }

    private boolean shouldShowNotification(StatusBarNotification statusBarNotification,
                                           List<StatusBarNotification> statusBarNotifications) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true;
        }
        // In case of grouped notifications
        // * show only the summary notification or
        // * every item if there is no group summary.
        boolean isGroupSummary = false;
        boolean hasGroupSummary = false;
        if (statusBarNotification.isGroup()) {
            isGroupSummary = ((statusBarNotification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0);
            if (!isGroupSummary) {
                for (StatusBarNotification statusBarNotification2 : statusBarNotifications) {
                    if (statusBarNotification2 != statusBarNotification
                            && statusBarNotification.getGroupKey().equals(statusBarNotification2.getGroupKey())
                            && ((statusBarNotification2.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0)) {
                        hasGroupSummary = true;
                        break;
                    }
                }
            }
        }
        return isGroupSummary || !hasGroupSummary;
    }

    private boolean hasNotificationWithRemoteViews(List<ItemMenuItem> itemMenuItems) {
        for (ItemMenuItem itemMenuItem : itemMenuItems) {
            if (itemMenuItem.name.isEmpty() && itemMenuItem.text.isEmpty() && itemMenuItem.contentRemoteViews != null) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void addAppShortcutsToItemMenu(AppItem appItem, List<ItemMenuItem> itemMenuItems) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return;
        }
        if (getContext() == null) {
            return;
        }
        List<ShortcutInfo> shortcutInfos = performAppShortcutQuery(
                appItem.getPackageName(),
                null,
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
        Collections.sort(shortcutInfos, new Comparator<ShortcutInfo>() {
            @Override
            public int compare(ShortcutInfo a, ShortcutInfo b) {
                if (!a.isDynamic() && b.isDynamic()) {
                    return -1;
                }
                if (a.isDynamic() && !b.isDynamic()) {
                    return 1;
                }
                return Integer.compare(a.getRank(), b.getRank());
            }
        });
        final int maxShortcutCount = 10;
        if (shortcutInfos.size() > maxShortcutCount) {
            ApplistLog.getInstance().log(new RuntimeException("Max " + maxShortcutCount + " app shortcuts are supported!"));
        }
        LauncherApps launcherApps = (LauncherApps) getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps == null) {
            return;
        }
        for (int i = 0; i < shortcutInfos.size() && i < maxShortcutCount; ++i) {
            ShortcutInfo shortcutInfo = shortcutInfos.get(i);
            Drawable iconDrawable = null;
            try {
                iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
            } catch (Exception e) {
                ApplistLog.getInstance().log(e);
            }
            try {
                iconDrawable = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0);
            } catch (Exception e) {
                ApplistLog.getInstance().log(e);
            }
            if (iconDrawable == null) {
                iconDrawable = getResources().getDrawable(R.drawable.app_shortcut_default, null);
            }
            itemMenuItems.add(createAppShortcutMenuItem(
                    shortcutInfo.getShortLabel().toString(),
                    iconDrawable,
                    shortcutInfo));
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private List<ShortcutInfo> performAppShortcutQuery(String packageName,
                                                       @Nullable String shortcutId,
                                                       int queryFlags) {
        List<ShortcutInfo> result = new ArrayList<>();
        LauncherApps launcherApps = (LauncherApps) getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps.hasShortcutHostPermission()) {
            List<UserHandle> profiles = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                profiles.addAll(launcherApps.getProfiles());
            } else {
                profiles.add(android.os.Process.myUserHandle());
            }
            for (UserHandle userHandle : profiles) {
                LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
                shortcutQuery.setPackage(packageName);
                shortcutQuery.setQueryFlags(queryFlags);
                if (shortcutId != null) {
                    shortcutQuery.setShortcutIds(Collections.singletonList(shortcutId));
                }
                List<ShortcutInfo> shortcutInfos = launcherApps.getShortcuts(shortcutQuery, userHandle);
                if (shortcutInfos != null) {
                    result.addAll(shortcutInfos);
                }
            }
        }
        return result;
    }

    // This is used only for testing pinning of app shortcuts.
    @TargetApi(Build.VERSION_CODES.O)
    private void testPinShortcut(ShortcutInfo shortcutInfo) {
        Log.d(TAG, "REQUEST PIN " + shortcutInfo.getPackage() + " " + shortcutInfo.getId());
        ShortcutManager mShortcutManager;
        mShortcutManager = getContext().getSystemService(ShortcutManager.class);
        if (mShortcutManager.isRequestPinShortcutSupported()) {
            mShortcutManager.requestPinShortcut(shortcutInfo, null);
        }
    }

    private void startAppShortcut(@Nullable ShortcutInfo shortcutInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return;
        }
        if (shortcutInfo == null) {
            Toast.makeText(getActivity(), R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show();
        } else if (shortcutInfo.isEnabled()) {
            LauncherApps launcherApps = (LauncherApps) getContext().getSystemService(Context.LAUNCHER_APPS_SERVICE);
            try {
                launcherApps.startShortcut(shortcutInfo, null, null);
            } catch (ActivityNotFoundException | IllegalStateException e) {
                Toast.makeText(getActivity(), R.string.cannot_start_shortcut, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.cannot_start_disabled_shortcut, Toast.LENGTH_SHORT).show();
        }
    }

    private void startNotification(StatusBarNotification statusBarNotification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        try {
            statusBarNotification.getNotification().contentIntent.send();
            if ((statusBarNotification.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) {
                Intent cancelNotificationIntent = new Intent(getActivity(), NotificationListener.class);
                cancelNotificationIntent.setAction(NotificationListener.ACTION_CANCEL_NOTIFICATION);
                cancelNotificationIntent.putExtra(NotificationListener.EXTRA_NOTIFICATION_KEY, statusBarNotification.getKey());
                getActivity().startService(cancelNotificationIntent);
            }
        } catch (PendingIntent.CanceledException e) {
            // Ignore.
        }
    }

    private void cancelNotification(StatusBarNotification statusBarNotification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        final boolean isOngoing = (statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0;
        if (!isOngoing) {
            Intent cancelNotificationIntent = new Intent(getActivity(), NotificationListener.class);
            cancelNotificationIntent.setAction(NotificationListener.ACTION_CANCEL_NOTIFICATION);
            cancelNotificationIntent.putExtra(NotificationListener.EXTRA_NOTIFICATION_KEY, statusBarNotification.getKey());
            getActivity().startService(cancelNotificationIntent);
        }
    }

    @Nullable
    private ShortcutInfo resolveAppShortcutInfo(AppShortcutItem appShortcutItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return null;
        }
        List<ShortcutInfo> shortcutInfos = performAppShortcutQuery(
                appShortcutItem.getPackageName(),
                appShortcutItem.getShortcutId(),
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
        return !shortcutInfos.isEmpty() ? shortcutInfos.get(0) : null;
    }

    private void loadAllItems() {
        PageData pageData = mApplistModel.getPage(getPageName());
        if (pageData == null) {
            pageData = new PageData(ApplistModel.INVALID_ID, getPageName(), new ArrayList<SectionData>());
        }
        mAdapter.setItems(ViewModelUtils.modelToView(mApplistModel, pageData));
    }

    private void clearAppBadge(AppItem appItem) {
        mBadgeStore.setBadgeCount(appItem.getPackageName(), appItem.getClassName(), 0);

        // Clear the badgecount also for the whole package.
        // This is necessary to get rid of notification badges that got stuck due to missed
        // notifications.
        mBadgeStore.setBadgeCount(appItem.getPackageName(), "", 0);
    }

    private void showAppInfo(StartableItem startableItem) {
        String packageName = null;
        if (startableItem instanceof AppItem) {
            final AppItem appItem = (AppItem) startableItem;
            packageName = appItem.getPackageName();
        } else if (startableItem instanceof ShortcutItem) {
            final ShortcutItem shortcutItem = (ShortcutItem) startableItem;
            packageName = shortcutItem.getIntent().getPackage();
            if (packageName == null) {
                ComponentName componentName = shortcutItem.getIntent().getComponent();
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                }
            }
        } else if (startableItem instanceof AppShortcutItem) {
            final AppShortcutItem appShortcutItem = (AppShortcutItem) startableItem;
            packageName = appShortcutItem.getPackageName();
        }
        if (packageName != null) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            getContext().startActivity(intent);
        } else {
            throw new RuntimeException("Package name is not available for shortcut");
        }
    }

    private void uninstallApp(AppItem appItem) {
        if (getContext() == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        Uri uri = Uri.fromParts("package", appItem.getPackageName(), null);
        intent.setData(uri);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, false);
        getContext().startActivity(intent);
    }

    private void removeShortcut(final StartableItem shortcutItem) {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mApplistModel.removeInstalledShortcut(shortcutItem.getId());
                return null;
            }
        });
    }

    private void renameSection(SectionItem sectionItem) {
        final String pageName = getPageName();
        final String oldSectionName = sectionItem.getName();
        final List<String> sectionNames = mAdapter.getSectionNames();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, oldSectionName,
                new RunnableWithRetArg<String, String>() {
                     @Override
                    public String run(String sectionName) {
                        if (sectionNames.contains(sectionName)) {
                            return getResources().getString(R.string.dialog_error_section_exists);
                        }
                        return null;
                    }
                },
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String newSectionName) {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mApplistModel.setSectionName(pageName, oldSectionName, newSectionName);
                                return null;
                            }
                        });
                    }
                });
    }

    private void deleteSection(SectionItem sectionItem) {
        final String sectionName = sectionItem.getName();
        final String pageName = getPageName();
        final String uncategorizedSectionName = mAdapter.getUncategorizedSectionName();
        ApplistDialogs.questionDialog(
                getActivity(),
                getResources().getString(R.string.remove_section_title),
                getResources().getString(R.string.remove_section_message, sectionName, uncategorizedSectionName),
                new Runnable() {
                    @Override
                    public void run() {
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mApplistModel.removeSection(pageName, sectionName);
                                return null;
                            }
                        });
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        // Nothing.
                    }
                });
    }

    private void sortSection(SectionItem sectionItem) {
        final String sectionName = sectionItem.getName();
        final String pageName = getPageName();
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mApplistModel.sortStartablesInSection(pageName, sectionName);
                return null;
            }
        });
    }

    private void createSection(@Nullable final AppItem appToMove) {
        final String pageName = getPageName();
        final List<String> sectionNames = mAdapter.getSectionNames();
        ApplistDialogs.textInputDialog(
                getActivity(), R.string.section_name, "",
                new RunnableWithRetArg<String, String>() {
                    @Override
                    public String run(String sectionName) {
                        if (sectionNames.contains(sectionName)) {
                            return getResources().getString(R.string.dialog_error_section_exists);
                        }
                        return null;
                    }
                },
                new RunnableWithArg<String>() {
                    @Override
                    public void run(final String sectionName) {
                        if (!sectionName.isEmpty()) {
                            Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    mApplistModel.addNewSection(pageName, sectionName, true);
                                    if (appToMove != null) {
                                        AppData appData = new AppData(appToMove);
                                        mApplistModel.moveStartableToSection(pageName, sectionName, appData);
                                    }
                                    return null;
                                }
                            });
                        }
                    }
                });
    }

}
