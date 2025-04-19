package com.illuzionzstudios.mist.config

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.config.format.Comment
import com.illuzionzstudios.mist.config.format.CommentStyle
import com.illuzionzstudios.mist.util.Valid
import lombok.*
import org.bukkit.configuration.Configuration
import org.bukkit.configuration.MemoryConfiguration
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * A section of memory that relates to a configuration. This configuration
 * is usually a YAML file which is split into different memory sections
 *
 *
 * See [MemoryConfiguration]
 */
open class ConfigSection : MemoryConfiguration {

    /**
     * This is the main [ConfigSection] for the file
     * All sections come under it
     */
    protected val root: ConfigSection?

    /**
     * This is the [ConfigSection] that this section
     * is under
     */
    protected val parent: ConfigSection?

    /**
     * These are the current [Comment] loaded for this section
     * Each comment is mapped to the absolute path to the value
     */
    val configComments: HashMap<String?, Comment?>?

    /**
     * These are the default [Comment] for each value. These are
     * loaded when default values are set. Again each comment
     * is mapped to the absolute path to the value
     */
    val defaultComments: HashMap<String?, Comment?>?

    /**
     * These are the loaded key value pairs for this [ConfigSection]
     */
    val values: MutableMap<String?, Any?>?

    /**
     * These are the default key value pairs for this [ConfigSection]
     * They're what are automatically loaded if no values are found
     */
    var defaultValues: MutableMap<String?, Any?>

    /**
     * Flag if this section is a default [ConfigSection]
     * Meaning it gets loaded into the file if not found
     */
    var default: Boolean

    /**
     * This object here is invoked on by the `synchronized` tag
     * This is to lock performing operations on our section across threads.
     * This is because concurrently editing the config may lead to weird things
     * being saved or our data not being saved properly.
     *
     *
     * Each [ConfigSection] contains their own lock because we only
     * need one lock per instance, as we can edit different instances at
     * the different times. Although, we generally invoke on the [.root]
     * [ConfigSection] as we want to lock each actual [YamlConfig] instance
     */
    protected val lock = Any()

    /**
     * This is the full node path to this section.
     * For instance if this section is for a player, the
     * full path may be "data.player.<player_name>"
    </player_name> */
    val fullPath: String

    /**
     * This is the relevant node for the section, could
     * also be called the name. For instance take the above
     * example for [.fullPath], this would be "<player_name>"
    </player_name> */
    val nodeKey: String?
    //  -------------------------------------------------------------------------
    //  Values we may want to change
    //  -------------------------------------------------------------------------
    /**
     * The amount of SPACE chars to use as indentation.
     * This means space of each key from the parent section
     */
    var indentation = 2

    /**
     * This is the character to separate the paths with.
     * For instance if set to '.', paths will be "foo.bar".
     * And again '#', "foo#bar"
     *
     *
     * IMPORTANT: Must not be set when the config is currently
     * loaded or when adding [ConfigSection]. This is because
     * paths may then different with separators and produce a whole
     * bunch of errors and be a pain to debug
     */
    protected var pathSeparator = '.'

    /**
     * Flag is set to true if changes were made the the section at all
     * Useful for detecting to save
     */
    protected var changed = false

    /**
     * Init blank config section
     */
    constructor() {
        this.root = this
        this.parent = this
        default = false
        nodeKey = ""
        this.fullPath = ""
        configComments = HashMap()
        defaultComments = HashMap()
        values = LinkedHashMap()
        this.defaultValues = LinkedHashMap<String, Any>().toMutableMap()
    }

    /**
     * Setup the config section in another [ConfigSection]
     *
     * @param root      The absolute root [ConfigSection] (Main file)
     * @param parent    The [ConfigSection] just above [this]
     * @param nodeKey   See [.nodeKey]
     * @param isDefault See [.isDefault]
     */
    constructor(root: ConfigSection?, parent: ConfigSection?, nodeKey: String?, isDefault: Boolean) {
        this.root = root
        this.parent = parent
        this.nodeKey = nodeKey
        this.fullPath = if (nodeKey != null) parent!!.fullPath + nodeKey + root!!.pathSeparator else parent!!.fullPath
        this.default = isDefault
        defaultComments = null
        configComments = defaultComments
        defaultValues = LinkedHashMap<String, Any>().toMutableMap()
        values = null
    }

