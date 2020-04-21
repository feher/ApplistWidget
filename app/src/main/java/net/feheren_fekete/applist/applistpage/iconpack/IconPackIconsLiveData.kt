package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class IconPackIconsLiveData(
    private val coroutineScope: CoroutineScope,
    private val iconPackIconsRepository: IconPackIconsRepository,
    private val packageManager: PackageManager,
    private val iconPackPackageName: String,
    private val appName: String,
    private val appComponentName: ComponentName
) : MutableLiveData<Pair<Boolean, List<IconPackIcon>>>() {

    private var job: Job? = null

    override fun onActive() {
        super.onActive()
        queryIconPackIcons(iconPackPackageName)
    }

    override fun onInactive() {
        super.onInactive()
        coroutineScope.launch {
            job?.cancelAndJoin()
        }
    }

    private fun queryIconPackIcons(iconPackPackageName: String) {
        job = coroutineScope.launch(Dispatchers.IO) {
            val icons = ArrayList<IconPackIcon>()
            var lastUpdateTime = System.currentTimeMillis()
            val f = iconPackIconsRepository.iconPackIcons(
                iconPackPackageName, appName, appComponentName)
            f.collect {
                icons.add(it)
                val time = System.currentTimeMillis()
                val shouldUpdateValue = (time - lastUpdateTime) > 500
                if (shouldUpdateValue) {
                    lastUpdateTime = time
                    icons.sortBy { it.rank }
                    postValue(Pair(false, icons))
                }
            }
            if (isActive) {
                icons.sortBy { it.rank }
                postValue(Pair(true, icons))
            }
        }
    }

}
