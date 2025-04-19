package com.illuzionzstudios.mist.config

import com.illuzionzstudios.mist.config.format.CommentStyle

/**
 * This represents a set option in a [YamlConfig]
 * It provides convenience to getting and setting this value. This way
 * we don't always have to make calls to the [YamlConfig] object
 * and can just access this. This is usually for values that we know will be in
 * the config not for custom configurations. For instance the language to use
 */
class ConfigSetting {

    /**
     * The node path (or key) this value is set at
     */
    val key: String

    /**
     * The [YamlConfig] instance this value is apart of. Can set
     * if we want to dynamically change the config file or reset it.
     */
    private var config: YamlConfig? = null

    /**
     * Default value of this setting
     */
    private var defaultValue: Any? = null

    /**
     * The comment style
     */
    private var commentStyle = CommentStyle.SIMPLE

    /**
     * Comments
     */
    private lateinit var comments: Array<out String?>

    constructor(key: String) {
        this.key = key
    }

    constructor(key: String, defaultValue: Any, vararg comment: String?) {
        this.key = key
        this.defaultValue = defaultValue
        this.comments = comment
    }

    constructor(key: String, defaultValue: Any, commentStyle: CommentStyle, vararg comment: String?) : this(
        key,
        defaultValue,
        *comment
    ) {
        this.commentStyle = commentStyle
    }

    /**
     * Load the setting if default value set. Doesn't load if config not set
     *
     * @param config The config to load this setting for
     */
    fun loadSetting(config: YamlConfig) {
        this.config = config
        if (defaultValue != null) this.config!!.setDefault(key, defaultValue, commentStyle, *comments)
    }

    /**
     * Set our value
     *
     * @param value Object to place as a value
     */
    fun set(value: Any?) {
        config!![key] = value
    }

    //  -------------------------------------------------------------------------
    //  Shorthand to get values from the config
    //  -------------------------------------------------------------------------

    val integerList: List<Int>
        get() = config!!.getIntegerList(key)
    val stringList: List<String>
        get() = config!!.getStringList(key)
    val boolean: Boolean
        get() = config!!.getBoolean(key)

    fun getBoolean(def: Boolean): Boolean {
        return config!!.getBoolean(key, def)
    }

    val int: Int
        get() = config!!.getInt(key)

    fun getInt(def: Int): Int {
        return config!!.getInt(key, def)
    }

    val long: Long
        get() = config!!.getLong(key)

    fun getLong(def: Long): Long {
        return config!!.getLong(key, def)
    }

    val double: Double
        get() = config!!.getDouble(key)

    fun getDouble(def: Double): Double {
        return config!!.getDouble(key, def)
    }

    val string: String?
        get() = config!!.getString(key)

    fun getString(def: String?): String? {
        return config!!.getString(key, def)
    }

    val `object`: Any?
        get() = config!![key]

    fun getObject(def: Any?): Any? {
        return config!![key, def]
    }

    fun <T> getObject(clazz: Class<T>): T? {
        return config!!.getObject(key, clazz)
    }

    fun <T> getObject(clazz: Class<T>, def: T?): T? {
        return config!!.getObject(key, clazz, def)
    }

    val char: Char
        get() = config!!.getChar(key)

    fun getChar(def: Char): Char {
        return config!!.getChar(key, def)
    }
}