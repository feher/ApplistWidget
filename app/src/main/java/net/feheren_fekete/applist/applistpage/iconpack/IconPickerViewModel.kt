package net.feheren_fekete.applist.applistpage.iconpack

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import org.koin.core.KoinComponent
import org.koin.core.inject

class IconPickerViewModel: ViewModel(), KoinComponent {

    private val iconPackHelper: IconPackHelper by inject()
    private val packageManager: PackageManager by inject()

    val iconPacks = IconPacksLiveData(packageManager)

    fun icons(iconpackPackageName: String) = IconPackIconsLiveData(iconPackHelper, packageManager, iconpackPackageName)

}
