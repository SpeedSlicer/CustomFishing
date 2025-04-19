package com.illuzionzstudios.mist.config

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.Mist
import com.illuzionzstudios.mist.config.format.Comment
import com.illuzionzstudios.mist.config.format.CommentStyle
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.FileUtil
import com.illuzionzstudios.mist.util.TextUtil
import org.apache.commons.lang.Validate
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConstructor
import org.bukkit.configuration.file.YamlRepresenter
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.representer.Representer
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors


/**
 * Handles a [ConfigSection] as a YAML file. This means being a file
 * and having a file location along with saving, reading, and writing utils.
 */
open class YamlConfig : ConfigSection {
    /**
     * This is the REGEX to parse YAML syntax. Matches "key: value" making sure syntax is right
     */
    protected val YAML_REGEX = Pattern.compile("^( *)([^:{}\\[\\],&*#?|<>=!%@`]+):(.*)$")

    /**
     * This is the path to the directory to store the file in. This
     * is taken relative from the [JavaPlugin.getDataFolder], meaning
     * the value "" is the [JavaPlugin.getDataFolder].
     *
     * If we set this value to "foo", this final dir would be "foo/config.yml"
     */
    protected val directory: String?

    /**
     * This is the name of the file for this [YamlConfig]. Name must include
     * the file extensions, otherwise default "yml" will be appended.
     */
    protected val fileName: String?

    /**
     * This is the instance of the [SpigotPlugin] that this [YamlConfig]
     * belongs to.
     */
    protected val plugin: SpigotPlugin?

    /**
     * These are YAML options used to help parse the file
     */
    private val yamlOptions = DumperOptions()
    private val yamlRepresenter: Representer = YamlRepresenter()
    private val yaml = Yaml(YamlConstructor(), yamlRepresenter, yamlOptions)

    /**
     * Flag indicating if the file is loaded
     */
    protected var isLoaded = false

    /**
     * This is the actual file that this [YamlConfig] is
     */
    protected var file: File? = null
        get() {
            if (field == null) {
                field = if (directory != null) {
                    File(plugin!!.dataFolder.toString() + File.separator + directory, fileName ?: Mist.SETTINGS_NAME)
                } else {
                    File(plugin!!.dataFolder, fileName ?: Mist.SETTINGS_NAME)
                }
            }
            return field
        }

    /**
     * Comments to display at the top/bottom of the file
     * to give more clarity on the contents
     */
    protected var headerComment: Comment? = null
    protected var footerComment: Comment? = null

    /**
     * This is the [Charset] to use for saving the file
     */
    private var defaultCharset = StandardCharsets.UTF_8

    /**
     * This flag indicates if we should remove nodes not included in defaults
     */
    private val autoRemove = false

    /**
     * This flag indicates if we should load comments to the file
     */
    private val loadComments = true

    /**
     * Default comment styling applied to nodes that hold a normal value
     */
    private val defaultNodeCommentFormat = CommentStyle.SIMPLE

    /**
     * Default comment styling applied to nodes that hold a [ConfigSection]
     */
    private val defaultSectionCommentFormat = CommentStyle.SPACED

    /**
     * Extra lines to put between root nodes, as in a "\n"
     */
    private val rootNodeSpacing = 1

    /**
     * Extra lines to put in front of comments. <br></br>
     * This is separate from rootNodeSpacing, if applicable.
     * These are " " characters
     */
    private val commentSpacing = 0

    //  -------------------------------------------------------------------------
    //  Constructors
    //  -------------------------------------------------------------------------
    constructor() {
        plugin = null
        file = null
        directory = null
        fileName = null
    }

    constructor(file: File) {
        plugin = null
        this.file = file
        directory = null
        fileName = file.name
    }

    constructor(plugin: SpigotPlugin) {
        this.plugin = plugin
        directory = null
        fileName = null
    }

    constructor(plugin: SpigotPlugin, file: String) {
        this.plugin = plugin
        directory = null
        fileName = file
    }

    constructor(plugin: SpigotPlugin, directory: String?, file: String) {
        this.plugin = plugin
        this.directory = directory
        fileName = file
    }

    /**
     * @return [String] lines from [.headerComment]
     */
    val header: List<String?>
        get() = if (headerComment != null) {
            headerComment!!.lines!!
        } else {
            ArrayList()
        }

