package me.kht.ayanamibot.plugin

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.AnyMessageEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

private const val TAG = "Help"

@Component
class HelpPlugin(
    groupFeatures: HashMap<Long, Set<String>>,
    private val pluginList: List<String>,
    private val applicationContext: ApplicationContext
) : BasePlugin(groupFeatures, tag = TAG) {

    private val helpRegex = Regex("""\$\?( (.*))?""")

    private val logger = KotlinLogging.logger(TAG)

    override fun onAnyMessage(bot: Bot?, event: AnyMessageEvent?): Int {

        bot ?: return MESSAGE_IGNORE
        event ?: return MESSAGE_IGNORE

        val message = event.rawMessage

        val matchEntries = helpRegex.matchEntire(message)

        matchEntries ?: return MESSAGE_IGNORE

        val command = matchEntries.groupValues[2]

        val msg = buildString {
            if (command.isEmpty()) {

                logger.info { "Help: List all plugins" }

                append("Available commands:\n")
                for (plugin in pluginList) {
                    try {
                        val pluginClass = Class.forName("me.kht.ayanamibot.plugin.${plugin}Plugin")
                        val pluginBean = applicationContext.getBean(pluginClass) as BasePlugin
                        append("$plugin ${pluginBean.getDesc()}\n")
                    } catch (e: ClassNotFoundException) {
                        logger.error { "Plugin $plugin not found" }
                        continue
                    }
                }
                append("Use $? <command> to get detailed help.")
            } else {
                try {
                    val pluginClass = Class.forName("me.kht.ayanamibot.plugin.${command}Plugin")
                    val pluginBean = applicationContext.getBean(pluginClass) as BasePlugin
                    val desc = pluginBean.getDesc()
                    val usage = pluginBean.getUsage()
                    append("Command: $command\n")
                    append("Description: $desc\n")
                    append("Usage: $usage")
                } catch (e: ClassNotFoundException) {
                    append("Command not found: $command")
                    return@buildString
                }
            }
        }

        bot.sendMsg(event, msg, false)

        return MESSAGE_BLOCK
    }

    override fun enabled(groupId: Long) = true

}