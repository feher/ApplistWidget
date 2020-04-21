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
) : MutableLiveData<Pair<Boolean, List<String>>>() {

    private var job: Job? = null

    override fun onActive() {
        super.onActive()
        Log.d("POO", "onActive")
        queryIconPackIcons(iconPackPackageName)
    }

    override fun onInactive() {
        super.onInactive()
        Log.d("POO", "onInactive")
        coroutineScope.launch {
            job?.cancelAndJoin()
        }
    }

    private fun queryIconPackIcons(iconPackPackageName: String) {
        job = coroutineScope.launch(Dispatchers.IO) {
            val iconDrawableNames = ArrayList<Pair<String, Int>>()
            var lastUpdateTime = System.currentTimeMillis()
            val f = iconPackIconsRepository.iconPackIcons(
                iconPackPackageName, appName, appComponentName)
            f.collect {
                //Log.d("POO", "GOT ONE ${it.first}")
                iconDrawableNames.add(it)
                val time = System.currentTimeMillis()
                val shouldUpdateValue = (time - lastUpdateTime) > 500
                if (shouldUpdateValue) {
                    Log.d("POO", "update")
                    lastUpdateTime = time
                    iconDrawableNames.sortBy { it.second }
                    postValue(Pair(false, iconDrawableNames.map { it.first }))
                }
            }
            if (isActive) {
                iconDrawableNames.sortBy { it.second }
                postValue(Pair(true, iconDrawableNames.map { it.first }))
            }
        }
    }

}
