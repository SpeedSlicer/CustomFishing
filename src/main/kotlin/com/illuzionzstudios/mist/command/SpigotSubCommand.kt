package com.illuzionzstudios.mist.command

import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.util.Valid
import lombok.*
import java.util.*

/**
 * This command is apart of a command group. It acts as a sub label to
 * add extra functionality. For instance, "/maincommand rewards", rewards is the
 * sub label
 *
 * Acts as a normal command that runs based of a [SpigotCommandGroup]
 */
abstract class SpigotSubCommand protected constructor(parent: SpigotCommandGroup, vararg aliases: String) :
    SpigotCommand(parent.label!!) {

    /**
     * The registered sub labels, or aliases this command has
     */
    val subLabels: Array<String>

    /**
     * The latest sub label used when the sub command was run,
     * always updated on executing
     */
    var subLabel: String

    /**
     * Create a new [SpigotCommand] with certain labels
     * Main command group found from [SpigotPlugin.getMainCommand]
     *
     * @param aliases Additional labels that correspond to this [SpigotCommand]
     * First label is the main label
     */
    protected constructor(vararg aliases: String) : this(mainCommandGroup, *aliases)

    /**
     * The command group automatically displays all sub commands in the /{label} help|? menu.
     * Shall we display the sub command in this menu?
     *
     * @return If to show in help
     */
    var showInHelp: Boolean = true

    /**
     * Replace additional {sublabel} placeholder for this subcommand.
     * See [SpigotCommand.replacePlaceholders]
     */
    override fun replacePlaceholders(message: String?): String? {
        return super.replacePlaceholders(message)?.replace("{sublabel}", subLabel)
    }

    /**
     * Compare based on sub labels
     */
    override fun equals(other: Any?): Boolean {
        return other is SpigotCommand && (other as SpigotSubCommand).subLabels.contentEquals(subLabels)
    }

    override fun hashCode(): Int {
        var result = subLabels.contentHashCode()
        result = 31 * result + showInHelp.hashCode()
        return result
    }

    companion object {
        /**
         * @return Main [SpigotCommandGroup] for the plugin
         */
        private val mainCommandGroup: SpigotCommandGroup
            get() {
                val main: SpigotCommandGroup = SpigotPlugin.instance!!.mainCommand!!
                Valid.checkNotNull(
                    main,
                    SpigotPlugin.instance!!.name + " does not define a main command group!"
                )
                return main
            }
    }

    /**
     * Create a new [SpigotCommand] with certain labels
     *
     * @param aliases Additional labels that correspond to this [SpigotCommand]
     * First label is the main label
     */
    init {
        // Set sub labels
        subLabels = aliases as Array<String>

        // Set main label
        subLabel = subLabels[0]

        // If the default perm was not changed, improve it
        if (rawPermission == DEFAULT_PERMISSION_SYNTAX) permission =
            if (SpigotPlugin.isMainCommand(this.mainLabel)) rawPermission?.replace(
                "{label}",
                "{sublabel}"
            ) // simply replace label with sublabel
            else "$rawPermission.{sublabel}" // append the sublabel at the end since this is not our main command
    }
}