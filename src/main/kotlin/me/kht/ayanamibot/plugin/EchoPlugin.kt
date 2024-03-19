package me.kht.ayanamibot.plugin

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import org.springframework.stereotype.Component

private const val USAGE = """
    Echo: $<> <message>
"""

private const val DESC = """
    Echo a message.
"""

@Component
class EchoPlugin(groupFeatures: HashMap<Long, Set<String>>) : BasePlugin(groupFeatures, USAGE, DESC, "Echo") {

    private val commandRegex = Regex("""^\$<> .+""")

    override fun onAnyMessage(bot: Bot?, event: AnyMessageEvent?): Int {
        bot ?: return MESSAGE_IGNORE
        event ?: return MESSAGE_IGNORE

        shouldWork(event) || return MESSAGE_IGNORE

        val message = event.rawMessage
        if (commandRegex.matches(message)) {
            val reply = message.substring(4)
            bot.sendMsg(event, reply, false)
            return MESSAGE_BLOCK
        }

        return MESSAGE_IGNORE
    }
}