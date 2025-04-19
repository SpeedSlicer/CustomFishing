package com.illuzionzstudios.mist.conversation.type

import com.illuzionzstudios.mist.conversation.SimplePrompt
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.Prompt
import org.bukkit.entity.Player
import java.util.function.Consumer

/**
 * A prompt that only accepts whole or decimal numbers
 */
class SimpleDecimalPrompt @JvmOverloads constructor(
    private val question: String?,
    private val successAction: Consumer<Double>,
    openMenu: Boolean = true
) : SimplePrompt(openMenu) {

    /**
     * The menu question
     */
    override fun getPrompt(ctx: ConversationContext?): String {
        Valid.checkNotNull(question, "Please either call setQuestion or override getPrompt")
        return "&6$question"
    }

    /**
     * Return true if input is a valid number
     */
    override fun isInputValid(context: ConversationContext, input: String): Boolean {
        return Valid.isDecimal(input) || Valid.isInteger(input)
    }

    /**
     * Show the message when the input is not a number
     */
    override fun getFailedValidationText(context: ConversationContext, invalidInput: String): String? {
        return "The number must be a whole or a decimal number."
    }

    /**
     * Parse through
     *
     * @see org.bukkit.conversations.ValidatingPrompt.acceptValidatedInput
     */
    override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt? {
        return acceptValidatedInput(context, input.toDouble())
    }

    /**
     * What happens when the number is entered
     *
     * @return the next prompt, or [Prompt.END_OF_CONVERSATION] (that is actualy null) to exit
     */
    protected fun acceptValidatedInput(context: ConversationContext?, input: Double): Prompt {
        Valid.checkNotNull(question, "Please either call setSuccessAction or override acceptValidatedInput")
        successAction.accept(input)
        return END_OF_CONVERSATION
    }

    companion object {
        /**
         * Show the question with the action to the player
         */
        fun show(player: Player?, question: String?, successAction: Consumer<Double>) {
            SimpleDecimalPrompt(question, successAction).show(player!!)
        }
    }
}