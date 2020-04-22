package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import org.koin.core.KoinComponent
import org.koin.core.inject

class IconPickerViewModel : ViewModel(), KoinComponent {

    private val applistLog: ApplistLog by inject()
    private val applistRepo: ApplistPageRepository by inject()
    private val iconPackHelper: IconPackHelper by inject()
    private val iconPackIconsRepository: IconPackIconsRepository by inject()
    private val packageManager: PackageManager by inject()

    val iconPacks = IconPacksLiveData(viewModelScope, packageManager)

    fun icons(
        iconPackPackageName: String,
        appName: String,
        appComponentName: ComponentName
    ) =
        IconPackIconsLiveData(
            viewModelScope,
            iconPackIconsRepository,
            packageManager,
            iconPackPackageName,
            appName,
            appComponentName
        )

    fun resetOriginalIcon(applistItemId: Long, customIconPath: String) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.deleteCustomStartableIcon(applistItemId, customIconPath)
        }
    }

    fun setAppIcon(
        applistItemId: Long,
        iconPackPackageName: String,
        iconDrawableName: String,
        customIconPath: String
    ) {
        val iconBitmap = iconPackHelper.loadIcon(iconPackPackageName, iconDrawableName)
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.storeCustomStartableIcon(applistItemId, customIconPath, iconBitmap!!, true)
        }
    }

    fun applyIconPack(iconPackPackageName: String) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.transaction {
                applistRepo.removeCustomIcons()
                applistRepo.forEachAppItem {
                    try {
                        val iconBitmap = iconPackHelper.loadIcon(
                            iconPackPackageName,
                            ComponentName(it.packageName, it.className),
                            48, 48
                        )
                        val iconPath = applistRepo.getCustomAppIconPath(it)
                        applistRepo.storeCustomStartableIcon(it.id, iconPath, iconBitmap, false)
                    } catch (e: Exception) {
                        ApplistLog.getInstance().log(e)
                    }
                }
            }
            // TODO: on app install -> create
            // TODO: on shortcut pin -> create
        }
    }

    fun resetAllIcons() {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.removeCustomIcons()
        }
    }


}
