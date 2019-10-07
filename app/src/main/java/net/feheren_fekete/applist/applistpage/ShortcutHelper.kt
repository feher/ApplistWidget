package net.feheren_fekete.applist.applistpage

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import net.feheren_fekete.applist.applistpage.repository.database.ApplistItemData
import net.feheren_fekete.applist.utils.ImageUtils
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject


class ShortcutHelper {

    // This must be get() to avoid crash due to lazy-init (inject())
    // inside coroutine threads.
    // Crash is caused by creating a Handler in ApplistModel. Inside
    // threads we cannot create handles by "new Handle()".
    private val applistRepo= get(ApplistPageRepository::class.java)

    private val imageUtils: ImageUtils by inject(ImageUtils::class.java)

    private val installShortcutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_INSTALL_SHORTCUT == action) {
                ApplistLog.getInstance().analytics(ApplistLog.CREATE_LEGACY_SHORTCUT, ApplistLog.OTHER_APP)
                val shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
                val shortcutIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
                var shortcutIconBitmap: Bitmap? = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON)
                if (shortcutIconBitmap == null) {
                    val shortcutIconResource = intent.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
                    if (shortcutIconResource != null) {
                        val packageManager = context.packageManager
                        var resources: Resources? = null
                        try {
                            resources = packageManager.getResourcesForApplication(shortcutIconResource.packageName)
                            val drawableId = resources!!.getIdentifier(shortcutIconResource.resourceName, null, null)
                            val drawable = resources.getDrawable(drawableId)
                            shortcutIconBitmap = imageUtils.drawableToBitmap(drawable)
                        } catch (e: PackageManager.NameNotFoundException) {
                            ApplistLog.getInstance().log(e)
                        } catch (e: NullPointerException) {
                            ApplistLog.getInstance().log(e)
                        }

                    }
                }
                if (shortcutIconBitmap == null) {
                    ApplistLog.getInstance().log(RuntimeException("Missing icon for shortcut: " + shortcutIntent.toUri(0)))
                    shortcutIconBitmap = imageUtils.shortcutPlaceholder()
                }

                val packageName = shortcutIntent.getPackage()
                        ?: shortcutIntent.component?.packageName
                if (packageName == null) {
                    ApplistLog.getInstance().log(RuntimeException("Missing package for shortcut: " + shortcutIntent.toUri(0)))
                    return
                }

                val shortcutData = ApplistItemData.createShortcut(
                        System.currentTimeMillis(),
                        packageName,
                        shortcutName,
                        "",
                        shortcutIntent)
                GlobalScope.launch {
                    applistRepo.addShortcut(shortcutData, shortcutIconBitmap!!)
                }
            }
        }
    }

    fun registerInstallShortcutReceiver(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_INSTALL_SHORTCUT)
        context.registerReceiver(installShortcutReceiver, intentFilter)
    }

    fun unregisterInstallShortcutReceiver(context: Context) {
        context.unregisterReceiver(installShortcutReceiver)
    }

    fun handleIntent(context: Context, intent: Intent): Boolean {
        val action = intent.action
        if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT == action) {
            ApplistLog.getInstance().analytics(ApplistLog.CREATE_PINNED_APP_SHORTCUT, ApplistLog.OTHER_APP)
            handleShortcutRequest(context, intent)
            return true
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun handleShortcutRequest(context: Context, intent: Intent) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (!launcherApps.hasShortcutHostPermission()) {
            return
        }

        val pinItemRequest = intent.getParcelableExtra<LauncherApps.PinItemRequest>(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
        val shortcutInfo = pinItemRequest.shortcutInfo
        Log.d(TAG, "PINNING " + shortcutInfo!!.getPackage() + " " + shortcutInfo.id)

        if (!shortcutInfo.isEnabled) {
            Toast.makeText(context, R.string.cannot_pin_disabled_shortcut, Toast.LENGTH_SHORT).show()
            return
        }

        val packageName = shortcutInfo.getPackage()
        val shortcutId = shortcutInfo.id

        // BUG: Framework or app?
        //
        // We receive the LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT intent twice.
        // First in MainActivity.onNewINtent() and then in MainActivity.onCreate(). Why is this?
        // Then we crash on the second call to LauncherApps.PinItemRequest.accept(). We are not allowed
        // to call it twice.
        //
        // Workaround: Use ApplistModel.hasAppShortcut() to check if we have already pinned
        // this shortcut (i.e. called accept() on it).
        //
        GlobalScope.launch(Dispatchers.Main) {
            val isShortcutInstalled = async(Dispatchers.IO) {
                applistRepo.hasAppShortcut(packageName, shortcutId)
            }.await()

            if (isShortcutInstalled) {
                Toast.makeText(context, R.string.cannot_pin_pinned_shortcut, Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                if (!pinItemRequest.accept()) {
                    return@launch
                }
            } catch (e: Exception) {
                Toast.makeText(context, R.string.cannot_pin, Toast.LENGTH_SHORT).show()
                ApplistLog.getInstance().log(e)
                return@launch
            }

            val shortcutName = shortcutInfo.shortLabel?.toString()
                    ?: shortcutInfo.longLabel?.toString()
            if (shortcutName == null) {
                Toast.makeText(context, R.string.cannot_pin, Toast.LENGTH_SHORT).show()
                ApplistLog.getInstance().log(RuntimeException("Shortcut has no label"))
                return@launch
            }

            val iconDrawable: Drawable? = launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0)
            val shortcutIconBitmap = if (iconDrawable != null) {
                imageUtils.drawableToBitmap(iconDrawable)
            } else {
                imageUtils.shortcutPlaceholder()
            }

            val shortcutData = ApplistItemData.createAppShortcut(
                    System.currentTimeMillis(),
                    shortcutName,
                    "",
                    packageName,
                    shortcutId)
            launch(Dispatchers.IO) {
                applistRepo.addShortcut(shortcutData, shortcutIconBitmap)
            }
        }
    }

    companion object {
        private val TAG = ShortcutHelper::class.java.simpleName
        private const val ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT"
    }

}
