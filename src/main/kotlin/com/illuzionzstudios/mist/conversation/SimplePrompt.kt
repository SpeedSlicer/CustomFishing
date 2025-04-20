package com.illuzionzstudios.mist.conversation

import com.illuzionzstudios.mist.Mist.Companion.tellConversing
import com.illuzionzstudios.mist.Mist.Companion.tellLaterConversing
import com.illuzionzstudios.mist.ui.UserInterface
import com.illuzionzstudios.mist.util.TextUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.conversations.*
import org.bukkit.entity.Player

/**
 * Represents one question for the player during a server conversation
 */
abstract class SimplePrompt : ValidatingPrompt, Cloneable {

    /**
     * Open the players menu back if any?
     */
    private var openMenu = true

    /**
     * The player who sees the input
     */
    private var player: Player? = null

    protected constructor()

    /**
     * Create a new prompt, show we open players menu back if he has any?
     */
    protected constructor(openMenu: Boolean) {
        this.openMenu = openMenu
    }

    /**
     * Return the prefix before tell messages
     */
    protected val customPrefix: String?
        get() = null

    /**
     * Return the question, implemented in own way using colors
     */
    override fun getPromptText(ctx: ConversationContext): String {
        return TextUtil.formatText(getPrompt(ctx))
    }

    /**
     * Return the question to the user in this prompt
     */
    protected abstract fun getPrompt(ctx: ConversationContext?): String

    /**
     * Checks if the input from the user was valid, if it was, we can continue to the next prompt
     */
    override fun isInputValid(context: ConversationContext, input: String): Boolean {
        return true
    }

    /**
     * Return the failed error message when [.isInputValid] returns false
     */
    override fun getFailedValidationText(context: ConversationContext, invalidInput: String): String? {
        return null
    }

    /**
     * Converts the [ConversationContext] into a [Player]
     * or throws an error if it is not a player
     */
    protected fun getPlayer(ctx: ConversationContext): Player {
        Valid.checkBoolean(ctx.forWhom is Player, "Conversable is not a player but: " + ctx.forWhom)
        return ctx.forWhom as Player
    }

    /**
     * Send the player (in case any) the given message
     */
    protected fun tell(message: String) {
        Valid.checkNotNull(player, "Cannot use tell() when player not yet set!")
        tell(player, message)
    }

    /**
     * Send the player (in case any) the given message
     */
    protected fun tell(ctx: ConversationContext, message: String) {
        tell(getPlayer(ctx), message)
    }

    /**
     * Sends the message to the player
     */
    protected fun tell(conversable: Conversable?, message: String) {
        tellConversing(conversable!!, (if (customPrefix != null) customPrefix else "") + message)
    }

    /**
     * Sends the message to the player later
     */
    protected fun tellLater(conversable: Conversable?, message: String, delayTicks: Int) {
        tellLaterConversing(conversable!!, (if (customPrefix != null) customPrefix else "") + message, delayTicks)
    }

    /**
     * Called when the whole conversation is over. This is called before [SimpleConversation.onConversationEnd]
     */
    fun onConversationEnd(conversation: SimpleConversation?, event: ConversationAbandonedEvent?) {}

    // Do not allow superclasses to modify this since we have isInputValid here
    override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
        return if (isInputValid(context, input!!)) acceptValidatedInput(context, input) else {
            val failPrompt = getFailedValidationText(context, input)
            if (failPrompt != null) tellLater(context.forWhom, "&c$failPrompt", 1)

            // Redisplay this prompt to the user to re-collect input
            this
        }
    }

    public override fun clone(): SimplePrompt {
        return (super<ValidatingPrompt>.clone() as SimplePrompt)
    }

    /**
     * Shows this prompt as a conversation to the player
     *
     *
     * NB: Do not call this as a means to showing this prompt DURING AN EXISTING
     * conversation as it will fail! Use [.acceptValidatedInput] instead
     * to show the next prompt
     */
    fun show(player: Player): SimpleConversation {
        Valid.checkBoolean(
            !player.isConversing,
            "Player " + player.name + " is already conversing! Show them their next prompt in acceptValidatedInput() in " + javaClass.simpleName + " instead!"
        )
        this.player = player
        val conversation: SimpleConversation = object : SimpleConversation() {
            override val firstPrompt: Prompt
                get() = this@SimplePrompt
            override val prefix: ConversationPrefix
                get() {
                    val prefix = customPrefix
                    return prefix?.let { SimplePrefix(it) } ?: super.prefix
                }
        }
        if (openMenu) {
            val menu: UserInterface? = UserInterface.getInterface(player)
            conversation.setMenuToReturnTo(menu)
        }
        conversation.start(player)
        return conversation
    }

    companion object {
        /**
         * Show the given prompt to the player
         */
        fun show(player: Player, prompt: SimplePrompt) {
            prompt.show(player)
        }
    }
}