    /**
     * This method is invoked everytime we make manual changes to
     * values in the code. This is so we can make any operations
     * or update data when we make changes.
     *
     *
     * Can be overridden to setup our own stuff when making changes
     */
    protected open fun onChange() {
        // Also call change on main root section
        if (root != null && root !== this) {
            root.onChange()
        }
        // Reset
        changed = false
    }

    /**
     * @return Sanitized [.fullPath]
     */
    val key: String
        get() = if (!fullPath.endsWith(root!!.pathSeparator.toString())) fullPath else fullPath.substring(
            0,
            fullPath.length - 1
        )

    /**
     * This is used to create the [ConfigSection] for a node. This is in order
     * to set values and avoid null errors.
     * **DON'T INVOKE ON THE [.lock] OBJECT**
     *
     * @param path       Full path to the node for the value. Eg, foo.bar.node, this will create
     * a [ConfigSection] for "foo" and "foo.bar"
     * @param useDefault If the value to be set at this node is a default value
     */
    protected fun createNodePath(path: String, useDefault: Boolean) {
        // Make sure our path separator is valid
        if (path.indexOf(root!!.pathSeparator) != -1) {
            // If any nodes leading to this full path don't exist, create them
            val pathParts = path.split(Pattern.quote(root.pathSeparator.toString()).toRegex()).toTypedArray()
            val nodePath = StringBuilder(fullPath)

            // If creating default path, write the nodes to defaults
            val writeTo: MutableMap<String?, Any?>? = if (useDefault) root.defaultValues else root.values
            Objects.requireNonNull(writeTo, "Can't write to invalid value map")

            // Last node that was set
            var travelNode: ConfigSection? = this
            synchronized(root.lock) {
                // For each node to full path
                for (i in 0 until pathParts.size - 1) {
                    // Create the current node
                    val node =
                        (if (i != 0) nodePath.append(root.pathSeparator) else nodePath).append(pathParts[i]).toString()

                    // If not set as a config section, set it
                    if (writeTo?.get(node) !is ConfigSection) {
                        writeTo?.set(node,
                            ConfigSection(root, travelNode, pathParts[i], useDefault).also { travelNode = it })
                    } else {
                        // Else just set our current node the mapped node
                        travelNode = writeTo[node] as ConfigSection?
                    }
                }
            }
        }
    }
    //  -------------------------------------------------------------------------
    //  Section utils
    //  -------------------------------------------------------------------------
    /**
     * This will create a [ConfigSection] that acts as a default that must
     * appear in the section. Also optional comments for this section
     *
     * @param path    The relevant path from this [ConfigSection] to the new [ConfigSection]
     * to create
     * @param comment Varargs of comments to explain this section
     * @return The created [ConfigSection] for this path
     */
    fun createDefaultSection(path: String, vararg comment: String?): ConfigSection {
        createNodePath(path, true)

        // Create the section
        val section = ConfigSection(root, this, path, true)

        // Assure not null
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull(
            root.defaultComments, "Root config has invalid default comments map"
        )

        // Insert into root maps
        synchronized(root.lock) {
            root.defaultValues[fullPath + path] = section
            root.defaultComments!!.put(fullPath + path, Comment(*comment))
        }
        return section
    }

    /**
     * See [.createDefaultSection]
     *
     * @param style The custom [CommentStyle] for the comments
     */
    fun createDefaultSection(path: String, style: CommentStyle?, vararg comment: String?): ConfigSection {
        createNodePath(path, true)

        // Create the section
        val section = ConfigSection(root, this, path, true)

        // Assure not null
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull(
            root.defaultComments, "Root config has invalid default comments map"
        )

        // Insert into root maps
        synchronized(root.lock) {
            root.defaultValues[fullPath + path] = section
            root.defaultComments!!.put(fullPath + path, Comment(style, *comment))
        }
        return section
    }
    //  -------------------------------------------------------------------------
    //  Help with manipulation of comments
    //  -------------------------------------------------------------------------
    /**
     * See [.setComment] and construct [Comment] from parameters
     *
     * @param commentStyle The styling for the comment
     * @param lines        The lines to set
     */
    fun setComment(path: String, commentStyle: CommentStyle?, vararg lines: String?): ConfigSection {
        return setComment(path, Comment(commentStyle, *lines))
    }

