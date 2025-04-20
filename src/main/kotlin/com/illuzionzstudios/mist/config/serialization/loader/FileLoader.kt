package com.illuzionzstudios.mist.config.serialization.loader

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.config.serialization.DataSerializable
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import lombok.*
import java.io.File

/**
 * An interface to load certain types of files
 *
 * @param <T> Type of data object being loaded
</T> */
abstract class FileLoader<T>(
    /**
     * File location for template on disk
     */
    protected val file: File,
    var extension: String
) {
    /**
     * Name of the file without extensions
     */
    protected val name: String

    /**
     * Our data object to get properties from
     */
    var `object`: T

    /**
     * @param directory The directory from plugin folder
     * @param fileName  The file name without extension
     * @param extension File extension to use
     */
    constructor(directory: String, fileName: String, extension: String) : this(File(SpigotPlugin.instance!!.dataFolder.toString() + File.separator + directory, "$fileName.$extension"), extension)

    /**
     * Serialize a [DataSerializable] object and save to disk
     */
    fun serialize(`object`: DataSerializable<T>): Boolean {
        return serialize(`object`.serialize())
    }

    /**
     * Serialize a [T] to this file
     *
     * @param object [to serialize][T]
     */
    fun serialize(`object`: T?): Boolean {
        if (`object` != null) {
            this.`object` = `object`
        }
        return save()
    }

    /**
     * Used to save our [T] to disk
     * at [.file]
     *
     * @return If was saved successfully
     */
    abstract fun save(): Boolean

    /**
     * Load basic object to memory from disk.
     * Loaded on creation of loader
     */
    abstract fun loadObject(file: File): T

    /**
     * @param file File to load from
     */
    init {
        `object` = loadObject(file)

        // Get name without extension
        name = file.name.split(".".toRegex()).toTypedArray()[0]
    }
}