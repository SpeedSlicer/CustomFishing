package com.illuzionzstudios.mist.command

import com.illuzionzstudios.mist.command.response.ReturnType
import com.illuzionzstudios.mist.config.locale.PluginLocale
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.TextUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.ChatColor
import java.util.*

/**
 * Contains a group of commands for execution. Contains the main command,
 * for instance "/customfishing", and the subs for that command, eg "/customfishing rewards"
 * allows us to group functionality for commands and interact with each other. Also will
 * provide a help sub command that lists all available commands for the group
 */
abstract class SpigotCommandGroup {

    /**
     * The [SpigotSubCommand] that belong to this group
     */
    protected val subCommands = HashSet<SpigotSubCommand>()

    /**
     * The main [SpigotCommand] which [SpigotSubCommand] are
     * executed through
     */
    private var mainCommand: SpigotCommand? = null

    /**
     * Register this command group with the main label,
     * then add sub commands to it
     *
     * @param labels List of main label and aliases
     */
    fun register(vararg labels: String) {
        Valid.checkBoolean(!isRegistered, "Main command was already registered as: $mainCommand")

        // Set command
        mainCommand = MainCommand(labels[0])

        // Set the aliases
        if (labels.size > 1) mainCommand?.aliases = listOf(*labels.copyOfRange(1, labels.size))

        // Register it
        mainCommand?.register()

        // Register sub commands
        registerSubcommands()
    }

    /**
     * Unregister the main command and all aliases
     */
    fun unregister() {
        Valid.checkBoolean(isRegistered, "Main command not registered!")
        mainCommand!!.unregister()
        mainCommand = null
        subCommands.clear()
    }

    /**
     * Register our sub commands to this command group
     */
    abstract fun registerSubcommands()

    /**
     * @param command Add a [SpigotSubCommand] to this group
     */
    protected fun registerSubCommand(command: SpigotSubCommand) {
        Valid.checkNotNull(mainCommand, "Cannot add subcommands when main command is missing! Call register()")
        Valid.checkBoolean(
            !subCommands.contains(command),
            "Subcommand /" + mainCommand!!.label + " " + command.subLabel + " already registered!"
        )
        subCommands.add(command)
    }

    /**
     * Get the label for this command group, failing if not yet registered
     *
     * @return The string label
     */
    val label: String?
        get() {
            Valid.checkBoolean(isRegistered, "Main command has not yet been set!")
            return mainCommand?.mainLabel
        }

    /**
     * Has the command group been registered yet?
     *
     * @return If main command is set
     */
    val isRegistered: Boolean
        get() = mainCommand != null

    /**
     * Return which subcommands should trigger the automatic help
     * menu that shows all subcommands sender has permission for.
     *
     *
     * Default: help and ?
     *
     * @return List of labels
     */
    protected val helpLabel: List<String>
        get() = listOf("help", "?")

    /**
     * Return the header messages used in /{label} help|? typically,
     * used to tell all available subcommands from this command group
     *
     * @return String array of messages
     */
    protected val helpHeader: Array<String>
        get() = arrayOf(
            "&8",
            "&8" + TextUtil.SMOOTH_LINE,
            headerPrefix + "  " + SpigotPlugin.pluginName + " &7v" + SpigotPlugin.pluginVersion
                    + if (SpigotPlugin.instance!!.description.authors.isNotEmpty()
            ) " by " + SpigotPlugin.instance!!.description.authors[0] else "",
            " ",
            "&2  [] &7= " + PluginLocale.COMMAND_LABEL_OPTIONAL_ARGS,
            "&6  <> &7= " + PluginLocale.COMMAND_LABEL_REQUIRED_ARGS,
            " "
        )

    /**
     * Return the default color in the [.getHelpHeader],
     * LIGHT_PURPLE + BOLD colors by default
     *
     * @return Header prefix colours
     */
    protected val headerPrefix: String
        get() = "" + SpigotPlugin.instance!!.pluginColor + ChatColor.BOLD

    /**
     * Handles our main command to detect sub commands and run functionality
     */
    private inner class MainCommand(label: String) : SpigotCommand(label) {
        /**
         * Here we handle our main help, showing sub commands etc.
         * Also handle execution of sub commands
         */
        override fun onCommand(): ReturnType {
            // Show our help
            // Assures arg[0] isn't null
            if (args.isEmpty() || helpLabel.contains(args[0])) {
                tellSubCommandsHelp()
                return ReturnType.SUCCESS
            }
            val subArg = args[0]
            val command = findSubcommand(subArg)

            // Attempt to run
            if (command != null) {
                command.subLabel = subArg

                // Run the command
                command.execute(
                    sender!!,
                    label,
                    if (args.size == 1) arrayOf() else args.copyOfRange(1, args.size)
                )
            } else {
                // Couldn't find sub command
                tell(PluginLocale.COMMAND_INVALID_SUB.toString("label", mainLabel))
            }
            return ReturnType.SUCCESS
        }

        /**
         * Inform help on sub commands for the player
         */
        private fun tellSubCommandsHelp() {
            // Send the header
            tell(*helpHeader)
            for (subcommand in subCommands) {
                if (subcommand.showInHelp && hasPerm(subcommand.permission)) {
                    val usage = colorizeUsage(subcommand.usage)
                    subcommand.description
                    val desc = subcommand.description
                    tell("  &7/" + label + " " + subcommand.subLabels[0] + (if (!usage.startsWith("/")) " $usage" else "") + if (desc.isNotEmpty()) "&e- $desc" else "")
                }
            }

            // End line
            tell("&8" + TextUtil.SMOOTH_LINE)
        }

        /**
         * Replaces some usage parameters such as <> or [] with colorized brackets
         *
         * @param message Message to colorize
         * @return Formatted message
         */
        private fun colorizeUsage(message: String?): String {
            return message?.replace("<", "&6<")?.replace(">", "&6>&7")?.replace("[", "&2[")?.replace("]", "&2]&7") ?: ""
        }

        /**
         * Finds a subcommand by label
         *
         * @param label Label to search by
         * @return If a subcommand contains that label as main
         * or an aliases, return that
         */
        private fun findSubcommand(label: String): SpigotSubCommand? {
            for (command in subCommands) {
                for (alias in command.subLabels) if (alias.equals(label, ignoreCase = true)) return command
            }
            return null
        }

        /**
         * Handle tabcomplete for subcommands and their tabcomplete
         */
        public override fun tabComplete(): MutableList<String>? {
            if (args.size == 1) return tabCompleteSubcommands(args[0]).toMutableList()
            if (args.size > 1) {
                val cmd = findSubcommand(args[0])
                if (cmd != null) return cmd.tabComplete(sender!!, label, args.copyOfRange(1, args.size)).toMutableList()
            }
            return null
        }

        /**
         * Automatically tab-complete subcommands
         */
        private fun tabCompleteSubcommands(param: String): List<String> {
            val tab: MutableList<String> = ArrayList()
            for (subcommand in subCommands) if (hasPerm(subcommand.permission)) for (label in subcommand.subLabels) if (label.trim { it <= ' ' }
                    .isNotEmpty() && label.startsWith(param.lowercase(Locale.getDefault()))) tab.add(label)
            return tab
        }

        init {
            // Let everyone view info
            permission = null
        }
    }
}