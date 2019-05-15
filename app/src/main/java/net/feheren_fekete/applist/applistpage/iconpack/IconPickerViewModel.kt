package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.applistpage.model.AppData
import net.feheren_fekete.applist.applistpage.model.ApplistModel
import org.koin.core.KoinComponent
import org.koin.core.inject

class IconPickerViewModel: ViewModel(), KoinComponent {

    private val applistModel: ApplistModel by inject()
    private val iconPackHelper: IconPackHelper by inject()
    private val packageManager: PackageManager by inject()

    val iconPacks = IconPacksLiveData(packageManager)

    fun icons(iconpackPackageName: String) = IconPackIconsLiveData(iconPackHelper, packageManager, iconpackPackageName)

    fun resetOriginalIcon(customIconPath: String) {
        GlobalScope.launch(Dispatchers.IO) {
            applistModel.deleteCustomStartableIcon(customIconPath)
        }
    }

    fun setAppIcon(iconPackPackageName: String,
                   iconDrawableName: String,
                   customIconPath: String) {
        val iconBitmap = iconPackHelper.loadIcon(
                packageManager,
                iconPackPackageName,
                iconDrawableName)

        GlobalScope.launch(Dispatchers.IO) {
            applistModel.storeCustomStartableIcon(customIconPath, iconBitmap, true)
        }
    }

    fun applyIconPack(iconPackPackageName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            applistModel.transaction(true) {
                applistModel.removeAllIcons(false)
                applistModel.forEachInstalledStartable {
                    if (it is AppData) {
                        val iconBitmap = iconPackHelper.loadIcon(
                                packageManager,
                                iconPackPackageName,
                                ComponentName(it.packageName, it.className))
                        if (iconBitmap != null) {
                            val iconPath = applistModel.getCustomAppIconPath(it)
                            applistModel.storeCustomStartableIcon(iconPath, iconBitmap, false)
                        }
                    }
                }
            }
            // on app install -> create
            // on shortcut pin -> create
        }
    }


}
