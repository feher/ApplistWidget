package net.feheren_fekete.applist.applistpage.iconpack.repository

import android.content.ComponentName
import android.content.Context
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.applistpage.iconpack.model.IconPackIcon
import net.feheren_fekete.applist.utils.FileUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.RuntimeException

class IconPacksStorage(
    context: Context,
    private val fileUtils: FileUtils
) {

    private val dirPath: String =
        context.cacheDir.absolutePath + java.io.File.separator + "icon-packs"

    init {
        val dir = File(dirPath)
        if (!dir.exists()) {
            try {
                if (!dir.mkdirs()) {
                    ApplistLog.getInstance().log(RuntimeException("Cannot create dir: $dirPath"))
                }
            } catch (e: Exception) {
                ApplistLog.getInstance().log(e)
            }
        }
    }

    fun getIcons(iconPackPackageName: String): List<IconPackIcon> {
        val icons = arrayListOf<IconPackIcon>()
        val fileContent = fileUtils.readFile(createFilePath(iconPackPackageName))
        try {
            val jsonObject = JSONObject(fileContent)
            val jsonIcons = jsonObject.getJSONArray("icons")
            for (i in 0 until jsonIcons.length()) {
                val jsonIcon = jsonIcons.getJSONObject(i)
                val drawableName = jsonIcon.getString("drawable")
                val jsonApps = jsonIcon.getJSONArray("apps")
                val apps = arrayListOf<ComponentName>()
                for (j in 0 until jsonApps.length()) {
                    val componentString = jsonApps.getString(j)
                    ComponentName.unflattenFromString(componentString)?.let {
                        apps.add(it)
                    }
                }
                icons.add(
                    IconPackIcon(
                        0,
                        drawableName,
                        apps
                    )
                )
            }
        } catch (e: Exception) {
            ApplistLog.getInstance().log(e)
        }
        return icons
    }

    fun storeIcons(iconPackPackageName: String, icons: List<IconPackIcon>) {
        try {
            val jsonIcons = JSONArray()
            for (icon in icons) {
                val jsonApps = JSONArray()
                for (component in icon.componentNames) {
                    jsonApps.put(component.flattenToString())
                }
                val jsonIcon = JSONObject()
                jsonIcon.put("drawable", icon.drawableName)
                jsonIcon.put("apps", jsonApps)
                jsonIcons.put(jsonIcon)
            }
            val json = JSONObject()
            json.put("packageName", iconPackPackageName)
            json.put("icons", jsonIcons)
            fileUtils.writeFile(createFilePath(iconPackPackageName), json.toString())
        } catch (e: Exception) {
            ApplistLog.getInstance().log(e)
        }
    }

    fun removeIcons(iconPackPackageName: String) {
        File(createFilePath(iconPackPackageName)).delete()
    }

    private fun createFilePath(iconPackPackageName: String) = "$dirPath/$iconPackPackageName"

}