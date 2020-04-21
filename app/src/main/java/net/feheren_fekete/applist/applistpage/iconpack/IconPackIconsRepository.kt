package net.feheren_fekete.applist.applistpage.iconpack

import android.content.ComponentName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.apache.commons.text.similarity.JaroWinklerDistance
import kotlin.math.max
import kotlin.math.min

class IconPackIconsRepository(private val iconPackHelper: IconPackHelper) {

    private val stringDistance = JaroWinklerDistance()

    public suspend fun iconPackIcons(
        iconPackPackageName: String,
        appName: String,
        appComponentName: ComponentName
    ): Flow<IconPackIcon> {
        val iconPackApps = ArrayList<IconPackApp>()
        iconPackHelper.getSupportedApps(iconPackPackageName).collect {
            iconPackApps.add(it)
        }
        val iconPackDrawables = iconPackHelper.getIconDrawableNames(iconPackPackageName)
        return iconPackDrawables.map { drawableName ->
            val drawableApps = getAppsOfDrawable(drawableName, iconPackApps)
            IconPackIcon(
                calculateIconRanking(
                    drawableName,
                    drawableApps,
                    appName,
                    appComponentName.packageName
                ),
                drawableName,
                drawableApps
            )
        }
    }

    private fun getAppsOfDrawable(
        drawableName: String,
        allApps: List<IconPackApp>
    ): List<ComponentName> {
        return allApps.filter {
            it.drawableName == drawableName
        }.map {
            it.componentName
        }
    }

    private fun calculateIconRanking(
        iconDrawableName: String,
        iconApps: List<ComponentName>,
        appName: String,
        appPackageName: String
    ): Int {
        var ranking = Int.MAX_VALUE
        iconApps.forEach {
            val r = calculateIconRanking(
                iconDrawableName,
                it.packageName,
                appName,
                appPackageName
            )
            ranking = min(ranking, r)
        }
        return ranking
    }

    private fun calculateIconRanking(
        iconDrawableName: String,
        iconPackageName: String,
        appName: String,
        appPackageName: String
    ): Int {
        val best = 1.0
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
        val md = max(max(max(d1, d2), d3), d4)
        return ((1.0 - md) * 100).toInt()
    }

}
