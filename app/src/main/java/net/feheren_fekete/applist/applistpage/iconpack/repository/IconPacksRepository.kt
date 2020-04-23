package net.feheren_fekete.applist.applistpage.iconpack.repository

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo

class IconPacksRepository(
    private val packageManager: PackageManager,
    private val iconPacksCache: IconPacksCache
) {

    fun getIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory("com.anddoes.launcher.THEME")

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos.sortWith(ResolveInfo.DisplayNameComparator(packageManager))

        resolveInfos.forEach {
            iconPacks.add(
                IconPackInfo(
                    it.loadLabel(packageManager).toString(),
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                )
            )
        }

        iconPacksCache.cleanupMissing(iconPacks)

        return iconPacks
    }

}
