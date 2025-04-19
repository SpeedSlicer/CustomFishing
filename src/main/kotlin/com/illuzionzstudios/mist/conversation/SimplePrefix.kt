package com.illuzionzstudios.mist.conversation

import com.illuzionzstudios.mist.util.TextUtil
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationPrefix

/**
 * A simple conversation prefix with a static string
 */
class SimplePrefix(private val prefix: String?) : ConversationPrefix {

    override fun getPrefix(context: ConversationContext): String {
        return TextUtil.formatText(prefix)
    }
}