    /**
     * See [.setComment]
     */
    fun setComment(path: String, commentStyle: CommentStyle?, lines: List<String>?): ConfigSection {
        return setComment(path, if (lines != null) Comment(commentStyle, lines) else null)
    }

    /**
     * Set a [Comment] for a node of a [ConfigSection]
     *
     * @param path    The relevant path to the node to set
     * @param comment The [Comment] object to set
     * @return The [ConfigSection] the comment was set for
     */
    fun setComment(path: String, comment: Comment?): ConfigSection {
        // Assure not null
        Objects.requireNonNull(root!!.defaultComments, "Root config has invalid default comments map")
        Objects.requireNonNull(
            root.configComments, "Root config has invalid config comments map"
        )
        synchronized(root.lock) {
            if (default) {
                root.defaultComments!!.put(fullPath + path, comment)
            } else {
                root.configComments!!.put(fullPath + path, comment)
            }
        }
        return this
    }

    /**
     * See [.setDefaultComment]
     */
    fun setDefaultComment(path: String, vararg lines: String?): ConfigSection {
        return setDefaultComment(path, (if (lines.isEmpty()) null else listOf(*lines)) as List<String>?)
    }

    /**
     * See [.setDefaultComment]
     */
    fun setDefaultComment(path: String, lines: List<String>?): ConfigSection {
        setDefaultComment(fullPath + path, Comment(lines))
        return this
    }

    /**
     * See [.setDefaultComment]
     */
    fun setDefaultComment(path: String, commentStyle: CommentStyle?, vararg lines: String?): ConfigSection {
        return setDefaultComment(
            path, commentStyle,
            (if (lines.isEmpty()) null else listOf(*lines)) as List<String>?
        )
    }

    /**
     * See [.setDefaultComment] but we set [CommentStyle]
     * for the comments
     */
    fun setDefaultComment(path: String, commentStyle: CommentStyle?, lines: List<String>?): ConfigSection {
        setDefaultComment(fullPath + path, Comment(commentStyle, lines))
        return this
    }

    /**
     * See [.setComment] but we are setting default values,
     * so mapped to [.defaultComments]
     */
    fun setDefaultComment(path: String, comment: Comment?): ConfigSection {
        Objects.requireNonNull(
            root!!.defaultComments, "Root config has invalid default comments map"
        )
        synchronized(root.lock) { root.defaultComments!!.put(fullPath + path, comment) }
        return this
    }

    /**
     * Get the [Comment] instance from a relevant node path
     * May produce `null`
     *
     * @param path The relevant path to the value
     * @return The [Comment] for the [ConfigSection] if applicable
     */
    fun getComment(path: String): Comment? {
        Objects.requireNonNull(
            root!!.defaultComments, "Root config has invalid default comments map"
        )
        Objects.requireNonNull(
            root.configComments, "Root config has invalid config comments map"
        )
        var result = root.configComments!![fullPath + path]
        if (result == null) {
            result = root.defaultComments!![fullPath + path]
        }
        return result
    }

    /**
     * See [.getComment]
     *
     * @return [Comment] invoked with [Comment.toString]
     * May produce `null`
     */
    fun getCommentString(path: String): String? {
        val result = getComment(path)
        return result?.toString()
    }
    //  -------------------------------------------------------------------------
    //  Methods to get sections and keys
    //  -------------------------------------------------------------------------
    /**
     * This method will create a default value for a specific node path
     *
     * @param path  The relative path to add the default to
     * @param value The value to set for this node
     */
    override fun addDefault(path: String, value: Any?) {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        createNodePath(path, true)
        synchronized(root.lock) { root.defaultValues[fullPath + path] = value }
    }

    /**
     * @return A new [ConfigSection] with this as a parent as a default section
     */
    override fun getDefaults(): ConfigSection? {
        return ConfigSection(root, this, null, true)
    }

