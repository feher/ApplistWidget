package net.feheren_fekete.applist.applistpage.iconpack.repository

import android.content.ComponentName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import net.feheren_fekete.applist.applistpage.iconpack.IconPackHelper
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon
import org.apache.commons.text.similarity.JaroWinklerDistance
import kotlin.math.max
import kotlin.math.min

class IconPackIconsRepository(
    private val iconPacksStorage: IconPacksStorage,
    private val iconPackHelper: IconPackHelper
) {

    private val stringDistance = JaroWinklerDistance()

    public suspend fun iconPackIcons(
        iconPackPackageName: String,
        appName: String,
        appComponentName: ComponentName
    ): Flow<IconPackIcon> {
        var icons = getIconsFromStorage(iconPackPackageName)
        if (icons.isEmpty()) {
            icons = getIconsFromPackage(iconPackPackageName)
            if (icons.isNotEmpty()) {
                iconPacksStorage.storeIcons(iconPackPackageName, icons)
            }
        }
        return flow {
            icons.forEach {
                val drawableName = it.drawableName
                val componentNames = it.componentNames
                emit(
                    IconPackIcon(
                        calculateIconRanking(
                            drawableName,
                            componentNames,
                            appName,
                            appComponentName.packageName
                        ),
                        drawableName,
                        componentNames
                    )
                )
            }
        }
    }

    private fun getIconsFromStorage(iconPackPackageName: String) =
        iconPacksStorage.getIcons(iconPackPackageName)

    private suspend fun getIconsFromPackage(iconPackPackageName: String): List<IconPackIcon> {
        val icons = HashMap<String, MutableList<ComponentName>>()
        iconPackHelper.getSupportedApps(iconPackPackageName).collect {
            val components = icons[it.drawableName]
            if (components == null) {
                icons[it.drawableName] = arrayListOf(it.componentName)
            } else {
                components.add(it.componentName)
            }
        }
        iconPackHelper.getIconDrawableNames(iconPackPackageName).collect {
            if (!icons.containsKey(it)) {
                icons[it] = arrayListOf()
            }
        }
        return icons.map {
            val drawableName = it.key
            val componentNames = it.value
            IconPackIcon(
                0,
                drawableName,
                componentNames
            )
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
