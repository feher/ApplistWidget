package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.LiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class IconPacksLiveData(private val packageManager: PackageManager): LiveData<List<IconPack>>() {

    override fun onActive() {
        super.onActive()
        GlobalScope.launch {
            postValue(queryIconPacks())
        }
    }

    private fun queryIconPacks(): List<IconPack> {
        val iconPacks = mutableListOf<IconPack>()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory("com.anddoes.launcher.THEME")

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos.sortWith(ResolveInfo.DisplayNameComparator(packageManager))

        resolveInfos.forEach {
            iconPacks.add(IconPack(
                    it.loadLabel(packageManager).toString(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name)))
        }

        return iconPacks
    }

}
