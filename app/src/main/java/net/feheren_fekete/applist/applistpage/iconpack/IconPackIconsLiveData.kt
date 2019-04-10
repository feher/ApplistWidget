package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData

class IconPackIconsLiveData(private val packageManager: PackageManager,
                            private val iconPackHelper: IconPackHelper,
                            private val iconPackPackageName: String): LiveData<List<ComponentName>>() {

    override fun onActive() {
        super.onActive()
        value = queryIconPackIcons()
    }

    private fun queryIconPackIcons(): List<ComponentName> {
        return iconPackHelper.getSupportedApps(packageManager, iconPackPackageName)
    }

}
