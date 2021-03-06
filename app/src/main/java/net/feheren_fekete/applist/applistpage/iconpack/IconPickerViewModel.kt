package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPackIconsRepository
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPacksRepository
import net.feheren_fekete.applist.applistpage.repository.ApplistPageRepository
import org.koin.core.KoinComponent
import org.koin.core.get

class IconPickerViewModel : ViewModel(), KoinComponent {

    private val applistLog = get<ApplistLog>()
    private val applistRepo = get<ApplistPageRepository>()
    private val iconPackIconsRepository = get<IconPackIconsRepository>()
    private val iconPacksRepository = get<IconPacksRepository>()

    val iconPacks = IconPacksLiveData(viewModelScope, iconPacksRepository)

    fun icons(
        iconPackPackageName: String,
        appName: String,
        appComponentName: ComponentName
    ) =
        IconPackIconsLiveData(
            viewModelScope,
            iconPackIconsRepository,
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
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.setCustomStartableIcon(
                applistItemId, iconPackPackageName, iconDrawableName,
                customIconPath
            )
        }
    }

    fun applyIconPack(iconPackPackageName: String) =
        viewModelScope.async(Dispatchers.IO + NonCancellable) {
            applistRepo.updateCustomIcons(iconPackPackageName)
        }

    fun resetAllIcons() =
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            applistRepo.removeCustomIcons()
        }

}
