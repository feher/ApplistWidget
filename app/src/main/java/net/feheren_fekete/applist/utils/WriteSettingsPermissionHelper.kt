package net.feheren_fekete.applist.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.ApplistDialogs
import java.util.*

class WriteSettingsPermissionHelper(private val context: Context) {

    private val affectedModels = arrayOf("mate 20 pro")

    fun hasWriteSettingsPermission(): Boolean {
        return (!isModelAffected()
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.System.canWrite(context)))
    }

    fun requestWriteSettingsPermission(activity: Activity) {
        ApplistDialogs.messageDialog(
                activity,
                context.getString(R.string.write_settings_permission_dialog_title),
                context.getString(R.string.write_settings_permission_dialog_message),
                {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:" + context.packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    activity.finish()
                },
                {
                    activity.finish()
                })
    }

    private fun isModelAffected() =
        affectedModels.contains(Build.MODEL.toLowerCase(Locale.US))

}