    /**
     * Set the [.headerComment] from VarArgs of [String]
     *
     * @param description Strings to set for comment
     */
    fun setHeader(vararg description: String) {
        headerComment = if (description.size == 0) {
            null
        } else {
            Comment(
                CommentStyle.BLOCKED,
                *description
            )
        }
    }

    /**
     * @return [String] lines from [.footerComment]
     */
    val footer: List<String?>
        get() = if (footerComment != null) {
            footerComment!!.lines!!
        } else {
            ArrayList()
        }

    /**
     * Set the [.footerComment] from VarArgs of [String]
     *
     * @param description Strings to set for comment
     */
    fun setFooter(vararg description: String) {
        footerComment = if (description.size == 0) {
            null
        } else {
            Comment(
                CommentStyle.BLOCKED,
                *description
            )
        }
    }

    /**
     * Clear all nodes and values in this config in memory.
     * DOES NOT SAVE TO DISK.
     *
     * @param clearDefaults If to also invoke [.clearDefaults]
     */
    fun clearConfig(clearDefaults: Boolean) {
        root!!.values!!.clear()
        root.configComments!!.clear()
        if (clearDefaults) {
            clearDefaults()
        }
    }
    //  -------------------------------------------------------------------------
    //  File Loading
    //  -------------------------------------------------------------------------
    /**
     * Clear all default options in the [ConfigSection]
     */
    fun clearDefaults() {
        root!!.defaultComments!!.clear()
        root.defaultValues.clear()
    }

    /**
     * To be overridden, called before loading the file into memory
     */
    protected fun preLoad() {}

    /**
     * To be overridden, called after loading the file into memory
     */
    protected fun postLoad() {}

    /**
     * Loads an internal resource onto the server
     * For instance file in resources/locales/en_US.lang will be loaded
     * onto the server under plugins/MY_PLUGIN/locales/en_US.lang
     *
     * Main applications are implementing this [YamlConfig] into a custom
     * object, for instance implementing a specific type of config. This way if we have
     * any defaults in the plugin we can load to the server.
     *
     * If file already exists on disk it just loads that.
     *
     * @param file The file object to load
     */
    /**
     * Load the [.file] into memory
     *
     * @return If loaded successfully
     */
    @JvmOverloads
    fun load(file: File = this.file!!): Boolean {
        Validate.notNull(file, "File cannot be null")
        val fileName = file.name

        if (file.exists()) {
            BufferedInputStream(FileInputStream(file)).use { stream ->
                this.load(InputStreamReader(stream, StandardCharsets.UTF_8))
            }

            isLoaded = true
            return true
        }

        // Internal path to locale
        val internalPath = (if (directory != null && directory.trim { it <= ' ' }.equals("", ignoreCase = true)) "" else "$directory/") + fileName
        // Attempt to find resource
        val input: InputStream? = FileUtil.getInternalResource(internalPath)

        // Load buffers
        // Input stream for internal file and existing file
        try {
            BufferedInputStream(input!!).use { defaultIn ->
                BufferedReader(InputStreamReader(defaultIn, StandardCharsets.UTF_8)).use { defaultReader ->
                    // Load from default
                    load(defaultReader)
                    // Then save in server
                    save(file)

                    isLoaded = true
                    return true
                }
            }
        } catch (ex: Exception) {
            // Couldn't find internal resource so just don't even load
        }
        return false
    }

    @Throws(IOException::class, InvalidConfigurationException::class)
    fun load(reader: Reader) {
        val builder = StringBuilder()
        reader.buffered().use { input ->
            var firstLine = true
            input.forEachLine { line ->
                builder.append(if (firstLine) line.replace("[\uFEFF\uFFFE\u200B]".toRegex(), "") else line).append('\n')
                if (firstLine) {
                    firstLine = false
                }
            }
        }
        loadFromString(builder.toString())
    }

    @Throws(InvalidConfigurationException::class)
    fun loadFromString(contents: String) {
        Validate.notNull(contents, "Contents cannot be null")
        val input: Map<*, *>? = try {
            yaml.load(contents)
        } catch (e2: YAMLException) {
            throw InvalidConfigurationException(e2)
        } catch (e3: ClassCastException) {
            throw InvalidConfigurationException("Top level is not a Map.")
        }
        if (input != null) {
            // Override old values
            map.clear()
            if (loadComments) {
                parseComments(contents, input)
            }
            convertMapsToSections(input, this)
        }

        // Loading is done
        postLoad()
    }

