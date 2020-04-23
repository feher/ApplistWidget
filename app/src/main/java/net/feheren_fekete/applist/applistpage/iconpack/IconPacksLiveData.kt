package net.feheren_fekete.applist.applistpage.iconpack

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo
import net.feheren_fekete.applist.applistpage.iconpack.repository.IconPacksRepository


class IconPacksLiveData(
    private val coroutineScope: CoroutineScope,
    private val iconPacksRepository: IconPacksRepository
) : LiveData<List<IconPackInfo>>() {

    override fun onActive() {
        super.onActive()
        coroutineScope.launch(Dispatchers.Default) {
            postValue(iconPacksRepository.getIconPacks())
        }
    }

}
