package net.feheren_fekete.applist.applistpage.iconpack.repository

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.iconpack.loader.*
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackInfo

class IconPacksRepository(
    private val packageManager: PackageManager,
    private val iconPacksCache: IconPacksCache
) {

    fun getIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableListOf<IconPackInfo>()

        iconPacks.add(IconPackLoader.createIconPackInfo(
            SepiaIconPackLoader.displayName,
            SepiaIconPackLoader.displayIconId,
            SepiaIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            GrayscaleIconPackLoader.displayName,
            GrayscaleIconPackLoader.displayIconId,
            GrayscaleIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            PosterizeIconPackLoader.displayName,
            PosterizeIconPackLoader.displayIconId,
            PosterizeIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            KuwaharaIconPackLoader.displayName,
            KuwaharaIconPackLoader.displayIconId,
            KuwaharaIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            ToonIconPackLoader.displayName,
            ToonIconPackLoader.displayIconId,
            ToonIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            PixelIconPackLoader.displayName,
            PixelIconPackLoader.displayIconId,
            PixelIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            HueIconPackLoader.displayName,
            HueIconPackLoader.displayIconId,
            HueIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            CgaIconPackLoader.displayName,
            CgaIconPackLoader.displayIconId,
            CgaIconPackLoader.name))

        iconPacks.add(IconPackLoader.createIconPackInfo(
            SketchIconPackLoader.displayName,
            SketchIconPackLoader.displayIconId,
            SketchIconPackLoader.name))

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