    protected fun convertMapsToSections(input: Map<*, *>, section: ConfigSection) {
        for ((key1, value1) in input) {
            val key = key1.toString()
            val value = value1!!
            if (value is Map<*, *>) {
                convertMapsToSections(value, section.createSection(key))
            } else {
                section[key] = value
            }
        }
    }

    protected fun parseComments(contents: String, input: Map<*, *>) {
        // If starts with a comment, load all non-breaking comments as a header
        // then load all comments and assign to the next valid node loaded
        // (Only load comments that are on their own line)
        val `in` = BufferedReader(StringReader(contents))
        var insideScalar = false
        var firstNode = true
        var index = 0
        val currentPath = LinkedList<String>()
        val commentBlock = ArrayList<String>()
        try {
            `in`.forEachLine { line ->
                if (line.isEmpty()) {
                    if (firstNode && !commentBlock.isEmpty()) {
                        // header comment
                        firstNode = false
                        headerComment = Comment.loadComment(commentBlock)
                        commentBlock.clear()
                    }
                    return@forEachLine
                } else if (line.trim { it <= ' ' }.startsWith("#")) {
                    // only load full-line comments
                    commentBlock.add(line.trim { it <= ' ' })
                    return@forEachLine
                }

                // check to see if this is a line that we can process
                val lineOffset: Int = TextUtil.getOffset(line)
                insideScalar = insideScalar and (lineOffset <= index)
                var m: Matcher? = null
                if (!insideScalar && YAML_REGEX.matcher(line).also { m = it }.find()) {
                    // we found a config node! ^.^
                    // check to see what the full path is
                    val depth = m!!.group(1).length / indentation
                    while (depth < currentPath.size) {
                        currentPath.removeLast()
                    }
                    currentPath.add(m!!.group(2))

                    // do we have a comment for this node?
                    if (!commentBlock.isEmpty()) {
                        val path = currentPath.stream().collect(Collectors.joining(pathSeparator.toString()))
                        val comment: Comment = Comment.Companion.loadComment(commentBlock)
                        commentBlock.clear()
                        setComment(path, comment)
                    }
                    firstNode = false // we're no longer on the first node

                    // ignore scalars
                    index = lineOffset
                    if (m!!.group(3).trim { it <= ' ' } == "|" || m!!.group(3).trim { it <= ' ' } == ">") {
                        insideScalar = true
                    }
                }
            }
            if (commentBlock.isNotEmpty()) {
                footerComment = Comment.loadComment(commentBlock)
                commentBlock.clear()
            }
        } catch (ignored: IOException) {
        }
    }

    /**
     * Delete all nodes and values that aren't default values
     */
    fun deleteNonDefaultSettings() {
        // Delete old config values (thread-safe)
        val defaultKeys = listOf(*defaultValues.keys.toTypedArray())
        for (key in values!!.keys.toTypedArray()) {
            if (!defaultKeys.contains(key)) {
                values.remove(key)
            }
        }
    }

    /**
     * Every time we change the file through code save it to disk
     */
    override fun onChange() {
        saveChanges()
    }

    /**
     * Save current values in memory to the file on disk
     *
     * @return If it saved correctly
     */
    fun saveChanges(): Boolean {
        var saved = true
        if (changed || hasNewDefaults()) {
            saved = save()
        }
        return saved
    }

    /**
     * @return If has default values defined by no values
     * have been set at those nodes.
     */
    fun hasNewDefaults(): Boolean {
        if (file != null && !file!!.exists()) return true
        for (def in defaultValues.keys) {
            if (!values!!.containsKey(def)) return true
        }
        return false
    }

    fun save(file: String): Boolean {
        Validate.notNull(file, "File cannot be null")
        return this.save(File(file))
    }