    /**
     * @param configuration Set the default configuration adapter
     */
    override fun setDefaults(configuration: Configuration) {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        if (fullPath.isEmpty()) {
            root.defaultValues.clear()
        } else {
            root.defaultValues.keys.stream()
                .filter { k: String? -> k?.startsWith(fullPath) ?: false }
                .forEach { key: String? -> root.defaultValues.remove(key) }
        }
        addDefaults(configuration)
    }

    /**
     * @return See [getDefaults]
     */
    override fun getDefaultSection(): ConfigSection? {
        return getDefaults()
    }

    /**
     * Used to get all the node paths set for values. This is only for every path under this
     * [ConfigSection]. For instance if there is "foo.bar" and "bar.foo", will return foo and bar.
     * If the deep option is set, will return foo foo.bar bar bar.foo
     *
     * @param deep If to recursive search for nodes otherwise returns full paths
     * @return A set of path nodes as a [String]
     */
    override fun getKeys(deep: Boolean): Set<String> {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")

        // Set of keys
        val result = LinkedHashSet<String>()
        val pathIndex = fullPath.lastIndexOf(root.pathSeparator)
        if (deep) {
            result.addAll(
                root.defaultValues.keys.stream()
                    .filter { k: String? -> k?.startsWith(fullPath) ?: false }
                    .map { k: String? ->
                        if (!k?.endsWith(root.pathSeparator.toString())!!)
                            k.substring(pathIndex + 1) else k.substring(pathIndex + 1, k.length - 1)
                    }
                    .collect(Collectors.toCollection { LinkedHashSet() })
            )
            result.addAll(
                root.values!!.keys.stream()
                    .filter { k: String? -> k!!.startsWith(fullPath) }
                    .map { k: String? ->
                        if (!k!!.endsWith(
                                root.pathSeparator.toString()
                            )
                        ) k.substring(pathIndex + 1) else k.substring(pathIndex + 1, k.length - 1)
                    }
                    .collect(
                        Collectors.toCollection { LinkedHashSet() }
                    )
            )
        } else {
            result.addAll(
                root.defaultValues.keys.stream()
                    .filter { k: String? ->
                        k!!.startsWith(fullPath) && k.lastIndexOf(
                            root.pathSeparator
                        ) == pathIndex
                    }
                    .map { k: String? ->
                        if (!k!!.endsWith(
                                root.pathSeparator.toString()
                            )
                        ) k.substring(pathIndex + 1) else k.substring(pathIndex + 1, k.length - 1)
                    }
                    .collect(Collectors.toCollection { LinkedHashSet() })
            )
            result.addAll(
                root.values!!.keys.stream()
                    .filter { k: String? ->
                        k!!.startsWith(fullPath) && k.lastIndexOf(
                            root.pathSeparator
                        ) == pathIndex
                    }
                    .map { k: String? ->
                        if (!k!!.endsWith(
                                root.pathSeparator.toString()
                            )
                        ) k.substring(pathIndex + 1) else k.substring(pathIndex + 1, k.length - 1)
                    }
                    .collect(
                        Collectors.toCollection { LinkedHashSet() }
                    )
            )
        }
        return result
    }

    /**
     * See [.getKeys]
     *
     *
     * Will do the same but instead map the nodes to the value found at that path
     *
     * @return A map of nodes to their values
     */
    override fun getValues(deep: Boolean): Map<String, Any> {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")
        val collectorFunction = Function { pathIndex1: Int ->
            Collectors.toMap<Map.Entry<String?, Any?>, String, Any, LinkedHashMap<String, Any>>(
                { (key1): Map.Entry<String?, Any?> ->
                    if (!key1!!.endsWith(
                            root.pathSeparator.toString()
                        )
                    ) key1.substring(pathIndex1 + 1) else key1.substring(pathIndex1 + 1, key1.length - 1)
                }, { (_, value) -> value },
                { _: Any?, _: Any? -> throw IllegalStateException() }) { LinkedHashMap() }
        }
        val result = LinkedHashMap<String, Any>()
        val pathIndex = fullPath.lastIndexOf(root.pathSeparator)
        if (deep) {
            result.putAll(
                root.defaultValues.entries.stream()
                    .filter { key1: MutableMap.MutableEntry<String?, Any?> -> key1.key!!.startsWith(fullPath) }
                    .collect(collectorFunction.apply(pathIndex))
            )
            result.putAll(
                root.values!!.entries.stream()
                    .filter { (key1): Map.Entry<String?, Any?> -> key1!!.startsWith(fullPath) }
                    .collect(collectorFunction.apply(pathIndex)))
        } else {
            result.putAll(
                root.defaultValues.entries.stream()
                    .filter { key1: MutableMap.MutableEntry<String?, Any?> ->
                        key1.key!!.startsWith(fullPath) && key1.key!!.lastIndexOf(
                            root.pathSeparator
                        ) == pathIndex
                    }
                    .collect(collectorFunction.apply(pathIndex))
            )
            result.putAll(
                root.values!!.entries.stream()
                    .filter { (key1): Map.Entry<String?, Any?> ->
                        key1!!.startsWith(fullPath) && key1.lastIndexOf(
                            root.pathSeparator
                        ) == pathIndex
                    }
                    .collect(collectorFunction.apply(pathIndex)))
        }
        return result
    }

