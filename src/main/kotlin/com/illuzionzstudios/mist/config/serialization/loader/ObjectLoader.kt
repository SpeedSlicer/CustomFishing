package com.illuzionzstudios.mist.config.serialization.loader

import lombok.Getter

/**
 * Works by taking a base object and loading that into another
 * object. Also contains a way to implement saving the other
 * way for serialization.
 *
 * Any serializable objects should be encapsulated by this to provide
 * easy loading and saving to files
 *
 * @param <T> The type of object being loaded
 * @param <L> The type of object loading from
 */
abstract class ObjectLoader<T, L>(
    /**
     * The section to load and save from
     */
    protected var loader: L
) {
    /**
     * Our data object to get properties from
     */
    var `object`: T
        protected set

    /**
     * Used to save our updated object to the loader.
     * Updates the loader then can call [.getObject] to get
     * the updated object
     *
     * @return If was saved successfully
     */
    abstract fun save(): Boolean

    /**
     * Load basic object to memory from disk.
     * Loaded on creation of loader
     */
    abstract fun loadObject(file: L): T

    init {
        `object` = loadObject(loader)
    }
}