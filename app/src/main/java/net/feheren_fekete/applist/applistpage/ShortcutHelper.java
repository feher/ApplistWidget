package net.feheren_fekete.applist.applistpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.ShortcutData;
import net.feheren_fekete.applist.utils.ImageUtils;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import bolts.Task;

public class ShortcutHelper {

    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    // TODO: Inject
    private ApplistModel mApplistModel = ApplistModel.getInstance();

    private Context mContext;
    private String mPageName;

    public ShortcutHelper(Context context, String pageName) {
        mContext = context;
        mPageName = pageName;
    }

    public void registerInstallShortcutReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_INSTALL_SHORTCUT);
        mContext.registerReceiver(mInstallShortcutReceiver, intentFilter);
    }

    public void unregisterInstallShortcutReceiver() {
        mContext.unregisterReceiver(mInstallShortcutReceiver);
    }

    private BroadcastReceiver mInstallShortcutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_INSTALL_SHORTCUT.equals(action)) {
                final String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

                final String shortcutIntentString = intent.getStringExtra(Intent.EXTRA_SHORTCUT_INTENT);
                Intent shortcutIntent = null;
                try {
                    shortcutIntent = Intent.parseUri(shortcutIntentString, 0);
                } catch (URISyntaxException e) {
                    ApplistLog.getInstance().log(e);
                    return;
                }

                Bitmap shortcutIconBitmap = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                if (shortcutIconBitmap == null) {
                    final Intent.ShortcutIconResource shortcutIconResource = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (shortcutIconResource != null) {
                        PackageManager packageManager = mContext.getPackageManager();
                        Resources resources = null;
                        try {
                            resources = packageManager.getResourcesForApplication(shortcutIconResource.packageName);
                            final int drawableId = resources.getIdentifier(shortcutIconResource.resourceName, null, null);
                            final Drawable drawable = resources.getDrawable(drawableId);
                            shortcutIconBitmap = ImageUtils.drawableToBitmap(drawable);
                        } catch (PackageManager.NameNotFoundException e) {
                            ApplistLog.getInstance().log(e);
                        }
                    }
                }
                if (shortcutIconBitmap == null) {
                    ApplistLog.getInstance().log(new RuntimeException("Missing icon for shortcut: " + shortcutIntentString));
                    return;
                }

                final ShortcutData shortcutData = new ShortcutData(
                        System.currentTimeMillis(),
                        shortcutName,
                        shortcutIntent);
                final Bitmap finalShortcutIconBitmap = shortcutIconBitmap;
                Task.callInBackground(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mApplistModel.createShortcut(mPageName, shortcutData, finalShortcutIconBitmap);
                        return null;
                    }
                });
            }
        }
    };

}
