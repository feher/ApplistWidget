package net.feheren_fekete.applist.applistpage;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import net.feheren_fekete.applist.ApplistLog;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.model.AppShortcutData;
import net.feheren_fekete.applist.applistpage.model.ApplistModel;
import net.feheren_fekete.applist.applistpage.model.ShortcutData;
import net.feheren_fekete.applist.utils.ImageUtils;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class ShortcutHelper {

    private static final String TAG = ShortcutHelper.class.getSimpleName();
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    // TODO: Inject
    private ApplistModel mApplistModel = ApplistModel.getInstance();

    private Context mContext;

    public ShortcutHelper(Context context) {
        mContext = context;
    }

    public void registerInstallShortcutReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_INSTALL_SHORTCUT);
        mContext.registerReceiver(mInstallShortcutReceiver, intentFilter);
    }

    public void unregisterInstallShortcutReceiver() {
        mContext.unregisterReceiver(mInstallShortcutReceiver);
    }

    public boolean handleIntent(Intent intent) {
        if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(intent.getAction())) {
            handleShortcutRequest(intent);
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void handleShortcutRequest(Intent intent) {
        final LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (!launcherApps.hasShortcutHostPermission()) {
            return;
        }

        final LauncherApps.PinItemRequest pinItemRequest = intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST);
        final ShortcutInfo shortcutInfo = pinItemRequest.getShortcutInfo();
        Log.d(TAG, "PINNING " + shortcutInfo.getPackage() + " " + shortcutInfo.getId());

        if (!shortcutInfo.isEnabled()) {
            Toast.makeText(mContext, R.string.cannot_pin_disabled_shortcut, Toast.LENGTH_SHORT).show();
            return;
        }

        final String packageName = shortcutInfo.getPackage();
        final String shortcutId = shortcutInfo.getId();

        // BUG: Framework or app?
        //
        // We receive the LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT intent twice.
        // First in MainActivity.onNewINtent() and then in MainActivity.onCreate(). Why is this?
        // Then we crash on the second call to LauncherApps.PinItemRequest.accept(). We are not allowed
        // to call it twice.
        //
        // Workaround: Use ApplistModel.hasInstalledAppShortcut() to check if we have already pinned
        // this shortcut (i.e. called accept() on it).
        //
        Task.callInBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return mApplistModel.hasInstalledAppShortcut(packageName, shortcutId);
            }
        }).continueWith(new Continuation<Boolean, Void>() {
            @Override
            public Void then(Task<Boolean> task) throws Exception {
                boolean isShortcutInstalled = task.getResult();
                if (isShortcutInstalled) {
                    Toast.makeText(mContext, R.string.cannot_pin_pinned_shortcut, Toast.LENGTH_SHORT).show();
                    return null;
                }
                if (!pinItemRequest.accept()) {
                    return null;
                }

                String shortcutName = null;
                if (shortcutInfo.getShortLabel() != null) {
                    shortcutName = shortcutInfo.getShortLabel().toString();
                } else if (shortcutInfo.getLongLabel() != null) {
                    shortcutName = shortcutInfo.getLongLabel().toString();
                } else {
                    ApplistLog.getInstance().log(new RuntimeException("Shortcut has no label"));
                    return null;
                }

                final Drawable iconDrawable = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0);
                final Bitmap shortcutIconBitmap = ImageUtils.drawableToBitmap(iconDrawable);

                final AppShortcutData shortcutData = new AppShortcutData(
                        System.currentTimeMillis(),
                        shortcutName,
                        packageName,
                        shortcutId);
                Task.callInBackground(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mApplistModel.addInstalledShortcut(shortcutData, shortcutIconBitmap);
                        return null;
                    }
                });
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private BroadcastReceiver mInstallShortcutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_INSTALL_SHORTCUT.equals(action)) {
                final String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                final Intent shortcutIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
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
                    ApplistLog.getInstance().log(new RuntimeException("Missing icon for shortcut: " + shortcutIntent.toUri(0)));
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
                        mApplistModel.addInstalledShortcut(shortcutData, finalShortcutIconBitmap);
                        return null;
                    }
                });
            }
        }
    };

}
