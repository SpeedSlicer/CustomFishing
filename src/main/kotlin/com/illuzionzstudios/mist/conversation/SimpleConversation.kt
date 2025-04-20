package com.illuzionzstudios.mist.conversation

import com.cryptomorin.xseries.XSound
import com.illuzionzstudios.mist.Mist.Companion.tellConversing
import com.illuzionzstudios.mist.Mist.Companion.tellLaterConversing
import com.illuzionzstudios.mist.config.locale.PluginLocale
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.ui.UserInterface
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.conversations.*
import org.bukkit.entity.Player

/**
 * A simple way to communicate with the player.
 * Their chat will be isolated, and they chat messages processed and
 * the conversation input.
 */
abstract class SimpleConversation
/**
 * Creates a simple conversation
 */ protected constructor(
    /**
     * The menu to return to, if any
     */
    private var menuToReturnTo: UserInterface? = null
) : ConversationAbandonedListener {
    /**
     * Start a conversation with the player, throwing error if [Player.isConversing]
     */
    fun start(player: Player) {
        Valid.checkBoolean(!player.isConversing, "Player " + player.name + " is already conversing!")

        // Do not allow open inventory since they cannot type anyways
        player.closeInventory()

        // Setup
        val conversation = CustomConversation(player)
        val inactivityCanceller = InactivityConversationCanceller(SpigotPlugin.instance!!, 45)
        inactivityCanceller.setConversation(conversation)
        conversation.cancellers.add(inactivityCanceller)
        conversation.cancellers.add(canceller)
        conversation.addConversationAbandonedListener(this)
        conversation.begin()
    }

    /**
     * Get the first prompt in this conversation for the player
     */
    protected abstract val firstPrompt: Prompt

    /**
     * Listen for and handle existing the conversation
     */
    override fun conversationAbandoned(event: ConversationAbandonedEvent) {
        val conversing = event.context.forWhom
        val source = event.source
        if (source is CustomConversation) {
            val lastPrompt: SimplePrompt? = source.lastSimplePrompt
            lastPrompt?.onConversationEnd(this, event)
        }
        onConversationEnd(event)
        if (conversing is Player) {
            (if (event.gracefulExit()) XSound.BLOCK_NOTE_BLOCK_PLING else XSound.BLOCK_NOTE_BLOCK_BASS).play(
                conversing,
                1f,
                1f
            )
            if (menuToReturnTo != null && reopenMenu()) menuToReturnTo!!.newInstance().show(conversing)
        }
    }

    /**
     * Fired when the user quits this conversation (see [.getCanceller], or
     * simply quits the game)
     */
    fun onConversationEnd(event: ConversationAbandonedEvent?) {}

    /**
     * Get conversation prefix before each message
     *
     * By default we use the plugins tell prefix
     *
     * TIP: You can use [SimplePrefix]
     */
    protected open val prefix: ConversationPrefix
        protected get() = SimplePrefix(PluginLocale.GENERAL_PLUGIN_PREFIX.toString() + " ")

    private fun addLastSpace(prefix: String): String {
        return if (prefix.endsWith(" ")) prefix else "$prefix "
    }

    /**
     * Return the canceller that listens for certain words to exit the convo,
     * by default we use [SimpleCanceller] that listens to quit|cancel|exit
     */
    protected val canceller: ConversationCanceller
        get() = SimpleCanceller("quit", "cancel", "exit")

    /**
     * Return true if we should insert a prefix before each message, see [.getPrefix]
     */
    protected fun insertPrefix(): Boolean {
        return true
    }

    /**
     * If we detect the player has a menu opened should we reopen it?
     */
    protected fun reopenMenu(): Boolean {
        return true
    }
    // ------------------------------------------------------------------------------------------------------------
    // Static access
    // ------------------------------------------------------------------------------------------------------------
    /**
     * Get the timeout in seconds before automatically exiting the convo
     */
    protected val timeout: Int
        get() = 60

    /**
     * Sets the menu to return to after the end of this conversation
     */
    fun setMenuToReturnTo(menu: UserInterface?) {
        menuToReturnTo = menu
    }

    /**
     * Custom conversation class used for only showing the question once per 20 seconds interval
     */
    private inner class CustomConversation(
        forWhom: Conversable,
        var lastSimplePrompt: SimplePrompt? = null
    ) :
        Conversation(SpigotPlugin.instance, forWhom, firstPrompt) {

        override fun outputNextPrompt() {
            if (currentPrompt == null) abandon(ConversationAbandonedEvent(this)) else {
                // Edit start

                // Edit 1 - save the time when we showed the question to the player
                // so that we only show it once per the given threshold
                val promptClass = currentPrompt.javaClass.simpleName
                val question = currentPrompt.getPromptText(context)
                try {
                    val askedQuestions = context.allSessionData
                        .getOrDefault("Asked_$promptClass", HashMap<Any, Any>()) as HashMap<String, Void?>
                    if (!askedQuestions.containsKey(question)) {
                        askedQuestions[question] = null
                        context.setSessionData("Asked_$promptClass", askedQuestions)
                        context.forWhom.sendRawMessage(prefix.getPrefix(context) + question)
                    }
                } catch (ex: NoSuchMethodError) {
                    // Unfortunately old MC version detected
                }

                // Edit 2 - Save last prompt if it is our class
                if (currentPrompt is SimplePrompt) lastSimplePrompt = ((currentPrompt as SimplePrompt).clone())

                // Edit end
                if (!currentPrompt.blocksForInput(context)) {
                    currentPrompt = currentPrompt.acceptInput(context, null)
                    outputNextPrompt()
                }
            }
        }

        init {
            localEchoEnabled = false
            if (insertPrefix()) prefix = this@SimpleConversation.prefix
        }
    }

    companion object {
        /**
         * Shortcut method for direct message send to the player
         */
        protected fun tell(conversable: Conversable?, message: String?) {
            tellConversing(conversable!!, message)
        }

        /**
         * Send a message to the conversable player later
         */
        protected fun tellLater(conversable: Conversable?, message: String?, delayTicks: Int) {
            tellLaterConversing(conversable!!, message, delayTicks)
        }
    }
}