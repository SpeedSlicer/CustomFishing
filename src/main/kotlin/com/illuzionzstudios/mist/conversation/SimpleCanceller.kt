package com.illuzionzstudios.mist.conversation

import com.illuzionzstudios.mist.util.Valid
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationCanceller
import org.bukkit.conversations.ConversationContext
import java.util.*

/**
 * A simple conversation canceller
 * If the players message matches any word in the list, his conversation is cancelled
 */
class SimpleCanceller(cancelPhrases: List<String?>) : ConversationCanceller {
    /**
     * The words that trigger the conversation cancellation
     */
    private val cancelPhrases: List<String?>

    /**
     * Create a new convo canceler based off the given strings
     * If the players message matches any word in the list, his conversation is cancelled
     *
     * @param cancelPhrases
     */
    constructor(vararg cancelPhrases: String?) : this(Arrays.asList<String?>(*cancelPhrases))

    override fun setConversation(conversation: Conversation) {}

    /**
     * Listen to cancel phrases and exit if they equals
     */
    override fun cancelBasedOnInput(context: ConversationContext, input: String): Boolean {
        for (phrase in cancelPhrases) if (input.equals(phrase, ignoreCase = true)) return true
        return false
    }

    override fun clone(): ConversationCanceller {
        return SimpleCanceller(cancelPhrases)
    }

    /**
     * Create a new convo canceler from the given lists
     * If the players message matches any word in the list, his conversation is cancelled
     *
     * @param cancelPhrases
     */
    init {
        Valid.checkBoolean(!cancelPhrases.isEmpty(), "Cancel phrases are empty for conversation cancel listener!")
        this.cancelPhrases = cancelPhrases
    }
}