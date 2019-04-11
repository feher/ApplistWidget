package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import org.koin.core.KoinComponent
import org.koin.core.inject

class IconPackIconsLiveData(private val packageManager: PackageManager,
                            private val iconPackPackageName: String): LiveData<List<ComponentName>>(), KoinComponent {

    private val iconPackHelper: IconPackHelper by inject()

    override fun onActive() {
        super.onActive()
        value = queryIconPackIcons()
    }

    private fun queryIconPackIcons(): List<ComponentName> {
        return iconPackHelper.getSupportedApps(packageManager, iconPackPackageName)
    }

}