    @JvmOverloads
    fun save(file: File = this.file!!): Boolean {
        Validate.notNull(file, "File cannot be null")
        if (file.parentFile != null && !file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        val data = saveToString()
        try {
            OutputStreamWriter(FileOutputStream(file), defaultCharset).use { writer -> writer.write(data) }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    fun saveToString(): String {
        try {
            if (autoRemove) {
                deleteNonDefaultSettings()
            }
            yamlOptions.indent = indentation
            yamlOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            yamlOptions.splitLines = false
            yamlRepresenter.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            val str = StringWriter()
            if (headerComment != null) {
                headerComment!!.writeComment(str, 0, CommentStyle.BLOCKED)
                str.write("\n") // add one space after the header
            }
            val dump = yaml.dump(this.getValues(false))
            if (dump != BLANK_CONFIG) {
                writeComments(dump, str)
            }
            if (footerComment != null) {
                str.write("\n")
                footerComment!!.writeComment(str, 0, CommentStyle.BLOCKED)
            }
            return str.toString()
        } catch (ex: Throwable) {
            Logger.displayError(ex, "Error loading config")
            saveChanges()
        }
        return ""
    }

    @Throws(IOException::class)
    protected fun writeComments(data: String?, out: Writer) {
        // line-by-line apply line spacing formatting and comments per-node
        val `in` = BufferedReader(StringReader(data!!))
        var line: String?
        var insideScalar = false
        var firstNode = true
        var index = 0
        val currentPath = LinkedList<String>()
        while (`in`.readLine().also { line = it } != null) {
            // ignore comments and empty lines (there shouldn't be any, but just in case)
            if (line!!.trim { it <= ' ' }.startsWith("#") || line!!.isEmpty()) {
                continue
            }

            // check to see if this is a line that we can process
            val lineOffset: Int = TextUtil.getOffset(line!!)
            insideScalar = insideScalar and (lineOffset <= index)
            var m: Matcher? = null
            if (!insideScalar && YAML_REGEX.matcher(line!!).also { m = it }.find()) {
                // we found a config node! ^.^
                // check to see what the full path is
                val depth = m!!.group(1).length / indentation
                while (depth < currentPath.size) {
                    currentPath.removeLast()
                }
                currentPath.add(m!!.group(2))
                val path = currentPath.stream().collect(Collectors.joining(pathSeparator.toString()))

                // if this is a root-level node, apply extra spacing if we aren't the first node
                if (!firstNode && depth == 0 && rootNodeSpacing > 0) {
                    out.write(
                        String(CharArray(rootNodeSpacing)).replace(
                            "\u0000",
                            "\n"
                        )
                    ) // yes it's silly, but it works :>
                }
                firstNode = false // we're no longer on the first node

                // insert the relavant comment
                val comment = getComment(path)
                if (comment != null) {
                    // add spacing between previous nodes and comments
                    if (depth != 0) {
                        out.write(String(CharArray(commentSpacing)).replace("\u0000", "\n"))
                    }

                    // formatting style for this node
                    var style = comment.styling
                    if (style == null) {
                        // check to see what type of node this is
                        style = if (m!!.group(3).trim { it <= ' ' }.isNotEmpty()) {
                            // setting node
                            defaultNodeCommentFormat
                        } else {
                            // probably a section? (need to peek ahead to check if this is a list)
                            `in`.mark(1000)
                            val nextLine = `in`.readLine().trim { it <= ' ' }
                            `in`.reset()
                            if (nextLine.startsWith("-")) {
                                // not a section :P
                                defaultNodeCommentFormat
                            } else {
                                defaultSectionCommentFormat
                            }
                        }
                    }

                    // write it down!
                    comment.writeComment(out, lineOffset, style)
                }
                // ignore scalars
                index = lineOffset
                if (m!!.group(3).trim { it <= ' ' } == "|" || m!!.group(3).trim { it <= ' ' } == ">") {
                    insideScalar = true
                }
            }
            out.write(line!!)
            out.write("\n")
        }
    }

    companion object {
        //  -------------------------------------------------------------------------
        //  Final variables
        //  -------------------------------------------------------------------------
        /**
         * The [String] representation of a blank [YamlConfig]
         */
        protected const val BLANK_CONFIG = "{}\n"

        /**
         * Static method to just load an internal YAML file right onto the server without having
         * to create a custom class because we aren't doing any internal loading.
         *
         * @param plugin    The plugin this file is apart of
         * @param directory The directory to put the file in
         * @param fileName  The name of the file
         */
        fun loadInternalYaml(plugin: SpigotPlugin, directory: String, fileName: String) {
            val toLoad = YamlConfig(plugin, directory, fileName)
            toLoad.load()
        }
    }
}