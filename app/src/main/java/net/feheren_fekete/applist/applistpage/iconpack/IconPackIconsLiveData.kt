package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPackIconsRepository

class IconPackIconsLiveData(
    private val coroutineScope: CoroutineScope,
    private val iconPackIconsRepository: IconPackIconsRepository,
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
                iconPackPackageName, appName, appComponentName
            )
            f.collect {
                icons.add(it)
                val time = System.currentTimeMillis()
                val shouldUpdateValue = (time - lastUpdateTime) > 1000
                if (shouldUpdateValue) {
                    lastUpdateTime = time
                    icons.sortBy { it.rank }
                    //
                    // This caused ConcurrentModificationException: must pass a copy of the
                    // array to postValue here.
                    //
                    val iconsCopy = icons.map { it }
                    postValue(Pair(false, iconsCopy))
                }
            }
            if (isActive) {
                icons.sortBy { it.rank }
                postValue(Pair(true, icons))
            }
        }
    }

}
