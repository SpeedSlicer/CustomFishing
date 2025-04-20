package com.illuzionzstudios.mist.config.serialization.loader

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import java.io.*

/**
 * Provides a way to load on object from a JSON file.
 *
 * @param <T> The object to load
</T> */
abstract class JsonFileLoader<T>(directory: String, fileName: String) : FileLoader<T>(directory, fileName, "json") {

    /**
     * The JSON object to use for loading
     */
    protected var json: JsonObject? = null

    /**
     * Save the object T into a json object to be saved to disk
     */
    abstract fun saveJson()

    /**
     * Used to save our [JsonObject] to disk
     * at [.file]
     *
     * @return If was saved successfully
     */
    override fun save(): Boolean {
        // Load new object before saving
        saveJson()
        try {
            val writer = FileWriter(file)
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            val jp = JsonParser()
            val je = jp.parse(json.toString())
            val prettyJsonString = gson.toJson(je)
            writer.write(prettyJsonString)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            Logger.displayError(e, "Could not save file to disk: " + file.name)
        }
        return true
    }

    override fun loadObject(file: File): T {
        // Try assign JSON file
        json = try {
            JsonParser().parse(FileReader(file)).asJsonObject
        } catch (e: FileNotFoundException) {
            // If couldn't load, it becomes a new object
            JsonObject()
        }
        return loadJsonObject()
    }

    /**
     * Load the object from a [JsonObject]
     *
     * @return The loaded object
     */
    abstract fun loadJsonObject(): T
}