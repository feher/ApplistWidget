package net.feheren_fekete.applist.applistpage.iconpack

import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

class IconPackIconsLiveData(
        private val coroutineScope: CoroutineScope,
        private val iconPackHelper: IconPackHelper,
        private val packageManager: PackageManager,
        private val iconPackPackageName: String): MutableLiveData<List<String>>() {

    private var job: Job? = null

    override fun onActive() {
        super.onActive()
        queryIconPackIcons(iconPackPackageName)
    }

    override fun onInactive() {
        super.onInactive()
        runBlocking {
            job?.cancelAndJoin()
        }
    }

    private fun queryIconPackIcons(iconPackPackageName: String) {
        job = coroutineScope.launch {
            iconPackHelper.getIconDrawableNames(iconPackPackageName, this@IconPackIconsLiveData, this)
        }
    }

}
