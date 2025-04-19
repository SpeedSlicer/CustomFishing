package com.illuzionzstudios.mist.config.serialization

import java.io.Serializable

/**
 * Defines an object that can be serialized into a {@param T} to be saved elsewhere
 * most likely to disk or a database.
 */
interface DataSerializable<T> : Serializable {
    /**
     * @return Object turned into a {@param T}
     */
    fun serialize(): T
}