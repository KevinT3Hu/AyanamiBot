package me.kht.ayanamibot.plugin

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import org.springframework.stereotype.Component

private const val USAGE = """
    Enable plugin: $, <plugin>
    Disable plugin: $. <plugin>
    List plugins: $:
"""

private const val DESC = """
    Enable or disable plugins.
"""

@Component
class MetaPlugin(
    private val groupFeatures: HashMap<Long, Set<String>>,
    private val pluginList: List<String>
) : BasePlugin(groupFeatures, USAGE, DESC) {

    override fun getUsage(): String {
        return USAGE
    }

    override fun getDesc(): String {
        return DESC
    }

    fun doEnable(bot: Bot, groupId: Long, message: String): Int {
        val enableRegex = Regex("""^\$, .+""")
        val disableRegex = Regex("""^\$\. .+""")

        if (message == "$:") {
            val enabled = groupFeatures[groupId] ?: setOf()
            val disabled = pluginList.minus(enabled)
            val enabledStr = enabled.joinToString(", ")
            val disabledStr = disabled.joinToString(", ")
            bot.sendGroupMsg(groupId, "Enabled plugins: $enabledStr\nDisabled plugins: $disabledStr", false)
            return MESSAGE_BLOCK
        }

        if (enableRegex.matches(message)) {
            val plugin = message.split(" ").last()
            pluginList.contains(plugin) || return MESSAGE_IGNORE
            groupFeatures[groupId] = groupFeatures[groupId]?.plus(plugin) ?: setOf(plugin)
            bot.sendGroupMsg(groupId, "Enabled plugin: $plugin", false)
        } else if (disableRegex.matches(message)) {
            val plugin = message.split(" ").last()
            pluginList.contains(plugin) || return MESSAGE_IGNORE
            groupFeatures[groupId] = groupFeatures[groupId]?.minus(plugin) ?: setOf()
            bot.sendGroupMsg(groupId, "Disabled plugin: $plugin", false)
        } else {
            return MESSAGE_IGNORE
        }

        return MESSAGE_BLOCK
    }

    override fun onGroupMessage(bot: Bot?, event: GroupMessageEvent?): Int {
        val groupId = event?.groupId ?: return MESSAGE_IGNORE
        val userId = event.userId ?: return MESSAGE_IGNORE
        val message = event.rawMessage ?: return MESSAGE_IGNORE
        bot ?: return MESSAGE_IGNORE

        return doEnable(bot, groupId, message)
    }
}