    /**
     * See [.getKeys]
     *
     * This will perform a shallow search for all keys and
     * add all found [ConfigSection] for that node path
     *
     * @param path The relative path to find
     * @return A list of found [ConfigSection]
     */
    fun getSections(path: String): List<ConfigSection?> {
        val rootSection = getConfigurationSection(path) ?: return emptyList()
        val result = ArrayList<ConfigSection?>()
        rootSection.getKeys(false).stream()
            .map { p: String -> rootSection[p] }
            .filter { `object`: Any? -> `object` is ConfigSection }
            .forEachOrdered { `object`: Any? -> result.add(`object` as ConfigSection?) }
        return result
    }

    /**
     * Check if a value is set at a node path, ie, path is in our value map
     *
     * @param path The path to check
     * @return Whether their is a value set there
     */
    override fun contains(path: String): Boolean {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")
        return root.defaultValues.contains(fullPath + path) || root.values!!.containsKey(fullPath + path)
    }

    /**
     * See [.contains]
     *
     * @param ignoreDefault If to not check the defaults as well
     */
    override fun contains(path: String, ignoreDefault: Boolean): Boolean {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")
        return !ignoreDefault && root.defaultValues.contains(fullPath + path) || root.values!!.containsKey(fullPath + path)
    }

    /**
     * See [.contains] except checks if value set is not null, ie, an actual value set
     */
    override fun isSet(path: String): Boolean {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")
        return root.defaultValues[fullPath + path] != null || root.values!![fullPath + path] != null
    }

    /**
     * @return Sanitized [.fullPath] without [.pathSeparator]
     */
    override fun getCurrentPath(): String {
        return if (fullPath.isEmpty()) "" else fullPath.substring(0, fullPath.length - 1)
    }

    /**
     * @return See [.nodeKey]
     */
    override fun getName(): String {
        return nodeKey!!
    }

    //  -------------------------------------------------------------------------
    //  Getting and setting values in the config
    //  -------------------------------------------------------------------------

    /**
     * Simplest way to get a value from the [ConfigSection]
     * If value was found at path, simply returns as [Object],
     * this means we need to do our own casting
     *
     * @param path The path to search for
     * @return Found object, could be `null`
     */
    override fun get(path: String): Any? {
        Objects.requireNonNull(root!!.defaultValues, "Root config has invalid default values map")
        Objects.requireNonNull<Map<String?, Any?>?>(root.values, "Root config has invalid values map")
        return root.values!![fullPath + path] ?: root.defaultValues[fullPath + path]
    }

    /**
     * See [.get]
     *
     * Able to provide a default value that get returns should nothing be
     * found at the path. Also doesn't search in defaults if not found, instead
     * returns our def.
     *
     * @param path The path to search for
     * @param def  Default object to return should one not be found
     * @return Found object or default
     */
    override fun get(path: String, def: Any?): Any? {
        Objects.requireNonNull<Map<String?, Any?>?>(root!!.values, "Root config has invalid values map")
        return root.values!![fullPath + path] ?: def
    }

    /**
     * See [.get]
     *
     * In this instance we provide a type and ensure our found object
     * get's returned as this type. Can still produce `null`
     *
     * If object isn't instance of T, simply return null
     *
     * @param path The path to search for
     * @param type The class of T for casting
     * @param <T>  The type that the found object must be
     * @return Found value as T
     */
    fun <T> getT(path: String, type: Class<T>): T? {
        return getT(path, type, null)
    }

