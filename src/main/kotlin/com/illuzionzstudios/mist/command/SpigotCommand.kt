package com.illuzionzstudios.mist.command

import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.command.response.ReturnType
import com.illuzionzstudios.mist.config.locale.MistString
import com.illuzionzstudios.mist.config.locale.PluginLocale
import com.illuzionzstudios.mist.config.locale.mist
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.PlayerUtil
import com.illuzionzstudios.mist.util.TextUtil
import com.illuzionzstudios.mist.util.Valid
import lombok.*
import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.ArrayList

/**
 * This is an instance of a custom command for a [com.illuzionzstudios.mist.plugin.SpigotPlugin]
 * Here we can create our own custom commands for functionality. These commands
 * can have child commands which are executed by specifying certain arguments,
 * eg, "/main arg", where "arg" will execute it's own functionality
 */
abstract class SpigotCommand protected constructor(
    /**
     * The main label for the command. This means when this label is
     * passed as a command, use this [SpigotCommand] functionality
     * This is updated based on what is sent, which must be on of our aliases
     */
    var commandLabel: String,

    /**
     * Command aliases
     */
    vararg aliases: String?
) : Command(
    commandLabel, "", "", listOf(*aliases)
) {
    /**
     * This is the instance of [CommandSender] who executed this command.
     * Updated dynamically when executing the command
     */
    protected var sender: CommandSender? = null

    /**
     * These are the last parsed arguments to the command. Updated
     * dynamically every time we execute the command
     */
    protected lateinit var args: Array<String>

    /**
     * Flag indicating if this [SpigotCommand] has been registered
     */
    var registered = false

    /**
     * These are the minimum amount of arguments to be passed to the command
     * for it to actually execute
     */
    protected var minArguments = 0

    /**
     * Should we automatically send usage message when the first argument
     * equals to "help" or "?" ?
     */
    protected var autoHandleHelp = true

    /**
     * Register this command into Bukkit to be used.
     * Can throw [com.illuzionzstudios.mist.exception.PluginException] if [.isRegistered]
     *
     * @param unregisterOldAliases If to unregister old aliases
     */
    /**
     * See [.register]
     */
    @JvmOverloads
    fun register(unregisterOldAliases: Boolean = true) {
        Valid.checkBoolean(!registered, "The command /$label has already been registered!")
        val oldCommand = Bukkit.getPluginCommand(label)
        if (oldCommand != null) {
            val owningPlugin = oldCommand.plugin.name
            if (owningPlugin != SpigotPlugin.pluginName) Logger.info("&eCommand &f/$label &ealready used by $owningPlugin, we take it over...")
            CommandUtil.unregisterCommand(oldCommand.label, unregisterOldAliases)
        }
        registered = true
        CommandUtil.registerCommand(this)
    }

    /**
     * Removes the command from Bukkit.
     *
     *
     * Throws an error if the command is not [.isRegistered].
     */
    fun unregister() {
        Valid.checkBoolean(registered, "The command /$label is not registered!")
        CommandUtil.unregisterCommand(label)
        registered = false
    }

    // ----------------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------------

    /**
     * Execute this command, updates the [.sender], [.label] and [.args] variables,
     * checks permission and returns if the sender lacks it,
     * checks minimum arguments and finally passes the command to the child class.
     *
     *
     * Also contains various error handling scenarios
     */
    override fun execute(sender: CommandSender, label: String, args: Array<String>): Boolean {

        // Update variables
        this.sender = sender
        this.label = label
        this.args = args

        // Attempt to execute commands and catch errors
        try {

            // Check permissions
            if (!hasPerm(permission)) {
                // Inform
                tell(permissionMessage?.mist?.toString("permission", permission))
                return true
            }

            // Too little arguments and inform help
            if (args.size < minArguments || (autoHandleHelp && args.size == 1 && ("help" == args[0] || "?" == args[0]))) {
                if (!usage.trim { it <= ' ' }.equals("", ignoreCase = true)) // Inform usage message
                    tell(
                        PluginLocale.COMMAND_INVALID_USAGE.toString("{label}", label)
                            .toString("{args}", java.lang.String.join(" ", *args))
                    )
                return true
            }

            // Finally execute command
            val response = onCommand()

            // TODO: Implement properly once new lang system is written
            if (response == ReturnType.NO_PERMISSION) {
                tell("&cYou don't have enough permissions to do this...")
            } else if (response == ReturnType.PLAYER_ONLY) {
                tell(PluginLocale.COMMAND_PLAYER_ONLY)
            } else if (response == ReturnType.UNKNOWN_ERROR) {
                tell("&cThis was not supposed to happen...")
            }
        } catch (ex: Throwable) {
            tell(
                "&cFatal error occurred trying to execute command /" + getLabel(),
                "&cPlease contact an administrator to resolve the issue"
            )
            Logger.displayError(
                ex,
                "Failed to execute command /" + getLabel() + " " + java.lang.String.join(" ", *args)
            )
        }
        return true
    }

    /**
     * This is invoked when the command is run. All dynamic information about the command
     * can be accessed via the class and doesn't need to be passed to here
     */
    protected abstract fun onCommand(): ReturnType

    protected fun tell(message: MistString?) {
        message!!.sendMessage(sender)
    }

    /**
     * @param message Tell the command sender a single message
     */
    protected fun tell(message: String?) {
        if (sender != null) sender?.sendMessage(TextUtil.formatText(message))
    }

    /**
     * @param messages Tell the command sender a set of messages
     */
    protected fun tell(vararg messages: String?) {
        if (sender != null) {
            for (message in messages) {
                sender?.sendMessage(TextUtil.formatText(message))
            }
        }
    }

    /**
     * Replaces placeholders in the message with arguments. For instance if the message is
     * "set user {0} to {1} rank" it will take the first args and become maybe
     * "set user IlluzionzDev to Owner rank".
     *
     * @param message Message to replace
     * @return Message with placeholders handled
     */
    open fun replacePlaceholders(message: String?): String? {
        // Replace basic labels
        var msg = message
        msg = replaceBasicPlaceholders(msg)

        // Replace {X} with arguments
        for (i in args.indices) msg = msg?.replace("{$i}", args[i])
        return message
    }

    /**
     * Internal method for replacing {label} {sublabel} and {plugin.name} placeholders
     *
     * @param message The message to replace
     * @return Replaced message
     */
    private fun replaceBasicPlaceholders(message: String?): String? {
        return message
            ?.replace("{label}", label)
            ?.replace("{sublabel}", if (this is SpigotSubCommand) this.subLabels[0] else super.getLabel())
            ?.replace("{plugin.name}", SpigotPlugin.pluginName.lowercase(Locale.getDefault()))
    }

    /**
     * Checks if the sender is a console
     */
    protected fun checkConsole(): Boolean {
        return !isPlayer()
    }

    /**
     * A convenience check for quickly determining if the sender has a given
     * permission.
     *
     * @param permission Permission to check
     * @return If player does have permission
     */
    protected fun hasPerm(permission: String?): Boolean {
        return PlayerUtil.hasPerm(sender, permission)
    }

    /**
     * This is the instance of [Player] who executed this command.
     * This only applies if the [.sender] is an instance of [Player]
     * If is not an instance of [CommandSender], returns null
     */
    protected val player: Player?
        get() = if (isPlayer()) sender as Player? else null

    /**
     * See [.getPlayer]
     *
     * @return Isn't null
     */
    protected fun isPlayer(): Boolean {
        return sender is Player
    }

    /**
     * Get the permission for this command, either the one you set or our from Localization
     */
    override fun getPermissionMessage(): String? {
        return if (super.getPermissionMessage() != null && super.getPermissionMessage()!!
                .trim { it <= ' ' } != ""
        ) super.getPermissionMessage() else PluginLocale.COMMAND_NO_PERMISSION.toString()
    }

    /**
     * By default we check if the player has the permission you set in setPermission.
     *
     *
     * If that is null, we check for the following:
     * {plugin.name}.command.{label} for [SpigotCommand]
     *
     *
     * We handle lacking permissions automatically and return with a no-permission message
     * when the player lacks it.
     *
     * @return The formatted permission
     */
    override fun getPermission(): String? {
        return replaceBasicPlaceholders(super.getPermission()) ?: ""
    }

    /**
     * Get the permission without replacing {plugin.name}, {label} or {sublabel}
     */
    val rawPermission: String?
        get() = super.getPermission()

    /**
     * Get the label given when the command was created or last updated with [.setLabel]
     */
    val mainLabel: String
        get() = super.getLabel()

    /**
     * Updates the label of this command
     */
    override fun setLabel(name: String): Boolean {
        commandLabel = name
        return super.setLabel(name)
    }

    /**
     * Show tab completion suggestions when the given sender
     * writes the command with the given arguments
     *
     *
     * Tab completion is only shown if the sender has [.getPermission]
     */
    @Throws(IllegalArgumentException::class)
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        this.sender = sender
        this.label = alias
        this.args = args
        val names: MutableList<String> = ArrayList()
        Bukkit.getOnlinePlayers().forEach {
            names.add(it.name)
        }
        Bukkit.getOnlinePlayers().forEach { names.add(it.name) }
        return if (hasPerm(permission)) {
            tabComplete() ?: ArrayList()
        } else names
    }

    /**
     * Override this method to support tab completing in your command.
     *
     *
     * You can then use "sender", "label" or "args" fields from [SpigotCommand]
     * class normally and return a list of tab completion suggestions.
     *
     *
     * We already check for [.getPermission] and only call this method if the
     * sender has it.
     *
     * @return the list of suggestions to complete, or null to complete player names automatically
     */
    protected open fun tabComplete(): List<String>? {
        return null
    }

    companion object {
        /**
         * This is the default permission syntax for a [SpigotCommand]
         * {plugin.name} The plugin's name
         * {label} The main command label
         */
        const val DEFAULT_PERMISSION_SYNTAX = "{plugin.name}.command.{label}"

        /**
         * A unique immutable list of all registered commands in the [com.illuzionzstudios.mist.plugin.SpigotPlugin]
         */
        @Getter
        private val registeredCommands = HashSet<SpigotCommand>()
    }

    /**
     * Create a new [SpigotCommand] with certain labels
     *
     * @param label   The main label for this command
     * @param aliases Additional labels that correspond to this [SpigotCommand]
     */
    init {
        // Set our permission formatting
        permission = DEFAULT_PERMISSION_SYNTAX

        // When creating this command instance, register it
        // Not actually registered for execution
        registeredCommands.add(this)
    }
}