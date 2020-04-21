package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.apache.commons.text.similarity.JaccardDistance
import org.apache.commons.text.similarity.JaroWinklerDistance
import org.apache.commons.text.similarity.LevenshteinDetailedDistance
import org.apache.commons.text.similarity.LevenshteinDistance
import kotlin.math.max
import kotlin.math.min

class IconPackIconsRepository(private val iconPackHelper: IconPackHelper) {

//    private val stringDistance = JaroWinklerDistance()
    private val stringDistance = JaccardDistance()

    public suspend fun iconPackIcons(
        iconPackPackageName: String,
        appName: String,
        appComponentName: ComponentName
    ): Flow<Pair<String, Int>> {
        val iconPackApps = ArrayList<IconPackAppItem>()
        iconPackHelper.getSupportedApps(iconPackPackageName).collect {
            iconPackApps.add(it)
        }
        val iconPackDrawables = iconPackHelper.getIconDrawableNames(iconPackPackageName)
        return iconPackDrawables.map { drawableName ->
            //                Log.d("POO", "GIVE ONE $drawableName")
            val drawableApps = getAppsOfDrawable(drawableName, iconPackApps)
            Pair(
                drawableName,
                calculateIconRanking(
                    appName,
                    appComponentName.packageName,
                    drawableName,
                    drawableApps
                )
//                    )
            )
        }
    }

    private fun getAppsOfDrawable(
        drawableName: String,
        allApps: List<IconPackAppItem>
    ): List<IconPackAppItem> {
        return allApps.filter {
            it.drawableName == drawableName
        }
    }

    private fun calculateIconRanking(
        appName: String,
        appPackageName: String,
        iconDrawableName: String,
        iconApps: List<IconPackAppItem>
    ): Int {
        var ranking = Int.MAX_VALUE
        iconApps.forEach {
            val r = calculateIconRanking(
                appName,
                appPackageName,
                iconDrawableName,
                it.componentName.packageName
            )
            ranking = min(ranking, r)
        }
        return ranking
    }

    private fun calculateIconRanking(
        appName: String,
        appPackageName: String,
        iconDrawableName: String,
        iconPackageName: String
    ): Int {
        val best = 0.0
        val d1 = stringDistance.apply(appPackageName, iconPackageName)
        if (d1 == best) {
            return 0
        }
        val d2 = stringDistance.apply(appName, iconDrawableName)
        if (d2 == best) {
            return 0
        }
        val d3 = stringDistance.apply(appName, iconPackageName)
        if (d3 == best) {
            return 0
        }
        val d4 = stringDistance.apply(appPackageName, iconDrawableName)
        if (d4 == best) {
            return 0
        }
        val md = min(min(min(d1, d2), d3), d4)
//        val md = max(max(max(d1, d2), d3), d4)
        return ((1.0 - md) * 100).toInt()

//        val md = min(min(min(d1, d2), d3), d4)
//        return md


//        if (appPackageName == iconPackageName) {
//            return 0
//        }
//
//        var ranking = 0
//        val appNameParts = appName.toLowerCase(Locale.getDefault()).split(' ')
//        val appPackageNameParts = appPackageName.toLowerCase(Locale.US).split('.')
//        val iconPackageNameParts = iconPackageName.toLowerCase(Locale.US).split('.')
//        iconPackageNameParts.forEachIndexed { j, iconPart ->
//            appNameParts.forEachIndexed { i, appPart ->
//                val i1 = i + 1
//                val j1 = j + 1
//                when {
//                    appPart == iconPart -> {
//                        ranking += (i1 * 10) * j1
//                    }
//                    iconPart.contains(appPart) -> {
//                        ranking += ((i1 * 10) * j1) * 10
//                    }
//                    appPart.contains(iconPart) -> {
//                        ranking += ((i1 * 10) * j1) * 100
//                    }
//                }
//            }
////            appPackageNameParts.forEachIndexed { i, appPkgPart ->
////                when {
////                    appPkgPart == iconPart -> {
////                        ranking += (i * 10) * j * 1000
////                    }
////                    iconPart.contains(appPkgPart) -> {
////                        ranking += ((i * 10) * j) * 10000
////                    }
////                    appPkgPart.contains(iconPart) -> {
////                        ranking += ((i * 10) * j) * 100000
////                    }
////                }
////            }
//        }
//
//        return if (ranking != 0) {
//            Log.d("POO", "RANK $ranking $iconDrawableName $iconPackageName")
//            ranking
//        } else {
//            Int.MAX_VALUE
//        }
    }

}
