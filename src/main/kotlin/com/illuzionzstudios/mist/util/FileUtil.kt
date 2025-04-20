package com.illuzionzstudios.mist.util

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile

/**
 * Utils to help with files
 */
class FileUtil {

    companion object {

        /**
         * Return an internal resource within our plugin's jar file
         *
         * @return the resource input stream, or null if not found
         */
        fun getInternalResource(path: String?): InputStream? {
            // First attempt
            var `is` = SpigotPlugin.instance!!.javaClass.getResourceAsStream(path)

            // Try using Bukkit
            if (`is` == null) `is` = SpigotPlugin.instance!!.getResource(path!!)

            // The hard way - go in the jar file
            if (`is` == null) try {
                JarFile(SpigotPlugin.source).use { jarFile ->
                    val jarEntry = jarFile.getJarEntry(path)
                    if (jarEntry != null) `is` = jarFile.getInputStream(jarEntry)
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            return `is`
        }

        /**
         * Recursively delete a directory and all contents
         *
         * @param dir File path to directory
         */
        fun purgeDirectory(dir: File) {
            Valid.checkNotNull(dir, "Cannot purge a null directory")
            // If not a directory just delete the file
            if (!dir.isDirectory) dir.delete()
            for (file in dir.listFiles()) {
                if (file.isDirectory) purgeDirectory(file)
                file.delete()
            }
        }
    }
}