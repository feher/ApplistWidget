package net.feheren_fekete.applist;

import static org.koin.java.KoinJavaComponent.get;

public interface ApplistLog {

    public static ApplistLog getInstance() {
        return get(ApplistLog.class);
    }

    void log(String message, Throwable exception);

    void log(Throwable exception);

    void d(String tag, String message);

    void analytics(String event, String origin);

    public static String SHOW_DONUTS = "ShowDonuts";
    public static String SHOW_SETTINGS = "ShowSettings";
    public static String SHOW_PAGE_EDITOR = "ShowPageEditor";
    public static String CHANGE_WALLPAPER = "ChangeWallpaper";
    public static String START_APP_SEARCH = "StartAppSearch";
    public static String COLLAPSE_SECTION = "CollapseSection";
    public static String UNCOLLAPSE_SECTION = "UncollapseSection";
    public static String START_APP_SHORTCUT = "StartAppShortcut";
    public static String START_NOTIFICATION = "StartNotification";
    public static String CANCEL_NOTIFICATION = "CancelNotification";
    public static String CLEAR_APP_BADGE = "ClearAppBadge";
    public static String SHOW_APP_INFO = "ShowAppInfo";
    public static String UNINSTALL_APP = "UninstallApp";
    public static String REMOVE_SHORTCUT = "RemoveShortcut";
    public static String RENAME_APP = "RenameApp";
    public static String MOVE_APP_TO_SECTION = "MoveAppToSection";
    public static String CHANGE_APP_ICON = "ChangeAppIcon";
    public static String SET_ALL_APP_ICONS = "SetAllAppIcons";
    public static String SET_APP_ICON = "SetAppIcon";
    public static String RESET_ALL_APP_ICONS = "ResetAllAppIcons";
    public static String RESET_APP_ICON = "ResetAppIcon";
    public static String SEARCH_APP_ICON = "SearchAppIcon";
    public static String RENAME_SECTION = "RenameSection";
    public static String DELETE_SECTION = "DeleteSection";
    public static String SORT_SECTION = "SortSection";
    public static String CLEAR_SELECTION = "ClearSelection";
    public static String CREATE_SECTION = "CreateSection";
    public static String REORDER_ITEMS = "ReorderItems";
    public static String REORDER_SECTIONS = "ReorderSections";
    public static String START_DRAG_APP = "StartDragApp";
    public static String START_DRAG_SECTION = "StartDragSection";
    public static String ADD_PAGE = "AddPage";
    public static String MOVE_PAGE = "MovePage";
    public static String DELETE_PAGE = "DeletePage";
    public static String SET_HOME_PAGE = "SetHomePage";
    public static String SHOW_WIDGET_PICKER = "AddWidget";
    public static String MOVE_WIDGET_TO_PAGE = "MoveWidgetToPage";
    public static String RAISE_WIDGET = "RaiseWidget";
    public static String ADJUST_WIDGET = "AdjustWidget";
    public static String DELETE_WIDGET = "DeleteWidget";
    public static String CREATE_LEGACY_SHORTCUT = "CreateLegacyShortcut";
    public static String CREATE_PINNED_APP_SHORTCUT = "CreatePinnedAppShortcut";
    public static String IAP_PURCHASE_COMPLETED = "IapPurchaseCompleted";
    public static String IAP_PURCHASE_CANCELLED = "IapPurchaseCancelled";

    public static String SETTINGS_KEEP_APPS_SORTED_ON = "SKeepAppsSortedOn";
    public static String SETTINGS_KEEP_APPS_SORTED_OFF = "SKeepAppsSortedOff";
    public static String SETTINGS_COLOR_THEME = "SColorTheme";
    public static String SETTINGS_COLUMN_WIDTH = "SColumnWidth";
    public static String SETTINGS_SHOW_PHONE_BADGE_ON = "SShowPhoneBadgeOn";
    public static String SETTINGS_SHOW_PHONE_BADGE_OFF = "SShowPhoneBadgeOff";
    public static String SETTINGS_SHOW_NOTIFICATION_BADGE_ON = "SShowNotificationBadgeOn";
    public static String SETTINGS_SHOW_NOTIFICATION_BADGE_OFF = "SShowNotificationBadgeOff";
    public static String SETTINGS_SHOW_NEW_CONTENT_BADGE_ON = "SShowNewContentBadgeOn";
    public static String SETTINGS_SHOW_NEW_CONTENT_BADGE_OFF = "SShowNewContentBadgeOff";

    public static String MIGRATE_LAUNCHER_PAGES = "MigrateLauncherPages";
    public static String MIGRATE_APPLIST = "MigrateApplist";

    public static String OTHER_APP = "OtherApp";
    public static String OPTIONS_MENU = "OptionsMenu";
    public static String APPLIST = "Applist";
    public static String SETTINGS = "Settings";
    public static String ITEM_MENU = "ItemMenu";
    public static String ACTION_BUTTONS = "ActionButtons";
    public static String TOOLBAR = "Toolbar";
    public static String PAGE_EDITOR = "PageEditor";
    public static String WIDGET_MENU = "WidgetMenu";
    public static String ICON_PACK_PICKER = "IconPackPicker";
    public static String WIDGET_PAGE_MENU = "WidgetPageMenu";
    public static String LAUNCHER_PAGE_REPOSITORY = "LauncherPageRepository";
    public static String APPLIST_PAGE_REPOSITORY = "ApplistPageRepository";
    public static String IAP_REPOSITORY = "IapRespository";


}
