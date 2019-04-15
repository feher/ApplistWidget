package net.feheren_fekete.applist.utils

import android.content.Context
import android.util.Log
import net.feheren_fekete.applist.ApplistLog
import java.io.File

class FileUtils {

    fun getIconCacheDirPath(context: Context): String {
        return "${context.cacheDir}${File.separator}IconCache"
    }

    fun deleteFiles(dirPath: String, fileNamePrefix: String) {
        val dir = File(dirPath)
        val matchingFiles = dir.listFiles { _, fileName ->
            fileName.startsWith(fileNamePrefix)
        }
        matchingFiles?.let {
            for (file in it) {
                Log.d(TAG, "Deleting cached icon: " + file.absolutePath)
                file.delete()
            }
        }
    }

    fun readFile(filePath: String) = try {
        File(filePath).inputStream().readBytes().toString(Charsets.UTF_8)
    } catch (e:Exception) {
        ApplistLog.getInstance().log(e)
        ""
    }

    fun writeFile(filePath: String, content: String) {
        try {
            File(filePath).bufferedWriter().use { out ->
                out.write(content)
            }
        } catch (e: Exception) {
            ApplistLog.getInstance().log(e)
        }
    }

    companion object {
        private val TAG = FileUtils::class.java.simpleName
    }


}