    /**
     * See [.getT]
     *
     * Except we are able to return a default instance of T should the value
     * not be found or not be able to cast to T
     */
    fun <T> getT(path: String, type: Class<T>, def: T?): T? {
        val raw = get(path)
        return if (type.isInstance(raw)) type.cast(raw) else def
    }

    /**
     * A simple way to set a value in the config. It will set the value
     * at a given path. It then checks for nodes without a value to free up
     * memory and make sure if we set anything null, it's removed
     *
     * @param path  Path to set value at
     * @param value Object to place as a value. Setting to `null` removes value from memory
     */
    override fun set(path: String, value: Any?) {
        Objects.requireNonNull(root!!.values, "Root config has invalid values map")
        if (default) {
            // If it's a default section, set a default value
            addDefault(path, value)
        } else {
            createNodePath(path, false)

            // Attempt to set current value
            var last: Any?
            synchronized(root.lock) {
                if (value != null) {
                    root.changed = root.changed or (root.values!!.put(fullPath + path, value).also { last = it } != value)
                } else {
                    root.changed = root.changed or (root.values!!.remove(fullPath + path).also { last = it } != null)
                }
            }

            if (last != null && last != value && last is ConfigSection) {
                // Then try remove nodes that don't have a value set in the config anymore
                // This is in case we set this value to null
                val trim = fullPath + path + root.pathSeparator
                synchronized(root.lock) {
                    root.values!!.keys.stream().filter { k: String? -> k!!.startsWith(trim) }
                        .collect(Collectors.toSet())
                        .forEach(Consumer { key: String? -> root.values.remove(key) })
                }
            }
            onChange()
        }
    }

    /**
     * See [.set] and [.setComment]
     */
    fun set(path: String, value: Any?, vararg comment: String?): ConfigSection {
        set(path, value)
        return setComment(path, null, *comment)
    }

    /**
     * See [.set]
     */
    operator fun set(path: String, value: Any?, comment: List<String>?): ConfigSection {
        set(path, value)
        return setComment(path, null, comment)
    }

    /**
     * See [.set] but set comment styling
     */
    fun set(path: String, value: Any?, commentStyle: CommentStyle?, vararg comment: String?): ConfigSection {
        set(path, value)
        return setComment(path, commentStyle, *comment)
    }

    /**
     * See [.set]
     */
    operator fun set(path: String, value: Any?, commentStyle: CommentStyle?, comment: List<String>?): ConfigSection {
        set(path, value)
        return setComment(path, commentStyle, comment)
    }

    /**
     * See [.addDefault]
     */
    fun setDefault(path: String, value: Any?): ConfigSection {
        addDefault(path, value)
        return this
    }

    /**
     * See [.setDefault] and [.setDefaultComment]
     */
    fun setDefault(path: String, value: Any?, vararg comment: String?): ConfigSection {
        addDefault(path, value)
        return setDefaultComment(path, *comment)
    }

    /**
     * See [.setDefault]
     */
    fun setDefault(path: String, value: Any?, comment: List<String>?): ConfigSection {
        addDefault(path, value)
        return setDefaultComment(path, comment)
    }

    /**
     * See [.set] but default
     */
    fun setDefault(path: String, value: Any?, commentStyle: CommentStyle?, vararg comment: String?): ConfigSection {
        addDefault(path, value)
        return setDefaultComment(path, commentStyle, *comment)
    }

    /**
     * See [.setDefault]
     */
    fun setDefault(path: String, value: Any?, commentStyle: CommentStyle?, comment: List<String>?): ConfigSection {
        addDefault(path, value)
        return setDefaultComment(path, commentStyle, comment)
    }

    /**
     * See [.set]
     *
     * This does the same thing except we are setting a new [ConfigSection]
     * This may be if we want to construct things under it and bulk update
     *
     * @param path The path to set for
     * @return The set [ConfigSection]
     */
    override fun createSection(path: String): ConfigSection {
        Objects.requireNonNull<Map<String?, Any?>?>(root!!.values, "Root config has invalid values map")
        createNodePath(path, false)
        val section = ConfigSection(root, this, path, false)
        synchronized(root.lock) { root.values!!.put(fullPath + path, section) }

        // Added section so changed
        root.changed = true
        onChange()
        return section
    }

    fun createSection(path: String, vararg comment: String?): ConfigSection {
        return createSection(path, null, (if (comment.isEmpty()) null else listOf(*comment)) as List<String>?)
    }

    fun createSection(path: String, comment: List<String>?): ConfigSection {
        return createSection(path, null, comment)
    }

    fun createSection(path: String, commentStyle: CommentStyle?, vararg comment: String?): ConfigSection {
        return createSection(
            path, commentStyle,
            (if (comment.isEmpty()) null else listOf(*comment)) as List<String>?
        )
    }

    /**
     * See [.createSection] and [.set]
     *
     * Except we are doing this on a new [ConfigSection]
     */
    fun createSection(path: String, commentStyle: CommentStyle?, comment: List<String>?): ConfigSection {
        Objects.requireNonNull<Map<String?, Any?>?>(root!!.values, "Root config has invalid values map")
        createNodePath(path, false)
        val section = ConfigSection(root, this, path, false)
        synchronized(root.lock) { root.values!!.put(fullPath + path, section) }
        setComment(path, commentStyle, comment)
        root.changed = true
        onChange()
        return section
    }

    /**
     * See [.createSection]
     *
     * Except we are able to map node value pairs to this section already
     */
    override fun createSection(path: String, map: Map<*, *>): ConfigSection {
        Objects.requireNonNull<Map<String?, Any?>?>(root!!.values, "Root config has invalid values map")
        createNodePath(path, false)
        val section = ConfigSection(root, this, path, false)
        synchronized(root.lock) { root.values!!.put(fullPath + path, section) }

        // Map into section
        for ((key1, value) in map) {
            if (value is Map<*, *>) {
                section.createSection(key1.toString(), (value as Map<*, *>?)!!)
                continue
            }
            section[key1.toString()] = value
        }
        root.changed = true
        onChange()
        return section
    }

    //  -------------------------------------------------------------------------
    //  Type safe getters
    //  -------------------------------------------------------------------------

    override fun getString(path: String): String? {
        return getT(path, String::class.java, "")
    }

    override fun getString(path: String, def: String?): String? {
        return getT(path, String::class.java, def)
    }

    fun getChar(path: String): Char {
        return getT(path, Char::class.java, '\u0000')!!
    }

    fun getChar(path: String, def: Char): Char {
        return getT(path, Char::class.java, def)!!
    }

    override fun getInt(path: String): Int {
        val result = get(path)
        return if (result is Number) result.toInt() else 0
    }

    override fun getInt(path: String, def: Int): Int {
        val result = get(path)
        return if (result is Number) result.toInt() else def
    }

    override fun getBoolean(path: String): Boolean {
        val result = get(path)
        return if (result is Boolean) result else false
    }

    override fun getBoolean(path: String, def: Boolean): Boolean {
        val result = get(path, def)
        return if (result is Boolean) result else false
    }

    override fun getDouble(path: String): Double {
        val result = get(path)
        return if (result is Number) result.toDouble() else 0.toDouble()
    }

    override fun getDouble(path: String, def: Double): Double {
        val result = get(path)
        return if (result is Number) result.toDouble() else def
    }

    override fun getLong(path: String): Long {
        val result = get(path)
        return if (result is Number) result.toLong() else 0
    }

    override fun getLong(path: String, def: Long): Long {
        val result = get(path)
        return if (result is Number) result.toLong() else def
    }

    override fun getList(path: String): List<*>? {
        return getT(path, List::class.java)
    }

    override fun getList(path: String, def: List<*>?): List<*>? {
        return getT(path, List::class.java, def)
    }

    override fun getConfigurationSection(path: String): ConfigSection? {
        return getT(path, ConfigSection::class.java)
    }

    /**
     * See [.getConfigurationSection]
     *
     * Except it will create the section if not found
     */
    fun getOrCreateConfigurationSection(path: String): ConfigSection {
        return getT(path, ConfigSection::class.java, createSection(path))!!
    }
}